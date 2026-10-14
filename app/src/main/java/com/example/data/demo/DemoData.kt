package com.example.data.demo

import com.example.data.local.AppDatabase
import com.example.data.model.*

object DemoData {
    val defaultSettings = CompanySettings(
        id = 1,
        companyName = "SYSCO LTD — Djibouti",
        salesRepName = "مندوب المبيعات: محمد علي عمر",
        address = "شارع البوليفارد التجاري، المنطقة الحرة، جيبوتي",
        phone = "+253 77 12 34 56 / +253 21 35 00 00",
        taxId = "NIF: 0014589-DJ / RC: 2021-B-89",
        exchangeRate = 177.72,
        webhookUrl = ""
    )

    suspend fun populateIfEmpty(db: AppDatabase) {
        val settingsDao = db.settingsDao()
        if (settingsDao.getSettingsSync() == null) {
            settingsDao.saveSettings(defaultSettings)
        }

        val customerDao = db.customerDao()
        val productDao = db.productDao()
        val invoiceDao = db.invoiceDao()
        val paymentDao = db.paymentDao()

        // Check if customers already exist
        val existingCust = customerDao.getCustomerById(1L)
        if (existingCust != null) return

        // 1. Insert Products
        val products = listOf(
            Product(
                id = 1,
                name = "أرز بسمتي فاخر كوهينور 25 كجم",
                category = "أرز وحبوب",
                specs = "نخب أول حبة طويلة معتقة - كيس خيش 25 كجم",
                priceUsd = 26.50,
                priceDjf = 4710.0,
                stock = 120,
                lowStockThreshold = 10
            ),
            Product(
                id = 2,
                name = "زيت طهي نقي كريستال 5 لتر",
                category = "زيوت ودهون",
                specs = "زيت ذرة مكرر خالي من الكوليسترول - كرتون 4 جوالين",
                priceUsd = 8.50,
                priceDjf = 1510.0,
                stock = 85,
                lowStockThreshold = 10
            ),
            Product(
                id = 3,
                name = "سكر برازيلي ناعم المذاق 50 كجم",
                category = "مواد تموينية",
                specs = "سكر أبيض نقي مطحون فائق الجودة - كيس 50 كجم",
                priceUsd = 38.00,
                priceDjf = 6753.0,
                stock = 54,
                lowStockThreshold = 10
            ),
            Product(
                id = 4,
                name = "حليب بودرة مجفف ممتاز 2.5 كجم",
                category = "ألبان ومجففات",
                specs = "حليب مجفف سريع الذوبان كامل الدسم - عبوة معدنية",
                priceUsd = 16.50,
                priceDjf = 2932.0,
                stock = 42,
                lowStockThreshold = 10
            ),
            Product(
                id = 5,
                name = "شاي كيني أسود نخب إكسبريس 5 كجم",
                category = "مشروبات ساخنة",
                specs = "أوراق شاي مفروم عالي النكهة - كيس ورقي معزول",
                priceUsd = 19.00,
                priceDjf = 3377.0,
                stock = 7, // Low Stock alert!
                lowStockThreshold = 10
            ),
            Product(
                id = 6,
                name = "معجون طماطم مركز 800 جم (كرتون 12)",
                category = "معلبات",
                specs = "تركيز 28-30% درجة أولى - كرتون 12 علبة",
                priceUsd = 14.00,
                priceDjf = 2488.0,
                stock = 6, // Low Stock alert!
                lowStockThreshold = 10
            ),
            Product(
                id = 7,
                name = "مكرونة ممتازة أصابع 500 جم (كرتون 24)",
                category = "معكرونة وبقوليات",
                specs = "قمح ديورم قاسي 100% - كرتون 24 كيس",
                priceUsd = 13.50,
                priceDjf = 2399.0,
                stock = 95,
                lowStockThreshold = 10
            )
        )
        products.forEach { productDao.insertProduct(it) }

        // 2. Insert Customers
        val customers = listOf(
            Customer(
                id = 1,
                name = "سوبرماركت النور والبركة",
                phone = "+25377812345",
                area = "بلبلا (Balbala) - جيبوتي",
                openingBalanceUsd = 450.0,
                openingBalanceDjf = 80000.0,
                notes = "عميل مميز - التسليم الصباحي"
            ),
            Customer(
                id = 2,
                name = "مؤسسة الفاروق للتجارة العامة",
                phone = "+25377654321",
                area = "شارع 26 - وسط المدينة",
                openingBalanceUsd = 850.0,
                openingBalanceDjf = 150000.0,
                notes = "دفع منتظم عبر Waafi / شيكات"
            ),
            Customer(
                id = 3,
                name = "هايبر ماركت كافيه دو باريس",
                phone = "+25377201122",
                area = "حي أراس الحرة - جيبوتي العاصمة",
                openingBalanceUsd = 0.0,
                openingBalanceDjf = 0.0,
                notes = "عميل جديد - طلبية أسبوعية"
            ),
            Customer(
                id = 4,
                name = "تموينات السلام والرخاء",
                phone = "+25377443322",
                area = "حي الهدى (Hayabley)",
                openingBalanceUsd = 200.0,
                openingBalanceDjf = 35000.0,
                notes = "تعامل نقدي ومحفظة D-Money"
            )
        )
        customers.forEach { customerDao.insertCustomer(it) }

        // 3. Insert Demo Invoices
        val invoiceItems1 = listOf(
            InvoiceItem(productId = 1, productName = "أرز بسمتي فاخر كوهينور 25 كجم", quantity = 10, unitPrice = 26.50, total = 265.0),
            InvoiceItem(productId = 2, productName = "زيت طهي نقي كريستال 5 لتر", quantity = 15, unitPrice = 8.50, total = 127.50)
        )
        val invoice1 = Invoice(
            id = 1,
            invoiceNumber = "INV-2026-001",
            customerId = 1,
            customerName = "سوبرماركت النور والبركة",
            date = System.currentTimeMillis() - 86400000L * 3, // 3 days ago
            currency = "USD",
            itemsJson = JsonHelper.serializeInvoiceItems(invoiceItems1),
            discount = 12.50,
            subtotal = 392.50,
            netTotal = 380.00,
            notes = "تم التوصيل إلى مستودع بلبلا"
        )
        invoiceDao.insertInvoice(invoice1)

        val invoiceItems2 = listOf(
            InvoiceItem(productId = 3, productName = "سكر برازيلي ناعم المذاق 50 كجم", quantity = 5, unitPrice = 6753.0, total = 33765.0),
            InvoiceItem(productId = 4, productName = "حليب بودرة مجفف ممتاز 2.5 كجم", quantity = 8, unitPrice = 2932.0, total = 23456.0)
        )
        val invoice2 = Invoice(
            id = 2,
            invoiceNumber = "INV-2026-002",
            customerId = 2,
            customerName = "مؤسسة الفاروق للتجارة العامة",
            date = System.currentTimeMillis() - 86400000L * 2,
            currency = "DJF",
            itemsJson = JsonHelper.serializeInvoiceItems(invoiceItems2),
            discount = 1221.0,
            subtotal = 57221.0,
            netTotal = 56000.0,
            notes = "فاتورة نصف شهرية"
        )
        invoiceDao.insertInvoice(invoice2)

        // 4. Insert Demo Payments (سندات القبض)
        val payment1 = Payment(
            id = 1,
            receiptNumber = "REC-2026-001",
            customerId = 1,
            customerName = "سوبرماركت النور والبركة",
            date = System.currentTimeMillis() - 86400000L,
            currency = "USD",
            amount = 250.00,
            paymentMethod = "D-Money / Waafi",
            notes = "دفعة جزئية عن حساب الفاتورة"
        )
        paymentDao.insertPayment(payment1)

        val payment2 = Payment(
            id = 2,
            receiptNumber = "REC-2026-002",
            customerId = 2,
            customerName = "مؤسسة الفاروق للتجارة العامة",
            date = System.currentTimeMillis() - 86400000L,
            currency = "DJF",
            amount = 50000.00,
            paymentMethod = "تحويل بنكي (Bank Transfer)",
            notes = "سداد عبر بنك BCIMR"
        )
        paymentDao.insertPayment(payment2)
    }
}
