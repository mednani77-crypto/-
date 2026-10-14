package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.local.CartDao
import com.example.data.local.ProductDao
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ProductCatalog displays an adaptive grid of products with images, dual prices (USD & DJF),
 * and warehouse stock status, fetching local data directly from Room database.
 */
@Composable
fun ProductCatalog(
    modifier: Modifier = Modifier,
    productDao: ProductDao? = null,
    cartDao: CartDao? = null,
    onAddToCart: ((Product, Int) -> Unit)? = null,
    onProductClick: ((Product) -> Unit)? = null,
    onEditProduct: ((Product) -> Unit)? = null,
    onDeleteProduct: ((Product) -> Unit)? = null,
    onCheckoutCart: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Fetch local data directly from Room
    val dao = remember(productDao) {
        productDao ?: AppDatabase.getDatabase(context).productDao()
    }
    val currentCartDao = remember(cartDao) {
        cartDao ?: AppDatabase.getDatabase(context).cartDao()
    }

    val products by dao.getAllProducts().collectAsStateWithLifecycle(initialValue = emptyList())
    val cartItems by currentCartDao.getAllCartItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val totalCartCount by currentCartDao.getTotalCartCount().collectAsStateWithLifecycle(initialValue = 0)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("الكل", "مخزون منخفض ⚠️") + products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true) ||
                    p.specs.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "الكل" -> true
                "مخزون منخفض ⚠️" -> p.stock <= p.lowStockThreshold
                else -> p.category == selectedCategory
            }
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBg),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Category Filter Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث بالاسم أو الفئة أو المواصفات...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = IndigoCatalog
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح البحث", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = IndigoCatalog,
                        unfocusedBorderColor = BorderLight
                    )
                )

                // Category Filter Horizontal Scroll
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoCatalogLight,
                                selectedLabelColor = IndigoCatalogDark,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Header Count & Grid Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "منتجات الكتالوج",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                if (searchQuery.isNotBlank() || selectedCategory != "الكل") {
                    Surface(
                        color = AmberSalesLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "مصفاة",
                            color = AmberSalesDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cart Action Button with Badge
                FilledTonalButton(
                    onClick = { showCartSheet = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (totalCartCount > 0) IndigoCatalogLight else Color(0xFFF1F5F9),
                        contentColor = if (totalCartCount > 0) IndigoCatalogDark else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("view_cart_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (totalCartCount > 0) {
                                Badge(
                                    containerColor = RoseReceivable,
                                    contentColor = Color.White
                                ) {
                                    Text("$totalCartCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "سلة التسوق",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (totalCartCount > 0) "السلة ($totalCartCount)" else "السلة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = IndigoCatalogLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${filteredProducts.size} صنف",
                        color = IndigoCatalogDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Product Grid or Empty State with Filter Reset
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = if (searchQuery.isBlank() && selectedCategory == "الكل")
                            "لا توجد منتجات مسجلة في قاعدة بيانات Room"
                        else
                            "لا توجد أصناف تطابق معايير البحث أو الفئة المحددة",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    if (searchQuery.isNotBlank() || selectedCategory != "الكل") {
                        Text(
                            text = "بحث: '${searchQuery.ifBlank { "الكل" }}' • الفئة: '$selectedCategory'",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "الكل"
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoCatalog)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إلغاء التصفية وإظهار كل المنتجات", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 155.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        onClick = {
                            selectedProductForDetail = product
                            onProductClick?.invoke(product)
                        },
                        onEdit = onEditProduct?.let { { it(product) } },
                        onDelete = onDeleteProduct?.let { { it(product) } }
                    )
                }
            }
        }
    }

    // Bottom Sheet displaying full product details, specs, and warehouse stock
    selectedProductForDetail?.let { prod ->
        ProductDetailBottomSheet(
            product = prod,
            cartDao = currentCartDao,
            onAddToCart = onAddToCart,
            onOpenCart = {
                selectedProductForDetail = null
                showCartSheet = true
            },
            onDismiss = { selectedProductForDetail = null },
            onEdit = onEditProduct?.let { { it(prod); selectedProductForDetail = null } },
            onDelete = onDeleteProduct?.let { { it(prod); selectedProductForDetail = null } }
        )
    }

    // Persistent Shopping Cart Bottom Sheet (Room Database)
    if (showCartSheet) {
        ShoppingCartBottomSheet(
            cartItems = cartItems,
            totalUsd = cartItems.sumOf { it.totalUsd },
            totalDjf = cartItems.sumOf { it.totalDjf },
            onDismiss = { showCartSheet = false },
            onUpdateQuantity = { prodId, qty ->
                coroutineScope.launch {
                    if (qty <= 0) currentCartDao.deleteCartItem(prodId)
                    else currentCartDao.updateQuantity(prodId, qty)
                }
            },
            onRemoveItem = { prodId ->
                coroutineScope.launch {
                    currentCartDao.deleteCartItem(prodId)
                }
            },
            onClearCart = {
                coroutineScope.launch {
                    currentCartDao.clearCart()
                }
            },
            onCheckout = onCheckoutCart
        )
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isLowStock = product.stock <= product.lowStockThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Product Visual Thumbnail / Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(getCategoryGradient(product.category)),
                contentAlignment = Alignment.Center
            ) {
                val decodedBitmap = remember(product.imageBase64) {
                    if (product.imageBase64.isNotBlank()) {
                        try {
                            val clean = if (product.imageBase64.contains(",")) product.imageBase64.substringAfter(",") else product.imageBase64
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                }

                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Stylized Product Illustration
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(product.category),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.category,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Category Tag floating badge
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Low Stock Badge overlay
                if (isLowStock) {
                    Surface(
                        color = RoseReceivable,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "مخزون منخفض: ${product.stock}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (product.specs.isNotBlank()) {
                    Text(
                        text = product.specs,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price Section (USD & DJF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$ ${String.format(Locale.US, "%.2f", product.priceUsd)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = EmeraldCollection
                        )
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", product.priceDjf)} DJF",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = AmberSales
                        )
                    }

                    // Stock Pill
                    Surface(
                        color = if (isLowStock) RoseReceivableLight else EmeraldCollectionLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${product.stock} ق",
                            color = if (isLowStock) RoseReceivableDark else EmeraldCollectionDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailBottomSheet(
    product: Product,
    cartDao: CartDao? = null,
    onAddToCart: ((Product, Int) -> Unit)? = null,
    onOpenCart: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isLowStock = product.stock <= product.lowStockThreshold
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentCartDao = remember(cartDao) {
        cartDao ?: AppDatabase.getDatabase(context).cartDao()
    }
    val existingCartItem by currentCartDao.getCartItemFlow(product.id).collectAsStateWithLifecycle(initialValue = null)

    var orderQuantity by remember { mutableIntStateOf(1) }
    var showSuccessFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(product.id) {
        orderQuantity = 1
        showSuccessFeedback = false
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف الصنف", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف '${product.name}' من الكتالوج؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke()
                        onDismiss()
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Image Container with decoded Bitmap or Category Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(getCategoryGradient(product.category)),
                contentAlignment = Alignment.Center
            ) {
                val decodedBitmap = remember(product.imageBase64) {
                    if (product.imageBase64.isNotBlank()) {
                        try {
                            val clean = if (product.imageBase64.contains(",")) product.imageBase64.substringAfter(",") else product.imageBase64
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                }

                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(product.category),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = product.category,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Category Floating Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Stock Warning Badge
                if (isLowStock) {
                    Surface(
                        color = RoseReceivable,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠️ مخزون منخفض: ${product.stock} ق",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Product Name and Category Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "التصنيف: ${product.category}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Stock Pill
                Surface(
                    color = if (isLowStock) RoseReceivableLight else EmeraldCollectionLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLowStock) "⚠️ مخزون حرج" else "✓ متوفر",
                        color = if (isLowStock) RoseReceivableDark else EmeraldCollectionDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Description and Specifications Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = NavyBrand,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "الوصف والمواصفات الكاملة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = if (product.specs.isNotBlank()) product.specs else "لا توجد مواصفات إضافية مسجلة لهذا الصنف",
                        fontSize = 13.sp,
                        color = if (product.specs.isNotBlank()) TextPrimary else TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            // Warehouse Stock Quantity Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLowStock) Color(0xFFFFF1F2) else Color(0xFFF0FDF4)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isLowStock) Color(0xFFFECDD3) else Color(0xFFBBF7D0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warehouse,
                                contentDescription = null,
                                tint = if (isLowStock) RoseReceivable else EmeraldCollection,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "رصيد المستودع الحالي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "${product.stock} قطعة",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (isLowStock) RoseReceivableDark else EmeraldCollectionDark
                        )
                    }

                    // Stock health explanation
                    Text(
                        text = if (isLowStock) "⚠️ تنبيه للمندوب: الكمية المتوفرة بالمستودع أقل من حد الأمان (${product.lowStockThreshold} قطع). يرجى التنسيق لإعادة التوريد."
                        else "✓ المخزون سليم ومتوفر للتسليم المباشر لمندوبي المبيعات والعملاء في جيبوتي.",
                        fontSize = 11.sp,
                        color = if (isLowStock) RoseReceivableDark else EmeraldCollectionDark,
                        lineHeight = 16.sp
                    )
                }
            }

            // Dual Pricing Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "السعر بالدولار الأمريكي",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "$ ${String.format(Locale.US, "%.2f", product.priceUsd)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = EmeraldCollection
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = Color(0xFFDBEAFE)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "السعر بالفرنك الجيبوتي",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${String.format(Locale.US, "%,.0f", product.priceDjf)} DJF",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = AmberSales
                        )
                    }
                }
            }

            // Add to Cart Section (Persistent Room Cart)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cart_action_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (product.stock > 0) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (product.stock > 0) EmeraldCollection.copy(alpha = 0.35f) else BorderLight
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (product.stock > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "طلب وإضافة إلى السلة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "الحد الأقصى المتاح بالمستودع: ${product.stock} قطعة",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            // Stepper: [-] [ quantity ] [+]
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (orderQuantity > 1) orderQuantity-- },
                                        enabled = orderQuantity > 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "إنقاص الكمية",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (orderQuantity > 1) TextPrimary else TextMuted
                                        )
                                    }

                                    Text(
                                        text = "$orderQuantity",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )

                                    IconButton(
                                        onClick = { if (orderQuantity < product.stock) orderQuantity++ },
                                        enabled = orderQuantity < product.stock,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "زيادة الكمية",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (orderQuantity < product.stock) TextPrimary else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Quick quantity selection chips if stock permits
                        if (product.stock > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 2, 5, 10).filter { it <= product.stock }.forEach { qtyChoice ->
                                    FilterChip(
                                        selected = orderQuantity == qtyChoice,
                                        onClick = { orderQuantity = qtyChoice },
                                        label = { Text("$qtyChoice قطع", fontSize = 11.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }

                        // Prominent Add to Cart Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (onAddToCart != null) {
                                        onAddToCart(product, orderQuantity)
                                    } else {
                                        val existing = currentCartDao.getCartItem(product.id)
                                        val currentQty = existing?.quantity ?: 0
                                        val targetQty = (currentQty + orderQuantity).coerceAtMost(product.stock)
                                        currentCartDao.insertOrUpdate(
                                            CartItem(
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
                                        )
                                    }
                                    showSuccessFeedback = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_to_cart_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCollection)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إضافة إلى السلة ( $ ${String.format(Locale.US, "%.2f", product.priceUsd * orderQuantity)} / ${String.format(Locale.US, "%,.0f", product.priceDjf * orderQuantity)} DJF )",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Feedback Banner / Existing Cart Status
                        if (showSuccessFeedback) {
                            Surface(
                                color = EmeraldCollectionLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldCollectionDark, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "تمت إضافة $orderQuantity قطع إلى السلة بنجاح!",
                                            color = EmeraldCollectionDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (onOpenCart != null) {
                                        TextButton(
                                            onClick = onOpenCart,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("عرض السلة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldCollectionDark)
                                        }
                                    }
                                }
                            }
                        } else if (existingCartItem != null) {
                            Surface(
                                color = IndigoCatalogLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🛒 يوجد حالياً ${existingCartItem?.quantity} قطعة في سلتك",
                                        color = IndigoCatalogDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (onOpenCart != null) {
                                        TextButton(
                                            onClick = onOpenCart,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("عرض السلة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoCatalogDark)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Out of stock warning
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RoseReceivable)
                            Column {
                                Text(
                                    text = "هذا الصنف نفد من المستودع حالياً",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = RoseReceivableDark
                                )
                                Text(
                                    text = "لا يمكن إضافة كميات إلى السلة حتى يتم توريد مخزون جديد للمستودع.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("غير متوفر في المخزون", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseReceivable)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف", fontSize = 12.sp)
                    }
                }

                if (onEdit != null) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoCatalog)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل الصنف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBrand)
                ) {
                    Text("إغلاق", fontSize = 12.sp)
                }
            }
        }
    }
}

// Category visual helpers
private fun getCategoryGradient(category: String): Brush {
    return when {
        category.contains("أرز") || category.contains("حبوب") -> Brush.verticalGradient(
            listOf(Color(0xFFB45309), Color(0xFFD97706))
        )
        category.contains("زيت") || category.contains("دهون") -> Brush.verticalGradient(
            listOf(Color(0xFF047857), Color(0xFF10B981))
        )
        category.contains("سكر") || category.contains("تموين") -> Brush.verticalGradient(
            listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
        )
        category.contains("حليب") || category.contains("ألبان") -> Brush.verticalGradient(
            listOf(Color(0xFF4338CA), Color(0xFF6366F1))
        )
        category.contains("شاي") || category.contains("مشروب") -> Brush.verticalGradient(
            listOf(Color(0xFF78350F), Color(0xFF92400E))
        )
        category.contains("طماطم") || category.contains("معلب") -> Brush.verticalGradient(
            listOf(Color(0xFF991B1B), Color(0xFFDC2626))
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF334155))
        )
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("أرز") || category.contains("حبوب") -> Icons.Default.Grass
        category.contains("زيت") || category.contains("دهون") -> Icons.Default.WaterDrop
        category.contains("سكر") || category.contains("تموين") -> Icons.Default.ShoppingBag
        category.contains("حليب") || category.contains("ألبان") -> Icons.Default.LocalDrink
        category.contains("شاي") || category.contains("مشروب") -> Icons.Default.LocalCafe
        category.contains("طماطم") || category.contains("معلب") -> Icons.Default.SoupKitchen
        else -> Icons.Default.Inventory2
    }
}
