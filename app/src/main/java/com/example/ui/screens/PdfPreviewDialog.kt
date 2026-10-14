package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerStatementDialog(
    balance: CustomerBalance,
    entries: List<LedgerEntry>,
    settings: CompanySettings,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val c = balance.customer

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "معاينة كشف الحساب الرسمي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyBrand
                        )
                        Text(
                            text = settings.companyName,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = BorderLight)

                // Customer Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("العميل: ${c.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("المنطقة: ${c.area} • الهاتف: ${c.phone}", fontSize = 11.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الرصيد الافتتاحي: $ ${String.format(Locale.US, "%.2f", c.openingBalanceUsd)}", fontSize = 11.sp, color = TextSecondary)
                            Text("${String.format(Locale.US, "%,.0f", c.openingBalanceDjf)} DJF", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                // Table of Transactions
                Text("سجل المعاملات والفواتير والسدادات:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries) { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${entry.description} (${entry.reference})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = dateFormat.format(Date(entry.date)),
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                if (entry.debit > 0) {
                                    Text(
                                        text = "+${String.format(Locale.US, "%.2f", entry.debit)} ${entry.currency}",
                                        color = AmberSales,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text(
                                        text = "-${String.format(Locale.US, "%.2f", entry.credit)} ${entry.currency}",
                                        color = EmeraldCollection,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Net Balance Summary Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الصافي المستحق (USD):", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "$ ${String.format(Locale.US, "%.2f", balance.netBalanceUsd)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = RoseReceivable
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("الصافي المستحق (DJF):", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", balance.netBalanceDjf)} DJF",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = RoseReceivable
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إغلاق")
                    }

                    Button(
                        onClick = onSharePdf,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBrand)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة كشف الحساب PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoicePreviewDialog(
    invoice: Invoice,
    items: List<InvoiceItem>,
    settings: CompanySettings,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "معاينة الفاتورة ${invoice.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AmberSalesDark
                        )
                        Text(
                            text = "العميل: ${invoice.customerName} • ${dateFormat.format(Date(invoice.date))}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = BorderLight)

                // Items list
                Text("تفاصيل الأصناف المباعة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFED7AA))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        "الكمية: ${item.quantity} × ${String.format(Locale.US, "%.2f", item.unitPrice)} ${invoice.currency}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Text(
                                    text = "${String.format(Locale.US, "%.2f", item.total)} ${invoice.currency}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AmberSalesDark
                                )
                            }
                        }
                    }
                }

                // Totals Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المجموع قبل الخصم:", fontSize = 12.sp, color = TextSecondary)
                            Text("${String.format(Locale.US, "%.2f", invoice.subtotal)} ${invoice.currency}", fontSize = 12.sp)
                        }
                        if (invoice.discount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الخصم:", fontSize = 12.sp, color = RoseReceivable)
                                Text("- ${String.format(Locale.US, "%.2f", invoice.discount)} ${invoice.currency}", color = RoseReceivable, fontSize = 12.sp)
                            }
                        }
                        Divider(color = Color(0xFFFDE68A))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الصافي النهائي:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmberSalesDark)
                            Text(
                                "${String.format(Locale.US, "%.2f", invoice.netTotal)} ${invoice.currency}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = AmberSalesDark
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إغلاق")
                    }

                    Button(
                        onClick = onSharePdf,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberSales)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة الفاتورة PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
