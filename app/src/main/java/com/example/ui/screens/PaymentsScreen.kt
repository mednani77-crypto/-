package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.ui.components.SegmentedSwitcher
import com.example.ui.theme.*
import com.example.ui.viewmodel.SubView
import com.example.ui.viewmodel.SyscoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentsScreen(
    viewModel: SyscoViewModel,
    payments: List<Payment>,
    customers: List<Customer>,
    activeSubView: SubView
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredPayments = remember(payments, searchQuery) {
        if (searchQuery.isBlank()) payments
        else payments.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
                    it.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                    it.paymentMethod.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Segmented Switcher in Emerald Collection Accent
        SegmentedSwitcher(
            activeView = activeSubView,
            onViewSelected = { viewModel.setPaymentSubView(it) },
            listLabel = "📋 سجل سندات القبض (${payments.size})",
            formLabel = "➕ تسجيل سند قبض جديد",
            accentColor = EmeraldCollection
        )

        if (activeSubView == SubView.LIST) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث برقم السند، العميل، أو وسيلة الدفع...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = EmeraldCollection,
                    unfocusedBorderColor = BorderMedium
                )
            )

            if (filteredPayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا توجد سندات قبض مسجلة" else "لا توجد نتائج مطابقة للبحث",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        PaymentCard(
                            payment = payment,
                            onDeleteClick = { viewModel.deletePayment(payment) }
                        )
                    }
                }
            }
        } else {
            // Add Payment Form
            PaymentForm(
                customers = customers,
                onSave = { payment ->
                    viewModel.recordPayment(payment)
                },
                onCancel = {
                    viewModel.setPaymentSubView(SubView.LIST)
                }
            )
        }
    }
}

@Composable
fun PaymentCard(
    payment: Payment,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف سند القبض", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف سند القبض ${payment.receiptNumber}؟ سيتم إعادة المبلغ لذمة العميل.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseReceivable)
                ) {
                    Text("نعم، حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("رجوع")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Receipt Number & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldCollectionLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = payment.receiptNumber,
                            color = EmeraldCollectionDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(payment.date)),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = RoseReceivable,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Customer Name & Payment Method Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = payment.customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = payment.paymentMethod,
                        color = NavyBrand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (payment.notes.isNotBlank()) {
                Text(
                    text = "ملاحظات: ${payment.notes}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Divider(color = BorderLight, thickness = 0.8.dp)

            // Amount Settled
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المبلغ المقبوض والمخصوم:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Text(
                    text = "${String.format(Locale.US, "%.2f", payment.amount)} ${payment.currency}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = EmeraldCollection
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentForm(
    customers: List<Customer>,
    onSave: (Payment) -> Unit,
    onCancel: () -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var selectedCurrency by remember { mutableStateOf("USD") } // "USD" or "DJF"
    var amountString by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("نقدي (Cash)") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val paymentMethods = listOf(
        "نقدي (Cash)",
        "تحويل بنكي (Bank Transfer)",
        "شيك (Cheque)",
        "D-Money / Waafi"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "تسجيل سند قبض وسداد مديونية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = EmeraldCollection
                )
            }

            if (errorMessage != null) {
                item {
                    Surface(
                        color = RoseReceivableLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = RoseReceivableDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Customer Selector
            item {
                Text("اسم العميل المسدد *", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.area})") },
                                onClick = {
                                    selectedCustomer = cust
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Currency Selector
            item {
                Text("عملة السداد *", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCurrency == "USD",
                        onClick = { selectedCurrency = "USD" },
                        label = { Text("الدولار الأمريكي ($ USD)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCurrency == "DJF",
                        onClick = { selectedCurrency = "DJF" },
                        label = { Text("الفرنك الجيبوتي (DJF)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Amount
            item {
                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("المبلغ المقبوض ($selectedCurrency) *") },
                    placeholder = { Text("أدخل المبلغ المسدد") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Payment Method
            item {
                Text("طريقة الاستلام والدفع *", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    paymentMethods.forEach { method ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (selectedPaymentMethod == method) EmeraldCollectionLight else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { selectedPaymentMethod = method }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == method,
                                    onClick = { selectedPaymentMethod = method },
                                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldCollection)
                                )
                                Text(
                                    text = method,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedPaymentMethod == method) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPaymentMethod == method) EmeraldCollectionDark else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية (رقم التحويل أو الشيك)") },
                    placeholder = { Text("مثال: تحويل محفظة Waafi رقم 98124") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val cust = selectedCustomer
                            if (cust == null) {
                                errorMessage = "يرجى اختيار العميل المسدد"
                                return@Button
                            }
                            val amount = amountString.toDoubleOrNull() ?: 0.0
                            if (amount <= 0.0) {
                                errorMessage = "يرجى إدخال مبلغ سداد صحيح أكبر من الصفر"
                                return@Button
                            }

                            val receiptNumber = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
                            val newPayment = Payment(
                                receiptNumber = receiptNumber,
                                customerId = cust.id,
                                customerName = cust.name,
                                date = System.currentTimeMillis(),
                                currency = selectedCurrency,
                                amount = amount,
                                paymentMethod = selectedPaymentMethod,
                                notes = notes.trim()
                            )
                            onSave(newPayment)
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldCollection)
                    ) {
                        Text("تسجيل السند وخصم المديونية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
