package com.example.appbanmypham.ui.product

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.appbanmypham.data.local.AppDatabase
import com.example.appbanmypham.ui.cart.CartActivity
import com.example.appbanmypham.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppBanMyPhamTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundPrimary) {
                    ProductScreen(
                        onGoCart = { startActivity(Intent(this, CartActivity::class.java)) },
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            finish()
                        }
                    )
                }
            }
        }
    }
}

// ── Data class ────────────────────────────────────────────────────────────────
data class ProductDisplay(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val stock: Int = 0,
    val description: String = "",
    val brandName: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val isNew: Boolean = false
)

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    onGoCart: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    var products    by remember { mutableStateOf(listOf<ProductDisplay>()) }
    var categories  by remember { mutableStateOf(listOf<String>()) }
    var isLoading   by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Tất cả") }
    var cartCount   by remember { mutableStateOf(0) }
    var isGridView  by remember { mutableStateOf(true) }
    var showSearch  by remember { mutableStateOf(false) }

    // Load sản phẩm realtime từ Firestore + sync Room
    LaunchedEffect(Unit) {
        db.collection("products")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val now = System.currentTimeMillis()
                val list = snap?.documents?.map { doc ->
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    ProductDisplay(
                        id            = doc.id,
                        name          = doc.getString("name") ?: "",
                        price         = doc.getDouble("price") ?: 0.0,
                        originalPrice = doc.getDouble("originalPrice") ?: 0.0,
                        stock         = (doc.getLong("stock") ?: 0L).toInt(),
                        description   = doc.getString("description") ?: "",
                        brandName     = doc.getString("brandName") ?: "",
                        imageUrl      = doc.getString("imageUrl") ?: "",
                        category      = doc.getString("category") ?: "",
                        isNew         = (now - createdAt) < 7 * 24 * 60 * 60 * 1000L
                    )
                } ?: emptyList()
                products = list
                categories = listOf("Tất cả") + list.map { it.category }.filter { it.isNotEmpty() }.distinct()
                isLoading = false
            }
    }

    // Lấy số lượng giỏ hàng
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("carts").document(uid).collection("items")
            .addSnapshotListener { snap, _ -> cartCount = snap?.size() ?: 0 }
    }

    val filtered = products.filter { p ->
        val matchCat = selectedCat == "Tất cả" || p.category == selectedCat
        val matchSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.brandName.contains(searchQuery, ignoreCase = true)
        matchCat && matchSearch && p.stock > 0
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            Column {
                // ── Top Bar ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = AppGradients.mintHorizontal)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Logo
                    if (!showSearch) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) { Text("🌿", fontSize = 16.sp) }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("LUMIÈRE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text("Beauty Store", color = Color.White.copy(0.75f), fontSize = 9.sp, fontStyle = FontStyle.Italic)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(end = 50.dp),
                            placeholder = { Text("Tìm kiếm...", color = Color.White.copy(0.7f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Right icons
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {
                            showSearch = !showSearch
                            if (!showSearch) searchQuery = ""
                        }) {
                            Icon(
                                if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = null, tint = Color.White
                            )
                        }
                        // Cart with badge
                        Box {
                            IconButton(onClick = onGoCart) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                            }
                            if (cartCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF6B6B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (cartCount > 9) "9+" else cartCount.toString(),
                                        color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                // ── Category chips ──
                if (categories.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(vertical = 10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = if (isSelected) AppGradients.mintHorizontal
                                        else androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(Color(0xFFEAF9F5), Color(0xFFEAF9F5))
                                        )
                                    )
                                    .clickable { selectedCat = cat }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cat,
                                    color = if (isSelected) Color.White else MintGreen,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── View toggle + count ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filtered.size} sản phẩm",
                    color = Color(0xFF8ACABA),
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEAF9F5))
                ) {
                    IconButton(
                        onClick = { isGridView = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isGridView) MintGreen else Color.Transparent)
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = null,
                            tint = if (isGridView) Color.White else MintGreen,
                            modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { isGridView = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isGridView) MintGreen else Color.Transparent)
                    ) {
                        Icon(Icons.Default.ViewList, contentDescription = null,
                            tint = if (!isGridView) Color.White else MintGreen,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MintGreen)
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Không tìm thấy sản phẩm", color = Color(0xFF8ACABA), fontSize = 15.sp)
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { product ->
                        ProductGridCard(
                            product = product,
                            onAddToCart = { addToCart(db, auth, it) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { product ->
                        ProductListCard(
                            product = product,
                            onAddToCart = { addToCart(db, auth, it) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Grid Card ─────────────────────────────────────────────────────────────────
@Composable
private fun ProductGridCard(
    product: ProductDisplay,
    onAddToCart: (ProductDisplay) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFFEAF9F5)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🌿", fontSize = 40.sp)
                }

                // NEW badge
                if (product.isNew) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush = AppGradients.mintHorizontal)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("MỚI", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                // Out of stock overlay
                if (product.stock <= 5 && product.stock > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Còn ${product.stock}", color = Color(0xFFE8A44A), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Info
            Column(modifier = Modifier.padding(10.dp)) {
                Text(product.brandName, color = MintGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(
                    product.name,
                    color = Color(0xFF1A4A40),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${"%,.0f".format(product.price)}đ",
                            color = Color(0xFF1A4A40),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (product.originalPrice > product.price) {
                            Text(
                                "${"%,.0f".format(product.originalPrice)}đ",
                                color = Color(0xFFAAD8CE),
                                fontSize = 10.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                    IconButton(
                        onClick = { onAddToCart(product) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush = AppGradients.mintHorizontal)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm vào giỏ", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── List Card ─────────────────────────────────────────────────────────────────
@Composable
private fun ProductListCard(
    product: ProductDisplay,
    onAddToCart: (ProductDisplay) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEAF9F5)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Text("🌿", fontSize = 32.sp)
                }
                if (product.isNew) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(bottomEnd = 6.dp))
                            .background(brush = AppGradients.mintHorizontal)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("MỚI", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(product.brandName, color = MintGreen, fontSize = 10.sp)
                Text(
                    product.name,
                    color = Color(0xFF1A4A40),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.category.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(product.category, color = Color(0xFFAAD8CE), fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${"%,.0f".format(product.price)}đ",
                        color = Color(0xFF1A4A40),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (product.originalPrice > product.price) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${"%,.0f".format(product.originalPrice)}đ",
                            color = Color(0xFFAAD8CE),
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush = AppGradients.mintHorizontal)
                    .clickable { onAddToCart(product) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Add to cart ───────────────────────────────────────────────────────────────
private fun addToCart(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    product: ProductDisplay
) {
    val uid = auth.currentUser?.uid ?: return
    val cartRef = db.collection("carts").document(uid)
        .collection("items").document(product.id)

    db.runTransaction { transaction ->
        val snap = transaction.get(cartRef)
        val qty  = (snap.getLong("quantity") ?: 0L).toInt()
        transaction.set(
            cartRef,
            hashMapOf(
                "productId"   to product.id,
                "name"        to product.name,
                "price"       to product.price,
                "imageUrl"    to product.imageUrl,
                "brandName"   to product.brandName,
                "quantity"    to qty + 1,
                "updatedAt"   to System.currentTimeMillis()
            )
        )
    }
}