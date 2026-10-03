package com.rindjada.food

import kotlinx.coroutines.delay

// =====================================================================
//  THIS IS THE FILE YOU EDIT TO ADD RESTAURANTS, FOOD, PRICES, CHARGES
// =====================================================================

// ---------- Data shapes (do not change unless you know what you are doing) ----------

data class Restaurant(
    val id: String,
    val name: String,
    val description: String,
    val deliveryCharge: Int,          // delivery charge in Rs
    val imageUrl: String = "",        // optional internet link of a photo
    val emoji: String = "馃嵔锔�",        // shown when there is no photo
    val isOpen: Boolean = true
)

data class FoodItem(
    val id: String,
    val restaurantId: String,         // must match a Restaurant id
    val name: String,
    val price: Int,                   // price in Rs
    val imageUrl: String = "",        // optional internet link of a photo
    val emoji: String = "馃崨"
)

data class CartLine(val item: FoodItem, val quantity: Int)

enum class OrderStatus(val label: String) {
    PLACED("Order placed"),
    PREPARING("Preparing your food"),
    ON_THE_WAY("On the way"),
    DELIVERED("Delivered")
}

data class Order(
    val id: String,
    val restaurant: Restaurant,
    val lines: List<CartLine>,
    val customerName: String,
    val phone: String,
    val address: String,
    val subtotal: Int,
    val deliveryCharge: Int
) {
    val total: Int get() = subtotal + deliveryCharge
}

// ---------- The "door" to your data ----------
// Today the app reads from the list below (LocalFoodRepository).
// Later, you (or a developer) can write ServerFoodRepository that talks to
// a backend/database and change ONE line in AppConfig. Nothing else changes.

interface FoodRepository {
    suspend fun getRestaurants(): List<Restaurant>
    suspend fun getFoodItems(restaurantId: String): List<FoodItem>
    suspend fun placeOrder(order: Order): Order
    suspend fun getOrderStatus(orderId: String): OrderStatus
}

object AppConfig {
    const val APP_NAME = "Rind Jada Food"
    const val AREA = "Rind Jada / Kehror Pacca"
    const val CURRENCY = "Rs"

    // To connect a backend later, replace LocalFoodRepository() with your own class.
    val repository: FoodRepository = LocalFoodRepository()
}

// ---------- Sample data stored inside the app ----------

class LocalFoodRepository : FoodRepository {

    // >>> ADD OR CHANGE RESTAURANTS HERE <<<
    // Copy one Restaurant(...) block, paste it below, change the values.
    private val restaurants = listOf(
        Restaurant(
            id = "r1",
            name = "Rind Jada Fast Food",
            description = "Burgers, rolls and fries",
            deliveryCharge = 50,
            emoji = "馃崝"
        ),
        Restaurant(
            id = "r2",
            name = "Kehror Pacca Biryani House",
            description = "Biryani, karahi and naan",
            deliveryCharge = 70,
            emoji = "馃崨"
        ),
        Restaurant(
            id = "r3",
            name = "Desi Dhaba Hotel",
            description = "Desi food, tea and paratha",
            deliveryCharge = 40,
            emoji = "馃珦"
        )
    )

    // >>> ADD OR CHANGE FOOD ITEMS AND PRICES HERE <<<
    // restaurantId must be the same as the restaurant "id" above.
    // imageUrl is optional: paste a photo link inside the quotes to show a photo.
    private val foodItems = listOf(
        FoodItem("f1", "r1", "Zinger Burger", 350, emoji = "馃崝"),
        FoodItem("f2", "r1", "Chicken Roll", 200, emoji = "馃尟"),
        FoodItem("f3", "r1", "French Fries", 150, emoji = "馃崯"),
        FoodItem("f4", "r1", "Cold Drink", 80, emoji = "馃イ"),

        FoodItem("f5", "r2", "Chicken Biryani", 300, emoji = "馃崨"),
        FoodItem("f6", "r2", "Chicken Karahi (Half)", 900, emoji = "馃嵅"),
        FoodItem("f7", "r2", "Naan", 25, emoji = "馃珦"),
        FoodItem("f8", "r2", "Raita", 40, emoji = "馃ィ"),

        FoodItem("f9", "r3", "Aloo Paratha", 120, emoji = "馃珦"),
        FoodItem("f10", "r3", "Daal Chawal", 220, emoji = "馃崥"),
        FoodItem("f11", "r3", "Doodh Patti Chai", 60, emoji = "鈽�"),
        FoodItem("f12", "r3", "Lassi", 100, emoji = "馃")
    )

    // Orders are kept only while the app is open (demo). A backend would store them for real.
    private val orders = mutableMapOf<String, Pair<Order, Long>>()

    override suspend fun getRestaurants(): List<Restaurant> {
        delay(300)
        return restaurants
    }

    override suspend fun getFoodItems(restaurantId: String): List<FoodItem> {
        delay(200)
        return foodItems.filter { it.restaurantId == restaurantId }
    }

    override suspend fun placeOrder(order: Order): Order {
        delay(500)
        val placed = order.copy(id = "RJF-" + (1000..9999).random())
        orders[placed.id] = Pair(placed, System.currentTimeMillis())
        return placed
    }

    // DEMO ONLY: status moves forward automatically with time.
    // With a real backend, the restaurant/admin would change the status instead.
    override suspend fun getOrderStatus(orderId: String): OrderStatus {
        val started = orders[orderId]?.second ?: return OrderStatus.PLACED
        val seconds = (System.currentTimeMillis() - started) / 1000
        return when {
            seconds < 10 -> OrderStatus.PLACED
            seconds < 25 -> OrderStatus.PREPARING
            seconds < 45 -> OrderStatus.ON_THE_WAY
            else -> OrderStatus.DELIVERED
        }
    }
}
