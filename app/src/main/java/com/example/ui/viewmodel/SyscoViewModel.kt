package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.demo.DemoData
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.pdf.PdfGenerator
import com.example.data.repository.SyscoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

enum class AppTab {
    DASHBOARD,
    CUSTOMERS,
    INVOICES,
    PAYMENTS,
    CATALOG,
    SETTINGS,
    AI_CHAT
}

enum class SubView {
    LIST,
    FORM
}

class SyscoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = SyscoRepository(db)

    // Current navigation state
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _isSplashVisible = MutableStateFlow(true)
    val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Sub-view toggles (List vs Add Form) for thumb-friendly mobile UX
    private val _customerSubView = MutableStateFlow(SubView.LIST)
    val customerSubView: StateFlow<SubView> = _customerSubView.asStateFlow()

    private val _invoiceSubView = MutableStateFlow(SubView.LIST)
    val invoiceSubView: StateFlow<SubView> = _invoiceSubView.asStateFlow()

    private val _paymentSubView = MutableStateFlow(SubView.LIST)
    val paymentSubView: StateFlow<SubView> = _paymentSubView.asStateFlow()

    private val _catalogSubView = MutableStateFlow(SubView.LIST)
    val catalogSubView: StateFlow<SubView> = _catalogSubView.asStateFlow()

    // Selected products for custom brochure
    private val _selectedProductIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedProductIds: StateFlow<Set<Long>> = _selectedProductIds.asStateFlow()

    // Editing states
    private val _editingCustomer = MutableStateFlow<Customer?>(null)
    val editingCustomer: StateFlow<Customer?> = _editingCustomer.asStateFlow()

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct.asStateFlow()

    // Data streams from repository
    val customers: StateFlow<List<Customer>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<Invoice>> = repository.invoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.payments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<CompanySettings> = repository.settings
        .map { it ?: DemoData.defaultSettings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoData.defaultSettings)

    val kpiSummary: StateFlow<KpiSummary> = repository.kpiSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KpiSummary())

    val customerBalances: StateFlow<List<CustomerBalance>> = repository.customerBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Persistent Shopping Cart State Flows (Room)
    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCartCount: StateFlow<Int> = repository.totalCartCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotalUsd: StateFlow<Double> = repository.cartTotalUsd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotalDjf: StateFlow<Double> = repository.cartTotalDjf
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Modals for Statement & Invoice preview
    private val _activeStatement = MutableStateFlow<Pair<CustomerBalance, List<LedgerEntry>>?>(null)
    val activeStatement: StateFlow<Pair<CustomerBalance, List<LedgerEntry>>?> = _activeStatement.asStateFlow()

    private val _activeInvoicePreview = MutableStateFlow<Pair<Invoice, List<InvoiceItem>>?>(null)
    val activeInvoicePreview: StateFlow<Pair<Invoice, List<InvoiceItem>>?> = _activeInvoicePreview.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            DemoData.populateIfEmpty(db)
        }
    }

    fun dismissSplash() {
        _isSplashVisible.value = false
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setCustomerSubView(view: SubView) {
        _customerSubView.value = view
        if (view == SubView.LIST) _editingCustomer.value = null
    }

    fun setInvoiceSubView(view: SubView) {
        _invoiceSubView.value = view
    }

    fun setPaymentSubView(view: SubView) {
        _paymentSubView.value = view
    }

    fun setCatalogSubView(view: SubView) {
        _catalogSubView.value = view
        if (view == SubView.LIST) _editingProduct.value = null
    }

    fun startEditCustomer(customer: Customer) {
        _editingCustomer.value = customer
        _customerSubView.value = SubView.FORM
    }

    fun startEditProduct(product: Product) {
        _editingProduct.value = product
        _catalogSubView.value = SubView.FORM
    }

    // Customer operations
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCustomer(customer)
            _editingCustomer.value = null
            _customerSubView.value = SubView.LIST
            _userMessage.value = "تم حفظ بيانات العميل بنجاح"
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(customer)
            _userMessage.value = "تم حذف العميل"
        }
    }

    // Product operations
    fun saveProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProduct(product)
            _editingProduct.value = null
            _catalogSubView.value = SubView.LIST
            _userMessage.value = "تم تحديث كتالوج المنتجات والمخزون"
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
            _userMessage.value = "تم حذف المنتج من الكتالوج"
        }
    }

    fun toggleProductSelection(productId: Long) {
        val current = _selectedProductIds.value.toMutableSet()
        if (current.contains(productId)) {
            current.remove(productId)
        } else {
            current.add(productId)
        }
        _selectedProductIds.value = current
    }

    fun clearProductSelections() {
        _selectedProductIds.value = emptySet()
    }

    // Invoice operations
    fun createInvoice(invoice: Invoice, items: List<InvoiceItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createInvoice(invoice, items)
            _invoiceSubView.value = SubView.LIST
            _userMessage.value = "تم إصدار الفاتورة (${invoice.invoiceNumber}) وخصم الكميات من المستودع"
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInvoice(invoice)
            _userMessage.value = "تم إلغاء الفاتورة وإعادة الكميات للمخزون"
        }
    }

    // Payment operations
    fun recordPayment(payment: Payment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordPayment(payment)
            _paymentSubView.value = SubView.LIST
            _userMessage.value = "تم تسجيل سند القبض (${payment.receiptNumber}) وتحديث رصيد العميل"
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePayment(payment)
            _userMessage.value = "تم حذف سند القبض"
        }
    }

    // Shopping Cart Operations (Room Persistent)
    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            if (product.stock <= 0) {
                _userMessage.value = "⚠️ عفواً، صنف '${product.name}' غير متوفر حالياً في المخزون"
                return@launch
            }
            val success = repository.addToCart(product, quantity)
            if (success) {
                _userMessage.value = "🛒 تمت إضافة '${product.name}' ($quantity ق) إلى سلة التسوق"
            } else {
                _userMessage.value = "⚠️ تعذر إضافة الصنف إلى السلة"
            }
        }
    }

    fun updateCartQuantity(productId: Long, newQuantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCartQuantity(productId, newQuantity)
        }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeFromCart(productId)
            _userMessage.value = "تم حذف الصنف من سلة التسوق"
        }
    }

    fun clearCart() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCart()
            _userMessage.value = "تم إفراغ سلة التسوق"
        }
    }

    fun getCartItemFlow(productId: Long): Flow<CartItem?> = repository.getCartItemFlow(productId)

    // Settings & Signature
    fun saveSettings(newSettings: CompanySettings) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(newSettings)
            _userMessage.value = "تم حفظ الإعدادات وسعر الصرف بنجاح"
        }
    }

    fun saveSignature(base64: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            val updated = current.copy(signatureBase64 = base64)
            repository.saveSettings(updated)
            _userMessage.value = "تم حفظ التوقيع الإلكتروني للاعتماد في الـ PDF"
        }
    }

    // Statement preview & actions
    fun openCustomerStatement(balance: CustomerBalance) {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = repository.getCustomerStatementEntries(balance.customer.id)
            _activeStatement.value = Pair(balance, entries)
        }
    }

    fun closeCustomerStatement() {
        _activeStatement.value = null
    }

    fun openInvoicePreview(invoice: Invoice) {
        val items = JsonHelper.deserializeInvoiceItems(invoice.itemsJson)
        _activeInvoicePreview.value = Pair(invoice, items)
    }

    fun closeInvoicePreview() {
        _activeInvoicePreview.value = null
    }

    // PDF Generation & Sharing
    fun shareCustomerStatementPdf(context: Context, balance: CustomerBalance, entries: List<LedgerEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = PdfGenerator.generateCustomerStatementPdf(context, settings.value, balance, entries)
            PdfGenerator.sharePdfFile(context, file, "كشف حساب عميل - ${balance.customer.name}")
        }
    }

    fun shareInvoicePdf(context: Context, invoice: Invoice, items: List<InvoiceItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = PdfGenerator.generateInvoicePdf(context, settings.value, invoice, items)
            PdfGenerator.sharePdfFile(context, file, "فاتورة مبيعات رقم ${invoice.invoiceNumber}")
        }
    }

    fun shareCatalogPdf(context: Context, isCustomOnly: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val allProds = products.value
            val prodsToExport = if (isCustomOnly) {
                allProds.filter { _selectedProductIds.value.contains(it.id) }
            } else {
                allProds
            }
            if (prodsToExport.isEmpty()) {
                _userMessage.value = "يرجى تحديد أصناف أولاً للتصدير المخصص"
                return@launch
            }
            val file = PdfGenerator.generateCatalogPdf(context, settings.value, prodsToExport, isCustomOnly)
            PdfGenerator.sharePdfFile(context, file, if (isCustomOnly) "عرض أسعار مخصص" else "كتالوج شركة SYSCO LTD")
        }
    }

    // WhatsApp Direct Action
    fun sendWhatsAppBalanceMessage(context: Context, balance: CustomerBalance) {
        val c = balance.customer
        val cleanPhone = c.phone.replace("[^0-9+]".toRegex(), "")
        val s = settings.value

        val messageText = """
السلام عليكم ورحمة الله وبركاته
السيد / ${c.name} المحترم،
تحية طيبة من شركة *${s.companyName}* — جيبوتي.

نود إحاطتكم علماً بموقف رصيد حسابكم الحالي:
💵 الرصيد المستحق (بالدولار): *${String.format(java.util.Locale.US, "%.2f", balance.netBalanceUsd)} USD*
🇩🇯 الرصيد المستحق (بالفرنك): *${String.format(java.util.Locale.US, "%,.0f", balance.netBalanceDjf)} DJF*

سعر الصرف المعتمد: 1 USD = ${s.exchangeRate} DJF
شاكرين ومقدرين حسن تعاونكم الدائم معنا.
${s.salesRepName}
هاتف: ${s.phone}
        """.trimIndent()

        try {
            val encoded = URLEncoder.encode(messageText, "UTF-8")
            val target = if (cleanPhone.startsWith("+")) cleanPhone.removePrefix("+") else cleanPhone
            val url = "https://api.whatsapp.com/send?phone=$target&text=$encoded"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            _userMessage.value = "تعذر فتح تطبيق واتساب. تأكد من تثبيته وصحة رقم الهاتف."
        }
    }

    // Export CSV
    fun exportCsvLedger(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val balances = customerBalances.value
            val csvContent = repository.exportCsvLedger(balances)
            val docsDir = File(context.cacheDir, "docs")
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, "SYSCO_Customer_Ledger_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            fos.write(csvContent.toByteArray(Charsets.UTF_8))
            fos.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير أرصدة عملاء SYSCO LTD")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "تصدير كشف Excel / CSV"))
        }
    }

    // Export JSON Backup
    fun exportBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.exportBackupJson()
            val docsDir = File(context.cacheDir, "docs")
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, "SYSCO_Backup_${System.currentTimeMillis()}.json")
            val fos = FileOutputStream(file)
            fos.write(json.toByteArray(Charsets.UTF_8))
            fos.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية SYSCO LTD")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة النسخة الاحتياطية"))
        }
    }
}
