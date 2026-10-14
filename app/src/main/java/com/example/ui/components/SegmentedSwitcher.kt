package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.SubView

@Composable
fun SegmentedSwitcher(
    activeView: SubView,
    onViewSelected: (SubView) -> Unit,
    listLabel: String = "📋 عرض القائمة والسجل",
    formLabel: String = "➕ إضافة جديد",
    accentColor: Color = NavyBrand
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE2E8F0))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Option 1: List
            val isList = activeView == SubView.LIST
            val listBg by animateColorAsState(if (isList) Color.White else Color.Transparent, label = "listBg")
            val listTextColor by animateColorAsState(if (isList) accentColor else TextSecondary, label = "listText")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(if (isList) Modifier.shadow(2.dp, RoundedCornerShape(9.dp)) else Modifier)
                    .background(listBg)
                    .clickable { onViewSelected(SubView.LIST) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = listLabel,
                    color = listTextColor,
                    fontSize = 13.sp,
                    fontWeight = if (isList) FontWeight.Bold else FontWeight.Medium
                )
            }

            // Option 2: Add Form
            val isForm = activeView == SubView.FORM
            val formBg by animateColorAsState(if (isForm) Color.White else Color.Transparent, label = "formBg")
            val formTextColor by animateColorAsState(if (isForm) accentColor else TextSecondary, label = "formText")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(if (isForm) Modifier.shadow(2.dp, RoundedCornerShape(9.dp)) else Modifier)
                    .background(formBg)
                    .clickable { onViewSelected(SubView.FORM) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formLabel,
                    color = formTextColor,
                    fontSize = 13.sp,
                    fontWeight = if (isForm) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
