package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab

@Composable
fun BottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp)
            .navigationBarsPadding(),
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(AppTab.DASHBOARD, "المؤشرات", Icons.Default.Dashboard),
            Triple(AppTab.CUSTOMERS, "العملاء", Icons.Default.People),
            Triple(AppTab.INVOICES, "الفواتير", Icons.Default.ReceiptLong),
            Triple(AppTab.PAYMENTS, "السندات", Icons.Default.Payments),
            Triple(AppTab.CATALOG, "الكتالوج", Icons.Default.Inventory2),
            Triple(AppTab.AI_CHAT, "مساعد AI", Icons.Default.AutoAwesome)
        )

        for ((tab, label, icon) in items) {
            val selected = currentTab == tab
            val tabColor = when (tab) {
                AppTab.DASHBOARD -> NavyBrand
                AppTab.CUSTOMERS -> NavyBrand
                AppTab.INVOICES -> AmberSales
                AppTab.PAYMENTS -> EmeraldCollection
                AppTab.CATALOG -> IndigoCatalog
                AppTab.AI_CHAT -> Color(0xFFD97706)
                else -> NavyBrand
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) tabColor else TextMuted
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) tabColor else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = when (tab) {
                        AppTab.INVOICES -> AmberSalesLight
                        AppTab.PAYMENTS -> EmeraldCollectionLight
                        AppTab.CATALOG -> IndigoCatalogLight
                        AppTab.AI_CHAT -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDBEAFE)
                    }
                )
            )
        }
    }
}
