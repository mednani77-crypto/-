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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.CustomerBalance
import com.example.ui.components.SegmentedSwitcher
import com.example.ui.theme.*
import com.example.ui.viewmodel.SubView
import com.example.ui.viewmodel.SyscoViewModel
import java.util.Locale

@Composable
fun CustomersScreen(
    viewModel: SyscoViewModel,
    balances: List<CustomerBalance>,
    activeSubView: SubView,
    editingCustomer: Customer?
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredBalances = remember(balances, searchQuery) {
        if (searchQuery.isBlank()) balances
        else balances.filter {
            it.customer.name.contains(searchQuery, ignoreCase = true) ||
                    it.customer.area.contains(searchQuery, ignoreCase = true) ||
                    it.customer.phone.contains(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mobile-first Segmented Switcher
        SegmentedSwitcher(
            activeView = activeSubView,
            onViewSelected = { viewModel.setCustomerSubView(it) },
            listLabel = "📋 سجل العملاء والأرصدة (${balances.size})",
            formLabel = if (editingCustomer != null) "✏️ تعديل عميل" else "➕ إضافة عميل جديد",
            accentColor = NavyBrand
        )

        if (activeSubView == SubView.LIST) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث باسم العميل، المنطقة، أو الهاتف...", fontSize = 13.sp) },
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
                    focusedBorderColor = NavyBrand,
                    unfocusedBorderColor = BorderMedium
                )
            )

            if (filteredBalances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا يوجد عملاء مضافين حالياً" else "لا توجد نتائج مطابقة للبحث",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredBalances, key = { it.customer.id }) { balance ->
                        CustomerCard(
                            balance = balance,
                            onWhatsAppClick = { viewModel.sendWhatsAppBalanceMessage(context, balance) },
                            onStatementPdfClick = { viewModel.openCustomerStatement(balance) },
                            onEditClick = { viewModel.startEditCustomer(balance.customer) },
                            onDeleteClick = { viewModel.deleteCustomer(balance.customer) }
                        )
                    }
                }
            }
        } else {
            // Customer Form (Add / Edit)
            CustomerForm(
                editingCustomer = editingCustomer,
                onSave = { customer ->
                    viewModel.saveCustomer(customer)
                },
                onCancel = {
                    viewModel.setCustomerSubView(SubView.LIST)
                }
            )
        }
    }
}

@Composable
fun CustomerCard(
    balance: CustomerBalance,
    onWhatsAppClick: () -> Unit,
    onStatementPdfClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val c = balance.customer
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف العميل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف العميل '${c.name}'؟") },
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
                    Text("إلغاء")
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
            // Header Row: Name & Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = c.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = c.area,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Edit & Delete icons
                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = NavyBrand, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseReceivable, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Phone number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Text(text = c.phone, fontSize = 12.sp, color = TextSecondary)
            }

            Divider(color = BorderLight, thickness = 0.8.dp)

            // Balance Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // USD balance
                    Column {
                        Text("المديونية (USD)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "$ ${String.format(Locale.US, "%.2f", balance.netBalanceUsd)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (balance.netBalanceUsd > 0) RoseReceivable else EmeraldCollection
                        )
                    }

                    // DJF balance
                    Column(horizontalAlignment = Alignment.End) {
                        Text("المديونية (DJF)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", balance.netBalanceDjf)} DJF",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (balance.netBalanceDjf > 0) RoseReceivable else EmeraldCollection
                        )
                    }
                }
            }

            // Action Buttons: WhatsApp & PDF Statement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp Button
                Button(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text(
                        text = "💬 واتساب",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // PDF Statement Button
                Button(
                    onClick = onStatementPdfClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBrand)
                ) {
                    Text(
                        text = "📄 كشف PDF",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerForm(
    editingCustomer: Customer?,
    onSave: (Customer) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(editingCustomer?.phone ?: "+253 ") }
    var area by remember { mutableStateOf(editingCustomer?.area ?: "") }
    var openingBalanceUsd by remember { mutableStateOf(editingCustomer?.openingBalanceUsd?.toString() ?: "0.0") }
    var openingBalanceDjf by remember { mutableStateOf(editingCustomer?.openingBalanceDjf?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(editingCustomer?.notes ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    text = if (editingCustomer != null) "تعديل بيانات العميل" else "إضافة عميل جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NavyBrand
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

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل أو المتجر *") },
                    placeholder = { Text("مثال: سوبرماركت السلام") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف العميل (جيبوتي) *") },
                    placeholder = { Text("+253 77 xx xx xx") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("المنطقة / العنوان في جيبوتي") },
                    placeholder = { Text("مثال: بلبلا، شارع 26، المنطقة الحرة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = openingBalanceUsd,
                        onValueChange = { openingBalanceUsd = it },
                        label = { Text("رصيد افتتاحي (USD)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = openingBalanceDjf,
                        onValueChange = { openingBalanceDjf = it },
                        label = { Text("رصيد افتتاحي (DJF)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )
            }

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
                            if (name.isBlank()) {
                                errorMessage = "يرجى كتابة اسم العميل"
                                return@Button
                            }
                            val usd = openingBalanceUsd.toDoubleOrNull() ?: 0.0
                            val djf = openingBalanceDjf.toDoubleOrNull() ?: 0.0

                            val updated = (editingCustomer ?: Customer(
                                name = "",
                                phone = "",
                                area = ""
                            )).copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                area = area.trim(),
                                openingBalanceUsd = usd,
                                openingBalanceDjf = djf,
                                notes = notes.trim()
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBrand)
                    ) {
                        Text(
                            text = if (editingCustomer != null) "تحديث العميل" else "حفظ العميل",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
