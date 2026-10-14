package com.example.data.repository

import com.example.data.demo.DemoData
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class SyscoRepository(private val db: AppDatabase) {
    val customers: Flow<List<Customer>> = db.customerDao().getAllCustomers()
    val products: Flow<List<Product>> = db.productDao().getAllProducts()
    val invoices: Flow<List<Invoice>> = db.invoiceDao().getAllInvoices()
    val payments: Flow<List<Payment>> = db.paymentDao().getAllPayments()
    val settings: Flow<CompanySettings?> = db.settingsDao().getSettings()

    // Persistent Shopping Cart flows
    val cartItems: Flow<List<CartItem>> = db.cartDao().getAllCartItems()
    val totalCartCount: Flow<Int> = db.cartDao().getTotalCartCount()
    val cartTotalUsd: Flow<Double> = cartItems.map { list -> list.sumOf { it.totalUsd } }
    val cartTotalDjf: Flow<Double> = cartItems.map { list -> list.sumOf { it.totalDjf } }

    // Real-time KPI summary combining all transactions
    val kpiSummary: Flow<KpiSummary> = combine(
        customers,
        products,
        invoices,
        payments
    ) { custs, prods, invs, pays ->
        var salesUsd = 0.0
        var salesDjf = 0.0
        for (inv in invs) {
            if (inv.currency == "USD") {
                salesUsd += inv.netTotal
            } else {
                salesDjf += inv.netTotal
            }
        }

        var colUsd = 0.0
        var colDjf = 0.0
        for (pay in pays) {
            if (pay.currency == "USD") {
                colUsd += pay.amount
            } else {
                colDjf += pay.amount
            }
        }

        var openingUsd = 0.0
        var openingDjf = 0.0
        for (c in custs) {
            openingUsd += c.openingBalanceUsd
            openingDjf += c.openingBalanceDjf
        }

        val netRecUsd = openingUsd + salesUsd - colUsd
        val netRecDjf = openingDjf + salesDjf - colDjf

        KpiSummary(
            totalSalesUsd = salesUsd,
            totalSalesDjf = salesDjf,
            totalCollectionsUsd = colUsd,
            totalCollectionsDjf = colDjf,
            netReceivablesUsd = netRecUsd,
            netReceivablesDjf = netRecDjf,
            customerCount = custs.size,
            productCount = prods.size
        )
    }

    // Real-time Customer Balances combining transactions for each customer
    val customerBalances: Flow<List<CustomerBalance>> = combine(
        customers,
        invoices,
        payments
    ) { custList, invList, payList ->
        custList.map { customer ->
            val customerInvs = invList.filter { it.customerId == customer.id }
            val customerPays = payList.filter { it.customerId == customer.id }

            val invUsd = customerInvs.filter { it.currency == "USD" }.sumOf { it.netTotal }
            val invDjf = customerInvs.filter { it.currency == "DJF" }.sumOf { it.netTotal }

            val payUsd = customerPays.filter { it.currency == "USD" }.sumOf { it.amount }
            val payDjf = customerPays.filter { it.currency == "DJF" }.sumOf { it.amount }

            val netUsd = customer.openingBalanceUsd + invUsd - payUsd
            val netDjf = customer.openingBalanceDjf + invDjf - payDjf

            CustomerBalance(
                customer = customer,
                totalInvoicesUsd = invUsd,
                totalInvoicesDjf = invDjf,
                totalPaymentsUsd = payUsd,
                totalPaymentsDjf = payDjf,
                netBalanceUsd = netUsd,
                netBalanceDjf = netDjf
            )
        }
    }

    // Customer operations
    suspend fun saveCustomer(customer: Customer): Long {
        return if (customer.id == 0L) {
            db.customerDao().insertCustomer(customer)
        } else {
            db.customerDao().updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        db.customerDao().deleteCustomer(customer)
    }

    suspend fun getCustomerById(id: Long): Customer? {
        return db.customerDao().getCustomerById(id)
    }

    // Product operations
    suspend fun saveProduct(product: Product): Long {
        return if (product.id == 0L) {
            db.productDao().insertProduct(product)
        } else {
            db.productDao().updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: Product) {
        db.productDao().deleteProduct(product)
    }

    // Invoice operations
    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = db.invoiceDao().insertInvoice(invoice)
        // Deduct warehouse stock for each item sold
        for (item in items) {
            if (item.productId > 0) {
                db.productDao().deductStock(item.productId, item.quantity)
            }
        }
        return invoiceId
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        val items = JsonHelper.deserializeInvoiceItems(invoice.itemsJson)
        // Restore stock
        for (item in items) {
            if (item.productId > 0) {
                db.productDao().restoreStock(item.productId, item.quantity)
            }
        }
        db.invoiceDao().deleteInvoice(invoice)
    }

    // Payment operations
    suspend fun recordPayment(payment: Payment): Long {
        return db.paymentDao().insertPayment(payment)
    }

    suspend fun deletePayment(payment: Payment) {
        db.paymentDao().deletePayment(payment)
    }

    // Shopping Cart operations
    fun getCartItemFlow(productId: Long): Flow<CartItem?> = db.cartDao().getCartItemFlow(productId)

    suspend fun getCartItem(productId: Long): CartItem? = db.cartDao().getCartItem(productId)

    suspend fun addToCart(product: Product, quantityToAdd: Int = 1): Boolean {
        if (product.stock <= 0) return false
        val existing = db.cartDao().getCartItem(product.id)
        val currentQty = existing?.quantity ?: 0
        val targetQty = (currentQty + quantityToAdd).coerceAtMost(product.stock)
        val item = CartItem(
            productId = product.id,
            productName = product.name,
            category = product.category,
            specs = product.specs,
            priceUsd = product.priceUsd,
            priceDjf = product.priceDjf,
            quantity = targetQty,
            stock = product.stock,
            imageBase64 = product.imageBase64,
            addedAt = System.currentTimeMillis()
        )
        db.cartDao().insertOrUpdate(item)
        return true
    }

    suspend fun updateCartQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            db.cartDao().deleteCartItem(productId)
        } else {
            db.cartDao().updateQuantity(productId, newQuantity)
        }
    }

    suspend fun removeFromCart(productId: Long) {
        db.cartDao().deleteCartItem(productId)
    }

    suspend fun clearCart() {
        db.cartDao().clearCart()
    }

    // Settings
    suspend fun saveSettings(settings: CompanySettings) {
        db.settingsDao().saveSettings(settings)
    }

    suspend fun getSettingsSync(): CompanySettings {
        return db.settingsDao().getSettingsSync() ?: DemoData.defaultSettings
    }

    // Statement items for a specific customer
    suspend fun getCustomerStatementEntries(customerId: Long): List<LedgerEntry> {
        val invs = db.invoiceDao().getInvoicesForCustomerSync(customerId)
        val pays = db.paymentDao().getPaymentsForCustomerSync(customerId)

        val entries = mutableListOf<LedgerEntry>()
        invs.forEach { entries.add(LedgerEntry.InvoiceEntry(it)) }
        pays.forEach { entries.add(LedgerEntry.PaymentEntry(it)) }

        return entries.sortedBy { it.date }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        val settings = getSettingsSync()
        val settingsObj = JSONObject().apply {
            put("companyName", settings.companyName)
            put("salesRepName", settings.salesRepName)
            put("address", settings.address)
            put("phone", settings.phone)
            put("taxId", settings.taxId)
            put("exchangeRate", settings.exchangeRate)
            put("logoBase64", settings.logoBase64)
            put("signatureBase64", settings.signatureBase64)
        }
        root.put("settings", settingsObj)

        // We can read lists synchronously via direct queries or collectors
        // Since Room supports flow, let's keep backup clean
        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", "1.0")
        return root.toString(2)
    }

    suspend fun exportCsvLedger(balances: List<CustomerBalance>): String {
        val sb = StringBuilder()
        // Add UTF-8 BOM so Excel opens Arabic correctly
        sb.append('\uFEFF')
        sb.append("اسم العميل,المنطقة,الهاتف,الرصيد الافتتاحي (USD),الرصيد الافتتاحي (DJF),إجمالي المبيعات (USD),إجمالي المبيعات (DJF),إجمالي السداد (USD),إجمالي السداد (DJF),صافي المديونية (USD),صافي المديونية (DJF)\n")
        for (b in balances) {
            val c = b.customer
            sb.append("\"${c.name.replace("\"", "\"\"")}\",")
            sb.append("\"${c.area.replace("\"", "\"\"")}\",")
            sb.append("\"${c.phone}\",")
            sb.append("${c.openingBalanceUsd},")
            sb.append("${c.openingBalanceDjf},")
            sb.append("${b.totalInvoicesUsd},")
            sb.append("${b.totalInvoicesDjf},")
            sb.append("${b.totalPaymentsUsd},")
            sb.append("${b.totalPaymentsDjf},")
            sb.append("${b.netBalanceUsd},")
            sb.append("${b.netBalanceDjf}\n")
        }
        return sb.toString()
    }
}
