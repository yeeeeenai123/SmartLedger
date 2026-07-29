package com.smartledger.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val Green500 = Color(0xFF4CAF50)
val Green700 = Color(0xFF388E3C)
val Orange500 = Color(0xFFFF9800)
val Red500 = Color(0xFFE53935)
val Blue500 = Color(0xFF2196F3)

val Background = Color(0xFFF5F5F5)
val Surface = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF212121)
val OnSurface = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)
val Divider = Color(0xFFE0E0E0)

val IncomeGreen = Color(0xFF4CAF50)
val ExpenseRed = Color(0xFFE53935)

// Category Colors
val CategoryColors = mapOf(
    "餐饮" to Color(0xFFFF7043),
    "交通" to Color(0xFF42A5F5),
    "购物" to Color(0xFFAB47BC),
    "娱乐" to Color(0xFFFFCA28),
    "住房" to Color(0xFF8D6E63),
    "水电" to Color(0xFF26C6DA),
    "医疗" to Color(0xFFEF5350),
    "教育" to Color(0xFF66BB6A),
    "其他" to Color(0xFF78909C),
    "收入" to Color(0xFF4CAF50)
)

fun getCategoryColor(category: String): Color {
    return CategoryColors[category] ?: Color(0xFF78909C)
}
