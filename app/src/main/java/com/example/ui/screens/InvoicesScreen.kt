package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.*
import com.example.ui.components.SegmentedSwitcher
import com.example.ui.theme.*
import com.example.ui.viewmodel.SubView
import com.example.ui.viewmodel.SyscoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicesScreen(
    viewModel: SyscoViewModel,
    invoices: List<Invoice>,
    customers: List<Customer>,
    products: List<Product>,
    activeSubView: SubView
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredInvoices = remember(invoices, searchQuery) {
        if (searchQuery.isBlank()) invoices
        else invoices.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
                    it.invoiceNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Segmented Switcher for thumb-friendly mobile ergonomics
        SegmentedSwitcher(
            activeView = activeSubView,
            onViewSelected = { viewModel.setInvoiceSubView(it) },
            listLabel = "📋 سجل الفواتير (${invoices.size})",
            formLabel = "➕ إصدار فاتورة جديدة",
            accentColor = AmberSales
        )

        if (activeSubView == SubView.LIST) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث برقم الفاتورة أو اسم العميل...", fontSize = 13.sp) },
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
                    focusedBorderColor = AmberSales,
                    unfocusedBorderColor = BorderMedium
                )
            )

            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا توجد فواتير مبيعات مسجلة" else "لا توجد فواتير مطابقة للبحث",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            onPreviewClick = { viewModel.openInvoicePreview(invoice) },
                            onSharePdfClick = {
                                val items = JsonHelper.deserializeInvoiceItems(invoice.itemsJson)
                                viewModel.shareInvoicePdf(context, invoice, items)
                            },
                            onDeleteClick = { viewModel.deleteInvoice(invoice) }
                        )
                    }
                }
            }
        } else {
            // Invoice Builder Form
            InvoiceBuilderForm(
                customers = customers,
                products = products,
                onCreateInvoice = { invoice, items ->
                    viewModel.createInvoice(invoice, items)
                },
                onCancel = {
                    viewModel.setInvoiceSubView(SubView.LIST)
                }
            )
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onPreviewClick: () -> Unit,
    onSharePdfClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val items = remember(invoice.itemsJson) { JsonHelper.deserializeInvoiceItems(invoice.itemsJson) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("إلغاء الفاتورة", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من إلغاء الفاتورة ${invoice.invoiceNumber}؟ سيتم إعادة الكميات للمستودع.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseReceivable)
                ) {
                    Text("نعم، إلغاء وحذف")
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
            // Header Row: Invoice Number, Date & Delete
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
                            .background(AmberSalesLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = invoice.invoiceNumber,
                            color = AmberSalesDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(invoice.date)),
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

            // Customer Name & Currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                Surface(
                    color = if (invoice.currency == "USD") Color(0xFFEFF6FF) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = invoice.currency,
                        color = if (invoice.currency == "USD") NavyBrand else AmberSalesDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Items breakdown summary
            Text(
                text = "الأصناف المباعة (${items.size}): " + items.joinToString("، ") { "${it.productName} (${it.quantity})" },
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2
            )

            Divider(color = BorderLight, thickness = 0.8.dp)

            // Totals Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (invoice.discount > 0) {
                        Text(
                            text = "خصم: -${String.format(Locale.US, "%.2f", invoice.discount)} ${invoice.currency}",
                            fontSize = 11.sp,
                            color = RoseReceivable
                        )
                    }
                    Text(
                        text = "الصافي النهائي",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.2f", invoice.netTotal)} ${invoice.currency}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = AmberSales
                )
            }

            // Action Buttons: Preview & PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("معاينة الفاتورة 👁️", fontSize = 12.sp)
                }

                Button(
                    onClick = onSharePdfClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSales)
                ) {
                    Text("مشاركة PDF 📄", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Temporary data class for building line items in the form
data class DraftItem(
    val selectedProduct: Product? = null,
    val quantityString: String = "1",
    val unitPriceString: String = "0.0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBuilderForm(
    customers: List<Customer>,
    products: List<Product>,
    onCreateInvoice: (Invoice, List<InvoiceItem>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var selectedCurrency by remember { mutableStateOf("USD") } // "USD" or "DJF"
    var discountString by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dropdown customer expanded state
    var customerMenuExpanded by remember { mutableStateOf(false) }

    // Items in the invoice
    val draftItems = remember {
        mutableStateListOf(
            DraftItem(
                selectedProduct = products.firstOrNull(),
                quantityString = "1",
                unitPriceString = products.firstOrNull()?.let {
                    if (selectedCurrency == "USD") it.priceUsd.toString() else it.priceDjf.toString()
                } ?: "0.0"
            )
        )
    }

    // Auto-update item unit prices when currency changes
    fun updatePricesForCurrency(newCurrency: String) {
        selectedCurrency = newCurrency
        for (i in draftItems.indices) {
            val prod = draftItems[i].selectedProduct
            if (prod != null) {
                val price = if (newCurrency == "USD") prod.priceUsd else prod.priceDjf
                draftItems[i] = draftItems[i].copy(unitPriceString = price.toString())
            }
        }
    }

    // Calculate subtotal & net
    val subtotal = draftItems.sumOf { item ->
        val q = item.quantityString.toIntOrNull() ?: 0
        val p = item.unitPriceString.toDoubleOrNull() ?: 0.0
        q * p
    }
    val discount = discountString.toDoubleOrNull() ?: 0.0
    val netTotal = (subtotal - discount).coerceAtLeast(0.0)

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
                    text = "إصدار فاتورة مبيعات جديدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AmberSales
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

            // Customer selector
            item {
                Text("اختيار العميل *", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = customerMenuExpanded,
                    onExpandedChange = { customerMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = customerMenuExpanded,
                        onDismissRequest = { customerMenuExpanded = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.area})") },
                                onClick = {
                                    selectedCustomer = cust
                                    customerMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Currency Selector (USD vs DJF)
            item {
                Text("عملة الفاتورة *", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCurrency == "USD",
                        onClick = { updatePricesForCurrency("USD") },
                        label = { Text("الدولار الأمريكي ($ USD)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCurrency == "DJF",
                        onClick = { updatePricesForCurrency("DJF") },
                        label = { Text("الفرنك الجيبوتي (DJF)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Line items header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أصناف الفاتورة (${draftItems.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    TextButton(
                        onClick = {
                            val defaultProd = products.firstOrNull()
                            val price = if (selectedCurrency == "USD") defaultProd?.priceUsd ?: 0.0 else defaultProd?.priceDjf ?: 0.0
                            draftItems.add(
                                DraftItem(
                                    selectedProduct = defaultProd,
                                    quantityString = "1",
                                    unitPriceString = price.toString()
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ إضافة صنف آخر", color = AmberSales, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Line Items
            items(draftItems.size) { index ->
                val draft = draftItems[index]
                var productDropdownExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صنف رقم ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberSalesDark)

                            if (draftItems.size > 1) {
                                IconButton(
                                    onClick = { draftItems.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = RoseReceivable)
                                }
                            }
                        }

                        // Product selector
                        ExposedDropdownMenuBox(
                            expanded = productDropdownExpanded,
                            onExpandedChange = { productDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = draft.selectedProduct?.name ?: "اختر منتج من الكتالوج...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = productDropdownExpanded,
                                onDismissRequest = { productDropdownExpanded = false }
                            ) {
                                products.forEach { prod ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(prod.name)
                                                Text(
                                                    "المخزون: ${prod.stock}",
                                                    color = if (prod.stock <= prod.lowStockThreshold) RoseReceivable else TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            val unitPrice = if (selectedCurrency == "USD") prod.priceUsd else prod.priceDjf
                                            draftItems[index] = draft.copy(
                                                selectedProduct = prod,
                                                unitPriceString = unitPrice.toString()
                                            )
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Stock warning if low or negative
                        draft.selectedProduct?.let { p ->
                            val q = draft.quantityString.toIntOrNull() ?: 0
                            if (q > p.stock) {
                                Text(
                                    text = "⚠️ الكمية المطلوبة ($q) تتجاوز الرصيد الحالي بالمستودع (${p.stock} فقط)",
                                    color = RoseReceivable,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Quantity & Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.quantityString,
                                onValueChange = {
                                    draftItems[index] = draft.copy(quantityString = it)
                                },
                                label = { Text("الكمية") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = draft.unitPriceString,
                                onValueChange = {
                                    draftItems[index] = draft.copy(unitPriceString = it)
                                },
                                label = { Text("سعر الوحدة ($selectedCurrency)") },
                                modifier = Modifier.weight(1.5f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        val itemSub = (draft.quantityString.toIntOrNull() ?: 0) * (draft.unitPriceString.toDoubleOrNull() ?: 0.0)
                        Text(
                            text = "إجمالي الصنف: ${String.format(Locale.US, "%.2f", itemSub)} $selectedCurrency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AmberSalesDark,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // Discount Field
            item {
                OutlinedTextField(
                    value = discountString,
                    onValueChange = { discountString = it },
                    label = { Text("خصم إضافي على الفاتورة ($selectedCurrency)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الفاتورة") },
                    placeholder = { Text("مثال: تم التوصيل لمستودع العميل") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Summary Totals Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المجموع قبل الخصم:", fontSize = 13.sp, color = TextSecondary)
                            Text("${String.format(Locale.US, "%.2f", subtotal)} $selectedCurrency", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (discount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الخصم:", fontSize = 13.sp, color = RoseReceivable)
                                Text("- ${String.format(Locale.US, "%.2f", discount)} $selectedCurrency", color = RoseReceivable, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Divider(color = Color(0xFFFDE68A), thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الصافي النهائي للفاتورة:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberSalesDark)
                            Text(
                                "${String.format(Locale.US, "%.2f", netTotal)} $selectedCurrency",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = AmberSalesDark
                            )
                        }
                    }
                }
            }

            // Submit Buttons
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
                                errorMessage = "يرجى اختيار العميل أولاً"
                                return@Button
                            }

                            val validItems = mutableListOf<InvoiceItem>()
                            for (draft in draftItems) {
                                val prod = draft.selectedProduct
                                val q = draft.quantityString.toIntOrNull() ?: 0
                                val p = draft.unitPriceString.toDoubleOrNull() ?: 0.0
                                if (prod != null && q > 0) {
                                    validItems.add(
                                        InvoiceItem(
                                            productId = prod.id,
                                            productName = prod.name,
                                            quantity = q,
                                            unitPrice = p,
                                            total = q * p
                                        )
                                    )
                                }
                            }

                            if (validItems.isEmpty()) {
                                errorMessage = "يرجى إضافة صنف واحد على الأقل مع تحديد كمية صالحة"
                                return@Button
                            }

                            val invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
                            val newInvoice = Invoice(
                                invoiceNumber = invoiceNumber,
                                customerId = cust.id,
                                customerName = cust.name,
                                date = System.currentTimeMillis(),
                                currency = selectedCurrency,
                                itemsJson = JsonHelper.serializeInvoiceItems(validItems),
                                discount = discount,
                                subtotal = subtotal,
                                netTotal = netTotal,
                                notes = notes.trim()
                            )
                            onCreateInvoice(newInvoice, validItems)
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberSales)
                    ) {
                        Text("إصدار الفاتورة وخصم المخزون", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
