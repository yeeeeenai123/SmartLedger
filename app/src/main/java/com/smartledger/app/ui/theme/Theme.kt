package com.smartledger.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════
//  可爱风配色 — 柔和、温暖、治愈系
// ═══════════════════════════════════════════════════

// 主色 — 薄荷绿
val MintPrimary = Color(0xFF6FCF97)
val MintDark = Color(0xFF27AE60)
val MintLight = Color(0xFFE8F8F0)

// 辅色 — 樱花粉
val PinkAccent = Color(0xFFFF6B9D)
val PinkLight = Color(0xFFFFF0F5)

// 辅色 — 天空蓝
val SkyBlue = Color(0xFF74B9FF)
val SkyLight = Color(0xFFEEF7FF)

// 辅色 — 阳光橙
val SunnyOrange = Color(0xFFFFB347)
val SunnyLight = Color(0xFFFFF8F0)

// 辅色 — 薰衣草紫
val Lavender = Color(0xFFA29BFE)
val LavenderLight = Color(0xFFF5F3FF)

// 背景
val Background = Color(0xFFFDF6F8)      // 淡粉米白
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF8F4F6)  // 淡粉灰
val OnBackground = Color(0xFF2D3436)
val OnSurface = Color(0xFF2D3436)
val TextSecondary = Color(0xFF718093)
val Divider = Color(0xFFF0E8EC)

// 收支颜色
val IncomeGreen = Color(0xFF27AE60)
val ExpenseRed = Color(0xFFE17055)

// 兼容旧引用
val Green500 = MintPrimary
val Green700 = MintDark
val Orange500 = SunnyOrange
val Red500 = ExpenseRed
val Blue500 = SkyBlue

// 分类颜色 — 鲜艳但柔和
val CategoryColors = mapOf(
    "餐饮" to Color(0xFFFF7675),      // 珊瑚红
    "交通" to Color(0xFF74B9FF),      // 天空蓝
    "购物" to Color(0xFFA29BFE),      // 薰衣草紫
    "娱乐" to Color(0xFFFFB347),      // 阳光橙
    "住房" to Color(0xFF8D6E63),      // 暖棕
    "水电" to Color(0xFF26C6DA),      // 青色
    "医疗" to Color(0xFFEF5350),      // 珊瑚红
    "教育" to Color(0xFF66BB6A),      // 草绿
    "转账" to Color(0xFF78909C),      // 蓝灰
    "其他" to Color(0xFFB39DDB),      // 淡紫
    "收入" to Color(0xFF27AE60),      // 深绿
    "工资" to Color(0xFF27AE60),
    "奖金" to Color(0xFFFFB347),
    "兼职" to Color(0xFF74B9FF),
    "退款" to Color(0xFF26C6DA),
    "理财" to Color(0xFFA29BFE),
    "报销" to Color(0xFF66BB6A),
    "红包" to Color(0xFFFF7675),
    "其他收入" to Color(0xFFB39DDB)
)

fun getCategoryColor(category: String): Color {
    return CategoryColors[category] ?: Color(0xFFB39DDB)
}
