package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val area: String,
    val openingBalanceUsd: Double = 0.0,
    val openingBalanceDjf: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val specs: String = "",
    val priceUsd: Double = 0.0,
    val priceDjf: Double = 0.0,
    val stock: Int = 0,
    val imageBase64: String = "",
    val lowStockThreshold: Int = 10
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey
    val productId: Long,
    val productName: String,
    val category: String,
    val specs: String = "",
    val priceUsd: Double = 0.0,
    val priceDjf: Double = 0.0,
    val quantity: Int = 1,
    val stock: Int = 0,
    val imageBase64: String = "",
    val addedAt: Long = System.currentTimeMillis()
) {
    val totalUsd: Double get() = priceUsd * quantity
    val totalDjf: Double get() = priceDjf * quantity
}

data class InvoiceItem(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double = quantity * unitPrice
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long,
    val customerName: String,
    val date: Long = System.currentTimeMillis(),
    val currency: String = "USD", // "USD" or "DJF"
    val itemsJson: String = "[]",
    val discount: Double = 0.0,
    val subtotal: Double = 0.0,
    val netTotal: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String,
    val customerId: Long,
    val customerName: String,
    val date: Long = System.currentTimeMillis(),
    val currency: String = "USD", // "USD" or "DJF"
    val amount: Double = 0.0,
    val paymentMethod: String = "نقدي (Cash)",
    val notes: String = ""
)

@Entity(tableName = "company_settings")
data class CompanySettings(
    @PrimaryKey
    val id: Int = 1,
    val companyName: String = "SYSCO LTD — Djibouti",
    val salesRepName: String = "مندوب مبيعات جيبوتي",
    val address: String = "شارع البوليفارد، المنطقة الحرة، جيبوتي",
    val phone: String = "+253 77 00 00 00 / 21 35 00 00",
    val taxId: String = "NIF: 0014589-DJ / RC: 2021-B-89",
    val exchangeRate: Double = 177.72, // 1 USD = 177.72 DJF
    val logoBase64: String = "",
    val signatureBase64: String = "",
    val webhookUrl: String = ""
)

// Computed models for UI & Statements
sealed class LedgerEntry(
    open val date: Long,
    open val reference: String,
    open val description: String,
    open val currency: String,
    open val debit: Double,  // Invoice (increases customer debt)
    open val credit: Double  // Payment (decreases customer debt)
) {
    data class InvoiceEntry(
        val invoice: Invoice
    ) : LedgerEntry(
        date = invoice.date,
        reference = invoice.invoiceNumber,
        description = "فاتورة مبيعات",
        currency = invoice.currency,
        debit = invoice.netTotal,
        credit = 0.0
    )

    data class PaymentEntry(
        val payment: Payment
    ) : LedgerEntry(
        date = payment.date,
        reference = payment.receiptNumber,
        description = "سند قبض (${payment.paymentMethod})",
        currency = payment.currency,
        debit = 0.0,
        credit = payment.amount
    )
}

data class CustomerBalance(
    val customer: Customer,
    val totalInvoicesUsd: Double,
    val totalInvoicesDjf: Double,
    val totalPaymentsUsd: Double,
    val totalPaymentsDjf: Double,
    val netBalanceUsd: Double,
    val netBalanceDjf: Double
)

data class KpiSummary(
    val totalSalesUsd: Double = 0.0,
    val totalSalesDjf: Double = 0.0,
    val totalCollectionsUsd: Double = 0.0,
    val totalCollectionsDjf: Double = 0.0,
    val netReceivablesUsd: Double = 0.0,
    val netReceivablesDjf: Double = 0.0,
    val customerCount: Int = 0,
    val productCount: Int = 0
)
