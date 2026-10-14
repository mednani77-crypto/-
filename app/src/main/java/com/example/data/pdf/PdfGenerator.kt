package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    private fun decodeBase64Bitmap(base64: String): Bitmap? {
        return try {
            if (base64.isBlank()) return null
            val clean = if (base64.contains(",")) base64.substringAfter(",") else base64
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun getOutputPdfFile(context: Context, prefix: String): File {
        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        return File(docsDir, "${prefix}_${System.currentTimeMillis()}.pdf")
    }

    // 1. Generate Customer Account Statement PDF
    fun generateCustomerStatementPdf(
        context: Context,
        settings: CompanySettings,
        balance: CustomerBalance,
        entries: List<LedgerEntry>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawStatementContent(canvas, settings, balance, entries)

        document.finishPage(page)
        val file = getOutputPdfFile(context, "Statement_${balance.customer.name.replace("\\s+".toRegex(), "_")}")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()
        return file
    }

    private fun drawStatementContent(
        canvas: Canvas,
        settings: CompanySettings,
        balance: CustomerBalance,
        entries: List<LedgerEntry>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Deep Navy Header Banner
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Accent Gold Strip
        paint.color = Color.parseColor("#D97706")
        canvas.drawRect(0f, 95f, PAGE_WIDTH.toFloat(), 100f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(settings.companyName, 30f, 40f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("${settings.address} | ${settings.phone} | ${settings.taxId}", 30f, 60f, paint)
        canvas.drawText("سعر الصرف المعتمد: 1 USD = ${settings.exchangeRate} DJF", 30f, 78f, paint)

        // Document Badge
        paint.color = Color.parseColor("#D97706")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText("كشف حساب عميل", PAGE_WIDTH - 150f, 45f, paint)
        paint.color = Color.LTGRAY
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("تاريخ الإصدار: ${dateFormat.format(Date())}", PAGE_WIDTH - 180f, 70f, paint)

        // Customer Info Card
        val custCard = RectF(30f, 115f, PAGE_WIDTH - 30f, 195f)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(custCard, 8f, 8f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(custCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        val c = balance.customer
        canvas.drawText("العميل: ${c.name}", 45f, 140f, paint)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#334155")
        canvas.drawText("المنطقة / العنوان: ${c.area}", 45f, 160f, paint)
        canvas.drawText("رقم الهاتف: ${c.phone}", 45f, 180f, paint)

        // Opening Balances Box
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("الرصيد الافتتاحي:", 340f, 140f, paint)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("USD: $ ${String.format(Locale.US, "%.2f", c.openingBalanceUsd)}", 340f, 160f, paint)
        canvas.drawText("DJF: ${String.format(Locale.US, "%,.0f", c.openingBalanceDjf)} DJF", 340f, 180f, paint)

        // Transactions Table Header
        var y = 220f
        paint.color = Color.parseColor("#1E3A8A")
        canvas.drawRect(30f, y, PAGE_WIDTH - 30f, y + 26f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("التاريخ", 40f, y + 17f, paint)
        canvas.drawText("المرجع / السند", 120f, y + 17f, paint)
        canvas.drawText("البيان", 210f, y + 17f, paint)
        canvas.drawText("العملة", 330f, y + 17f, paint)
        canvas.drawText("مدين (فاتورة)", 390f, y + 17f, paint)
        canvas.drawText("دائن (سداد)", 480f, y + 17f, paint)

        y += 26f
        paint.isFakeBoldText = false
        val itemDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        val maxRows = 14
        val displayEntries = entries.take(maxRows)
        for ((idx, entry) in displayEntries.withIndex()) {
            paint.color = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
            canvas.drawRect(30f, y, PAGE_WIDTH - 30f, y + 22f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 9f
            canvas.drawText(itemDateFormat.format(Date(entry.date)), 40f, y + 15f, paint)
            canvas.drawText(entry.reference, 120f, y + 15f, paint)
            canvas.drawText(entry.description, 210f, y + 15f, paint)
            canvas.drawText(entry.currency, 335f, y + 15f, paint)

            if (entry.debit > 0) {
                paint.color = Color.parseColor("#D97706") // Amber
                canvas.drawText(String.format(Locale.US, "%.2f", entry.debit), 395f, y + 15f, paint)
            } else {
                paint.color = Color.GRAY
                canvas.drawText("-", 415f, y + 15f, paint)
            }

            if (entry.credit > 0) {
                paint.color = Color.parseColor("#059669") // Emerald
                canvas.drawText(String.format(Locale.US, "%.2f", entry.credit), 485f, y + 15f, paint)
            } else {
                paint.color = Color.GRAY
                canvas.drawText("-", 505f, y + 15f, paint)
            }

            // row line
            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(30f, y + 22f, PAGE_WIDTH - 30f, y + 22f, paint)
            y += 22f
        }

        // Summary Net Balance Box
        y = 580f
        val summaryCard = RectF(30f, y, PAGE_WIDTH - 30f, y + 110f)
        paint.color = Color.parseColor("#EFF6FF")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(summaryCard, 8f, 8f, paint)
        paint.color = Color.parseColor("#BFDBFE")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(summaryCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("ملخص رصيد الحساب المتبقي بذمة العميل", 45f, y + 25f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#334155")
        canvas.drawText("إجمالي الفواتير (USD): $ ${String.format(Locale.US, "%.2f", balance.totalInvoicesUsd)}", 45f, y + 50f, paint)
        canvas.drawText("إجمالي السدادات (USD): $ ${String.format(Locale.US, "%.2f", balance.totalPaymentsUsd)}", 45f, y + 70f, paint)

        canvas.drawText("إجمالي الفواتير (DJF): ${String.format(Locale.US, "%,.0f", balance.totalInvoicesDjf)} DJF", 220f, y + 50f, paint)
        canvas.drawText("إجمالي السدادات (DJF): ${String.format(Locale.US, "%,.0f", balance.totalPaymentsDjf)} DJF", 220f, y + 70f, paint)

        // Highlight Box for Net Balances
        paint.color = Color.parseColor("#E11D48")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("الصافي المستحق بالدولار:", 400f, y + 35f, paint)
        paint.textSize = 14f
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", balance.netBalanceUsd)}", 400f, y + 55f, paint)

        paint.textSize = 11f
        canvas.drawText("الصافي بالفرنك:", 400f, y + 80f, paint)
        paint.textSize = 13f
        canvas.drawText("${String.format(Locale.US, "%,.0f", balance.netBalanceDjf)} DJF", 400f, y + 100f, paint)

        // Footer & Signature Area
        y = 715f
        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText(settings.salesRepName, 60f, y, paint)
        canvas.drawText("ختم واعتماد شركة SYSCO LTD", PAGE_WIDTH - 200f, y, paint)

        // Render Touch Signature if available
        val sigBmp = decodeBase64Bitmap(settings.signatureBase64)
        if (sigBmp != null) {
            val destRect = RectF(60f, y + 10f, 180f, y + 65f)
            canvas.drawBitmap(sigBmp, null, destRect, null)
        } else {
            paint.color = Color.GRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawLine(60f, y + 45f, 200f, y + 45f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 8f
            canvas.drawText("توقيع المندوب", 80f, y + 60f, paint)
        }

        // Bottom disclaimer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("كشف الحساب هذا رسمي ومعتمد من برنامج مبيعات SYSCO LTD — جمهورية جيبوتي", 120f, PAGE_HEIGHT - 20f, paint)
    }

    // 2. Generate Detailed Sales Invoice PDF
    fun generateInvoicePdf(
        context: Context,
        settings: CompanySettings,
        invoice: Invoice,
        items: List<InvoiceItem>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoiceContent(canvas, settings, invoice, items)

        document.finishPage(page)
        val file = getOutputPdfFile(context, "Invoice_${invoice.invoiceNumber}")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()
        return file
    }

    private fun drawInvoiceContent(
        canvas: Canvas,
        settings: CompanySettings,
        invoice: Invoice,
        items: List<InvoiceItem>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)
        paint.color = Color.parseColor("#D97706")
        canvas.drawRect(0f, 95f, PAGE_WIDTH.toFloat(), 100f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(settings.companyName, 30f, 40f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("${settings.address} | هاتف: ${settings.phone}", 30f, 60f, paint)
        canvas.drawText("الرقم الضريبي: ${settings.taxId}", 30f, 78f, paint)

        paint.color = Color.parseColor("#D97706")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("فاتورة مبيعات", PAGE_WIDTH - 140f, 45f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        canvas.drawText(invoice.invoiceNumber, PAGE_WIDTH - 140f, 65f, paint)
        paint.color = Color.LTGRAY
        paint.textSize = 9f
        canvas.drawText("التاريخ: ${dateFormat.format(Date(invoice.date))}", PAGE_WIDTH - 160f, 82f, paint)

        // Details card
        val infoCard = RectF(30f, 115f, PAGE_WIDTH - 30f, 185f)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(infoCard, 8f, 8f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(infoCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("السيد / السادة: ${invoice.customerName}", 45f, 140f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#475569")
        canvas.drawText("عملة الفاتورة: ${invoice.currency}", 45f, 162f, paint)
        canvas.drawText(settings.salesRepName, 300f, 140f, paint)
        canvas.drawText("ملاحظات: ${invoice.notes.ifBlank { "لا توجد" }}", 300f, 162f, paint)

        // Items Table
        var y = 210f
        paint.color = Color.parseColor("#D97706")
        canvas.drawRect(30f, y, PAGE_WIDTH - 30f, y + 26f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("#", 40f, y + 17f, paint)
        canvas.drawText("اسم الصنف والمواصفات", 70f, y + 17f, paint)
        canvas.drawText("الكمية", 320f, y + 17f, paint)
        canvas.drawText("سعر الوحدة (${invoice.currency})", 380f, y + 17f, paint)
        canvas.drawText("الإجمالي (${invoice.currency})", 470f, y + 17f, paint)

        y += 26f
        paint.isFakeBoldText = false

        for ((idx, item) in items.withIndex()) {
            paint.color = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#FFFBEB")
            canvas.drawRect(30f, y, PAGE_WIDTH - 30f, y + 24f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 9f
            canvas.drawText("${idx + 1}", 40f, y + 16f, paint)
            canvas.drawText(item.productName, 70f, y + 16f, paint)
            canvas.drawText("${item.quantity}", 330f, y + 16f, paint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), 390f, y + 16f, paint)
            paint.isFakeBoldText = true
            canvas.drawText(String.format(Locale.US, "%.2f", item.total), 480f, y + 16f, paint)
            paint.isFakeBoldText = false

            paint.color = Color.parseColor("#FED7AA")
            paint.strokeWidth = 0.5f
            canvas.drawLine(30f, y + 24f, PAGE_WIDTH - 30f, y + 24f, paint)
            y += 24f
        }

        // Totals Box
        y += 20f
        val totalsCard = RectF(PAGE_WIDTH - 260f, y, PAGE_WIDTH - 30f, y + 95f)
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(totalsCard, 8f, 8f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(totalsCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        canvas.drawText("المجموع قبل الخصم:", PAGE_WIDTH - 245f, y + 25f, paint)
        canvas.drawText("${String.format(Locale.US, "%.2f", invoice.subtotal)} ${invoice.currency}", PAGE_WIDTH - 120f, y + 25f, paint)

        canvas.drawText("الخصم الممنوح:", PAGE_WIDTH - 245f, y + 48f, paint)
        paint.color = Color.parseColor("#E11D48")
        canvas.drawText("- ${String.format(Locale.US, "%.2f", invoice.discount)} ${invoice.currency}", PAGE_WIDTH - 120f, y + 48f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("الصافي النهائي:", PAGE_WIDTH - 245f, y + 78f, paint)
        paint.color = Color.parseColor("#D97706")
        canvas.drawText("${String.format(Locale.US, "%.2f", invoice.netTotal)} ${invoice.currency}", PAGE_WIDTH - 120f, y + 78f, paint)

        // Signature
        y = 715f
        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("المستلم / العميل", 60f, y, paint)
        canvas.drawText("المندوب / المبيعات", PAGE_WIDTH - 200f, y, paint)

        val sigBmp = decodeBase64Bitmap(settings.signatureBase64)
        if (sigBmp != null) {
            val destRect = RectF(PAGE_WIDTH - 200f, y + 10f, PAGE_WIDTH - 80f, y + 65f)
            canvas.drawBitmap(sigBmp, null, destRect, null)
        } else {
            paint.color = Color.GRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawLine(PAGE_WIDTH - 200f, y + 45f, PAGE_WIDTH - 60f, y + 45f, paint)
        }

        // Bottom text
        paint.color = Color.parseColor("#94A3B8")
        paint.style = Paint.Style.FILL
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("شكراً لتعاملكم مع SYSCO LTD — جيبوتي", 220f, PAGE_HEIGHT - 20f, paint)
    }

    // 3. Generate Product Catalog Brochure PDF (All or Custom Selected)
    fun generateCatalogPdf(
        context: Context,
        settings: CompanySettings,
        products: List<Product>,
        isCustomSelection: Boolean
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawCatalogContent(canvas, settings, products, isCustomSelection)

        document.finishPage(page)
        val file = getOutputPdfFile(context, if (isCustomSelection) "Custom_Brochure" else "Full_Catalog")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()
        return file
    }

    private fun drawCatalogContent(
        canvas: Canvas,
        settings: CompanySettings,
        products: List<Product>,
        isCustomSelection: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Indigo Header Banner for Catalog
        paint.color = Color.parseColor("#4F46E5")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)
        paint.color = Color.parseColor("#D97706")
        canvas.drawRect(0f, 95f, PAGE_WIDTH.toFloat(), 100f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(settings.companyName, 30f, 40f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("كتالوج عروض ومنتجات الشركة المعتمدة — جيبوتي", 30f, 60f, paint)
        canvas.drawText("للطلب والاستفسار: ${settings.phone} | سعر الصرف: 1 USD = ${settings.exchangeRate} DJF", 30f, 78f, paint)

        paint.color = Color.parseColor("#FBBF24")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        val title = if (isCustomSelection) "عرض سعر مخصص للعميل" else "كتالوج المنتجات الشامل"
        canvas.drawText(title, PAGE_WIDTH - 180f, 45f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9f
        canvas.drawText("عدد الأصناف: ${products.size}", PAGE_WIDTH - 120f, 70f, paint)

        // Products Grid / List
        var y = 120f
        paint.textSize = 9f

        val displayProducts = products.take(9)
        for (product in displayProducts) {
            val itemCard = RectF(30f, y, PAGE_WIDTH - 30f, y + 54f)
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(itemCard, 6f, 6f, paint)
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(itemCard, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Product Icon box
            val iconRect = RectF(40f, y + 7f, 80f, y + 47f)
            paint.color = Color.parseColor("#EEF2FF")
            canvas.drawRoundRect(iconRect, 4f, 4f, paint)
            paint.color = Color.parseColor("#4F46E5")
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("SYSCO", 44f, y + 31f, paint)

            // Name & Specs
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText(product.name, 95f, y + 22f, paint)

            paint.color = Color.parseColor("#64748B")
            paint.textSize = 9f
            paint.isFakeBoldText = false
            canvas.drawText("${product.category} | ${product.specs}", 95f, y + 40f, paint)

            // Dual Price & Stock Box
            paint.color = Color.parseColor("#059669") // Green price
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("$ ${String.format(Locale.US, "%.2f", product.priceUsd)}", 380f, y + 22f, paint)

            paint.color = Color.parseColor("#D97706") // DJF price
            canvas.drawText("${String.format(Locale.US, "%,.0f", product.priceDjf)} DJF", 380f, y + 40f, paint)

            // Stock badge
            if (product.stock <= product.lowStockThreshold) {
                paint.color = Color.parseColor("#E11D48")
                paint.textSize = 8f
                paint.isFakeBoldText = true
                canvas.drawText("المتبقي: ${product.stock} ق", 480f, y + 30f, paint)
            } else {
                paint.color = Color.parseColor("#059669")
                paint.textSize = 8f
                paint.isFakeBoldText = false
                canvas.drawText("متوفر: ${product.stock} ق", 480f, y + 30f, paint)
            }

            y += 62f
        }

        // Footer
        y = 720f
        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("مندوب المبيعات المعتمد: ${settings.salesRepName}", 40f, y, paint)

        val sigBmp = decodeBase64Bitmap(settings.signatureBase64)
        if (sigBmp != null) {
            val destRect = RectF(PAGE_WIDTH - 180f, y - 10f, PAGE_WIDTH - 60f, y + 45f)
            canvas.drawBitmap(sigBmp, null, destRect, null)
        }

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("SYSCO LTD Djibouti — جميع الحقوق محفوظة والأسعار قابلة للتحديث حسب الكميات", 110f, PAGE_HEIGHT - 20f, paint)
    }

    // Share PDF file using Intent with FileProvider
    fun sharePdfFile(context: Context, file: File, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "مشاركة المستند PDF"))
        } catch (_: Exception) {
        }
    }
}
