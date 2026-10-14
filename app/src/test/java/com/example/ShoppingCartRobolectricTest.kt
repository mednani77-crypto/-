package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.repository.SyscoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingCartRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SyscoRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SyscoRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAddToCartAndRetrieve() = runBlocking {
        val product = Product(
            id = 101L,
            name = "أرز بسمتي فاخر",
            category = "حبوب",
            specs = "كيس 25 كغ",
            priceUsd = 24.50,
            priceDjf = 4350.0,
            stock = 80
        )

        val added = repository.addToCart(product, quantityToAdd = 2)
        assertTrue(added)

        val items = repository.cartItems.first()
        assertEquals(1, items.size)
        assertEquals(101L, items[0].productId)
        assertEquals(2, items[0].quantity)
        assertEquals(49.0, items[0].totalUsd, 0.001)

        val totalCount = repository.totalCartCount.first()
        assertEquals(2, totalCount)
    }

    @Test
    fun testUpdateQuantityAndRemove() = runBlocking {
        val product = Product(
            id = 102L,
            name = "زيت طهي نباتي",
            category = "زيوت",
            priceUsd = 18.0,
            priceDjf = 3200.0,
            stock = 50
        )

        repository.addToCart(product, quantityToAdd = 1)
        repository.updateCartQuantity(product.id, newQuantity = 4)

        var items = repository.cartItems.first()
        assertEquals(4, items[0].quantity)

        repository.removeFromCart(product.id)
        items = repository.cartItems.first()
        assertTrue(items.isEmpty())
    }
}
