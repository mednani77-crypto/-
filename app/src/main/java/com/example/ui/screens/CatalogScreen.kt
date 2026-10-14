package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.CompanySettings
import com.example.data.model.Product
import com.example.ui.components.ProductCatalog
import com.example.ui.components.SegmentedSwitcher
import com.example.ui.theme.*
import com.example.ui.viewmodel.SubView
import com.example.ui.viewmodel.SyscoViewModel
import java.util.Locale

@Composable
fun CatalogScreen(
    viewModel: SyscoViewModel,
    products: List<Product>,
    settings: CompanySettings,
    selectedIds: Set<Long>,
    activeSubView: SubView,
    editingProduct: Product?
) {
    val context = LocalContext.current
    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }

    val categories = remember(products) {
        listOf("الكل", "مخزون منخفض ⚠️") + products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategoryFilter) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true) ||
                    p.specs.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategoryFilter) {
                "الكل" -> true
                "مخزون منخفض ⚠️" -> p.stock <= p.lowStockThreshold
                else -> p.category == selectedCategoryFilter
            }
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Segmented Switcher for thumb-friendly mobile experience
        SegmentedSwitcher(
            activeView = activeSubView,
            onViewSelected = { viewModel.setCatalogSubView(it) },
            listLabel = "📋 كتالوج المنتجات (${products.size})",
            formLabel = if (editingProduct != null) "✏️ تعديل صنف" else "➕ إضافة صنف جديد",
            accentColor = IndigoCatalog
        )

        if (activeSubView == SubView.LIST) {
            // View Mode Selector: ProductCatalog (Grid with Images & Prices) vs PDF List Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isGridView,
                    onClick = { isGridView = true },
                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("🔲 شبكة الصور والأسعار", fontSize = 11.sp, fontWeight = if (isGridView) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isGridView,
                    onClick = { isGridView = false },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("📄 تصدير الكتالوج PDF", fontSize = 11.sp, fontWeight = if (!isGridView) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isGridView) {
                // The requested ProductCatalog Composable fetching Room data
                ProductCatalog(
                    modifier = Modifier.fillMaxSize(),
                    onAddToCart = { prod, qty -> viewModel.addToCart(prod, qty) },
                    onEditProduct = { viewModel.startEditProduct(it) },
                    onDeleteProduct = { viewModel.deleteProduct(it) },
                    onCheckoutCart = {
                        viewModel.setInvoiceSubView(SubView.FORM)
                        viewModel.setTab(com.example.ui.viewmodel.AppTab.INVOICES)
                    }
                )
            } else {
                // PDF Export Action Strip (Full vs Custom Selection)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.shareCatalogPdf(context, isCustomOnly = false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoCatalog)
                        ) {
                            Text("📄 كتالوج شامل PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.shareCatalogPdf(context, isCustomOnly = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedIds.isNotEmpty()) AmberSales else Color(0xFFCBD5E1)
                            ),
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Text(
                                text = if (selectedIds.isEmpty()) "عرض مخصص (0)" else "📄 عرض مخصص (${selectedIds.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedIds.isNotEmpty()) Color.White else TextMuted
                            )
                        }
                    }
                }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث عن صنف، تصنيف، أو مواصفات...", fontSize = 13.sp) },
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
                    focusedBorderColor = IndigoCatalog,
                    unfocusedBorderColor = BorderMedium
                )
            )

            // Category Filter Chips
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(4).forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (filteredProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أصناف مطابقة للبحث",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            isSelected = selectedIds.contains(product.id),
                            onToggleSelect = { viewModel.toggleProductSelection(product.id) },
                            onEditClick = { viewModel.startEditProduct(product) },
                            onDeleteClick = { viewModel.deleteProduct(product) }
                        )
                    }
                }
            }
        }
    } else {
            // Product Form
            ProductForm(
                editingProduct = editingProduct,
                exchangeRate = settings.exchangeRate,
                onSave = { prod ->
                    viewModel.saveProduct(prod)
                },
                onCancel = {
                    viewModel.setCatalogSubView(SubView.LIST)
                }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف الصنف من الكتالوج", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف '${product.name}'؟") },
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
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, IndigoCatalog, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF5F7FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Checkbox, Name, Category & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = IndigoCatalog)
                    )

                    Column {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = product.category,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = IndigoCatalog, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseReceivable, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (product.specs.isNotBlank()) {
                Text(
                    text = product.specs,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Divider(color = BorderLight, thickness = 0.8.dp)

            // Price & Stock Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dual Pricing
                Column {
                    Text(
                        text = "$ ${String.format(Locale.US, "%.2f", product.priceUsd)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = EmeraldCollection
                    )
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", product.priceDjf)} DJF",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AmberSales
                    )
                }

                // Stock Badge with Warning
                val isLowStock = product.stock <= product.lowStockThreshold
                Surface(
                    color = if (isLowStock) RoseReceivableLight else EmeraldCollectionLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isLowStock) {
                            Text(
                                text = "⚠️ مخزون منخفض: ${product.stock} ق",
                                color = RoseReceivableDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "المخزون المتوفر: ${product.stock} ق",
                                color = EmeraldCollectionDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductForm(
    editingProduct: Product?,
    exchangeRate: Double,
    onSave: (Product) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(editingProduct?.name ?: "") }
    var category by remember { mutableStateOf(editingProduct?.category ?: "مواد غذائية") }
    var specs by remember { mutableStateOf(editingProduct?.specs ?: "") }
    var priceUsdString by remember { mutableStateOf(editingProduct?.priceUsd?.toString() ?: "0.0") }
    var priceDjfString by remember { mutableStateOf(editingProduct?.priceDjf?.toString() ?: "0.0") }
    var stockString by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "50") }
    var lowThresholdString by remember { mutableStateOf(editingProduct?.lowStockThreshold?.toString() ?: "10") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dual currency auto-calculator
    fun onUsdPriceChanged(newUsd: String) {
        priceUsdString = newUsd
        val usd = newUsd.toDoubleOrNull()
        if (usd != null) {
            val djf = usd * exchangeRate
            priceDjfString = String.format(Locale.US, "%.0f", djf)
        }
    }

    fun onDjfPriceChanged(newDjf: String) {
        priceDjfString = newDjf
        val djf = newDjf.toDoubleOrNull()
        if (djf != null && exchangeRate > 0) {
            val usd = djf / exchangeRate
            priceUsdString = String.format(Locale.US, "%.2f", usd)
        }
    }

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
                    text = if (editingProduct != null) "تعديل الصنف والمخزون" else "إضافة صنف جديد للكتالوج",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = IndigoCatalog
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
                    label = { Text("اسم الصنف أو المنتج *") },
                    placeholder = { Text("مثال: أرز بسمتي فاخر 25 كجم") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف أو الفئة") },
                    placeholder = { Text("مثال: زيوت، حبوب، معلبات، مشروبات") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = specs,
                    onValueChange = { specs = it },
                    label = { Text("المواصفات وحجم العبوة") },
                    placeholder = { Text("مثال: كرتون 12 علبة، نخب أول ممتاز") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Dual Pricing with Auto-calculation
            item {
                Text(
                    text = "حاسبة الأسعار التلقائية (1 USD = $exchangeRate DJF):",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = priceUsdString,
                        onValueChange = { onUsdPriceChanged(it) },
                        label = { Text("السعر ($ USD)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = priceDjfString,
                        onValueChange = { onDjfPriceChanged(it) },
                        label = { Text("السعر (DJF)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Stock & Low Stock Threshold
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockString,
                        onValueChange = { stockString = it },
                        label = { Text("الكمية المتوفرة بالمستودع") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = lowThresholdString,
                        onValueChange = { lowThresholdString = it },
                        label = { Text("حد التنبيه للمخزون") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
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
                            if (name.isBlank()) {
                                errorMessage = "يرجى كتابة اسم المنتج"
                                return@Button
                            }
                            val usd = priceUsdString.toDoubleOrNull() ?: 0.0
                            val djf = priceDjfString.toDoubleOrNull() ?: 0.0
                            val stock = stockString.toIntOrNull() ?: 0
                            val threshold = lowThresholdString.toIntOrNull() ?: 10

                            val updated = (editingProduct ?: Product(
                                name = "",
                                category = ""
                            )).copy(
                                name = name.trim(),
                                category = category.trim(),
                                specs = specs.trim(),
                                priceUsd = usd,
                                priceDjf = djf,
                                stock = stock,
                                lowStockThreshold = threshold
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoCatalog)
                    ) {
                        Text("حفظ المنتج بالكتالوج", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
