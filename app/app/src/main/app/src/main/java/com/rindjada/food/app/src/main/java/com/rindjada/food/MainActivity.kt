package com.rindjada.food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Orange = Color(0xFFE65100)
private val Cream = Color(0xFFFFF8F1)
private val Green = Color(0xFF2E7D32)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Orange, background = Cream, surface = Color.White)
            ) {
                RindJadaApp()
            }
        }
    }
}

// ---------------- App state ----------------

sealed interface Screen {
    data object Home : Screen
    data class Menu(val restaurant: Restaurant) : Screen
    data object Cart : Screen
    data object Checkout : Screen
    data object Confirmation : Screen
    data object Status : Screen
}

class AppState {
    var screen: Screen by mutableStateOf(Screen.Home)
    var cartRestaurant: Restaurant? by mutableStateOf(null)
    val cart = mutableStateListOf<CartLine>()
    var lastOrder: Order? by mutableStateOf(null)

    val cartCount: Int get() = cart.sumOf { it.quantity }
    val subtotal: Int get() = cart.sumOf { it.item.price * it.quantity }

    fun qty(item: FoodItem): Int = cart.firstOrNull { it.item.id == item.id }?.quantity ?: 0

    fun add(restaurant: Restaurant, item: FoodItem) {
        // One order = one restaurant. Adding from another restaurant starts a new cart.
        if (cartRestaurant?.id != restaurant.id) {
            cart.clear()
            cartRestaurant = restaurant
        }
        val i = cart.indexOfFirst { it.item.id == item.id }
        if (i >= 0) cart[i] = cart[i].copy(quantity = cart[i].quantity + 1)
        else cart.add(CartLine(item, 1))
    }

    fun remove(item: FoodItem) {
        val i = cart.indexOfFirst { it.item.id == item.id }
        if (i < 0) return
        if (cart[i].quantity <= 1) cart.removeAt(i)
        else cart[i] = cart[i].copy(quantity = cart[i].quantity - 1)
        if (cart.isEmpty()) cartRestaurant = null
    }

    fun goBack() {
        screen = when (screen) {
            is Screen.Menu -> Screen.Home
            Screen.Cart -> cartRestaurant?.let { Screen.Menu(it) } ?: Screen.Home
            Screen.Checkout -> Screen.Cart
            else -> Screen.Home
        }
    }
}

// ---------------- Main layout ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RindJadaApp() {
    val state = remember { AppState() }
    val screen = state.screen

    BackHandler(enabled = screen !is Screen.Home) { state.goBack() }

    val title = when (screen) {
        Screen.Home -> AppConfig.APP_NAME
        is Screen.Menu -> screen.restaurant.name
        Screen.Cart -> "Your Cart"
        Screen.Checkout -> "Delivery Details"
        Screen.Confirmation -> "Order Confirmed"
        Screen.Status -> "Order Status"
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (screen !is Screen.Home) {
                        TextButton(onClick = { state.goBack() }) { Text("鈫� Back", color = Color.White) }
                    }
                },
                actions = {
                    if (state.cartCount > 0 && (screen is Screen.Home || screen is Screen.Menu)) {
                        TextButton(onClick = { state.screen = Screen.Cart }) {
                            Text("馃洅 ${state.cartCount}", color = Color.White, fontSize = 18.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Orange,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(state)
                is Screen.Menu -> MenuScreen(state, screen.restaurant)
                Screen.Cart -> CartScreen(state)
                Screen.Checkout -> CheckoutScreen(state)
                Screen.Confirmation -> ConfirmationScreen(state)
                Screen.Status -> StatusScreen(state)
            }
        }
    }
}

// ---------------- Small reusable pieces ----------------

@Composable
fun PhotoOrEmoji(imageUrl: String, emoji: String, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFE0B2)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(emoji, fontSize = (size / 2).sp)
        }
    }
}

@Composable
fun BigButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

fun money(amount: Int) = "${AppConfig.CURRENCY} $amount"

// ---------------- Home + restaurant list ----------------

@Composable
fun HomeScreen(state: AppState) {
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        restaurants = AppConfig.repository.getRestaurants()
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Orange).padding(20.dp)
            ) {
                Text("Hungry? 馃構", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Fast food delivery in ${AppConfig.AREA}", color = Color.White, fontSize = 15.sp)
            }
        }
        item { Text("Restaurants & Hotels", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (loading) item { CircularProgressIndicator() }
        items(restaurants) { r ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = r.isOpen) { state.screen = Screen.Menu(r) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PhotoOrEmoji(r.imageUrl, r.emoji, 72)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(r.description, fontSize = 14.sp, color = Color.Gray)
                        Text(
                            if (r.isOpen) "Delivery: ${money(r.deliveryCharge)}" else "Closed now",
                            fontSize = 13.sp,
                            color = if (r.isOpen) Green else Color.Red
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Food items ----------------

@Composable
fun MenuScreen(state: AppState, restaurant: Restaurant) {
    var items by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(restaurant.id) {
        items = AppConfig.repository.getFoodItems(restaurant.id)
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Delivery charge: ${money(restaurant.deliveryCharge)}",
                    color = Color.Gray, fontSize = 14.sp
                )
            }
            if (loading) item { CircularProgressIndicator() }
            items(items) { food ->
                val qty = state.qty(food)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PhotoOrEmoji(food.imageUrl, food.emoji, 64)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(food.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(money(food.price), fontSize = 15.sp, color = Orange, fontWeight = FontWeight.Bold)
                        }
                        if (qty == 0) {
                            Button(onClick = { state.add(restaurant, food) }) { Text("Add") }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { state.remove(food) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(38.dp)
                                ) { Text("鈭�", fontSize = 20.sp) }
                                Text("  $qty  ", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { state.add(restaurant, food) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(38.dp)
                                ) { Text("+", fontSize = 20.sp) }
                            }
                        }
                    }
                }
            }
        }
        if (state.cartCount > 0) {
            Box(Modifier.padding(16.dp)) {
                BigButton("View Cart (${state.cartCount}) 鈥� ${money(state.subtotal)}") {
                    state.screen = Screen.Cart
                }
            }
        }
    }
}

// ---------------- Cart ----------------

@Composable
fun CartScreen(state: AppState) {
    val restaurant = state.cartRestaurant
    if (state.cart.isEmpty() || restaurant == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("馃洅", fontSize = 60.sp)
            Text("Your cart is empty", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            BigButton("Browse restaurants") { state.screen = Screen.Home }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("From: ${restaurant.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            items(state.cart.toList()) { line ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.item.name, fontWeight = FontWeight.Bold)
                            Text(money(line.item.price * line.quantity), color = Orange)
                        }
                        OutlinedButton(
                            onClick = { state.remove(line.item) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(38.dp)
                        ) { Text("鈭�", fontSize = 20.sp) }
                        Text("  ${line.quantity}  ", fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { state.add(restaurant, line.item) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(38.dp)
                        ) { Text("+", fontSize = 20.sp) }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SummaryRow("Items total", money(state.subtotal))
                SummaryRow("Delivery charge", money(restaurant.deliveryCharge))
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                SummaryRow("Total", money(state.subtotal + restaurant.deliveryCharge), bold = true)
            }
        }
        Box(Modifier.padding(16.dp)) {
            BigButton("Continue") { state.screen = Screen.Checkout }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = weight, fontSize = if (bold) 18.sp else 15.sp)
        Text(value, fontWeight = weight, fontSize = if (bold) 18.sp else 15.sp)
    }
}

// ---------------- Customer details ----------------

@Composable
fun CheckoutScreen(state: AppState) {
    val restaurant = state.cartRestaurant
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var placing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (restaurant == null || state.cart.isEmpty()) {
        state.screen = Screen.Home
        return
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("Phone number") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = address, onValueChange = { address = it },
            label = { Text("Full delivery address") }, minLines = 3, modifier = Modifier.fillMaxWidth()
        )
        Text("Payment: Cash on delivery", color = Color.Gray)
        SummaryRow("Total to pay", money(state.subtotal + restaurant.deliveryCharge), bold = true)
        if (error.isNotEmpty()) Text(error, color = Color.Red)
        BigButton(if (placing) "Placing order..." else "Place Order", enabled = !placing) {
            when {
                name.isBlank() -> error = "Please enter your name."
                phone.filter { it.isDigit() }.length < 10 -> error = "Please enter a valid phone number."
                address.isBlank() -> error = "Please enter your delivery address."
                else -> {
                    error = ""
                    placing = true
                    scope.launch {
                        val order = Order(
                            id = "",
                            restaurant = restaurant,
                            lines = state.cart.toList(),
                            customerName = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            subtotal = state.subtotal,
                            deliveryCharge = restaurant.deliveryCharge
                        )
                        state.lastOrder = AppConfig.repository.placeOrder(order)
                        state.cart.clear()
                        state.cartRestaurant = null
                        state.screen = Screen.Confirmation
                    }
                }
            }
        }
    }
}

// ---------------- Confirmation ----------------

@Composable
fun ConfirmationScreen(state: AppState) {
    val order = state.lastOrder ?: run { state.screen = Screen.Home; return }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("鉁�", fontSize = 64.sp)
        Text("Thank you, ${order.customerName}!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Your order has been placed.", color = Color.Gray)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Order number: ${order.id}", fontWeight = FontWeight.Bold)
                Text("Restaurant: ${order.restaurant.name}")
                order.lines.forEach { Text("${it.quantity} 脳 ${it.item.name}") }
                Text("Deliver to: ${order.address}")
                Text("Phone: ${order.phone}")
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SummaryRow("Items", money(order.subtotal))
                SummaryRow("Delivery", money(order.deliveryCharge))
                SummaryRow("Total (cash)", money(order.total), bold = true)
            }
        }
        Spacer(Modifier.weight(1f))
        BigButton("Track my order") { state.screen = Screen.Status }
        TextButton(onClick = { state.screen = Screen.Home }) { Text("Back to home") }
    }
}

// ---------------- Order status ----------------

@Composable
fun StatusScreen(state: AppState) {
    val order = state.lastOrder ?: run { state.screen = Screen.Home; return }
    var status by remember { mutableStateOf(OrderStatus.PLACED) }

    LaunchedEffect(order.id) {
        while (true) {
            status = AppConfig.repository.getOrderStatus(order.id)
            if (status == OrderStatus.DELIVERED) break
            delay(3000)
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement 
