package com.smartledger.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartledger.app.data.database.CategoryTotal
import com.smartledger.app.ui.theme.*
import com.smartledger.app.viewmodel.MainViewModel
import java.util.*

@Composable
fun StatsScreen(viewModel: MainViewModel) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val isYearlyView by viewModel.isYearlyView.collectAsState()

    // 选中月份的数据
    val monthExpense by viewModel.selectedMonthTotal.collectAsState()
    val monthIncome by viewModel.selectedMonthIncome.collectAsState()
    val monthCategoryStats by viewModel.selectedCategoryStats.collectAsState()
    val monthExpenseCount by viewModel.selectedExpenseCount.collectAsState()

    // 年度数据
    val yearlyExpense by viewModel.yearlyTotal.collectAsState()
    val yearlyIncome by viewModel.yearlyIncome.collectAsState()
    val yearlyCount by viewModel.yearlyExpenseCount.collectAsState()

    val displayExpense = if (isYearlyView) yearlyExpense else monthExpense
    val displayIncome = if (isYearlyView) yearlyIncome else monthIncome
    val displayCount = if (isYearlyView) yearlyCount else monthExpenseCount
    val displayCategoryStats = monthCategoryStats // 用月度分类

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // ──── 视图切换：月度 / 年度 ────
        item {
            ViewToggle(
                isYearlyView = isYearlyView,
                onToggle = { viewModel.setYearlyView(it) }
            )
        }

        // ──── 年份选择 ────
        item {
            Text(
                "选择年份",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items((currentYear - 2..currentYear).toList()) { year ->
                    val isSelected = year == selectedYear
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Lavender else SurfaceVariant,
                        modifier = Modifier.clickable { viewModel.setSelectedYear(year) }
                    ) {
                        Text(
                            "${year}年",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // ──── 月份选择（只在月度视图显示） ────
        if (!isYearlyView) {
            item {
                Text(
                    "选择月份",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items((1..12).toList()) { month ->
                        val isSelected = month == selectedMonth
                        val isFuture = selectedYear == currentYear && month > currentMonth

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isSelected -> MintPrimary
                                isFuture -> SurfaceVariant.copy(alpha = 0.5f)
                                else -> SurfaceVariant
                            },
                            modifier = Modifier.clickable(enabled = !isFuture) {
                                viewModel.setSelectedMonth(month)
                            }
                        ) {
                            Text(
                                "${month}月",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                color = when {
                                    isSelected -> Color.White
                                    isFuture -> TextSecondary.copy(alpha = 0.4f)
                                    else -> TextSecondary
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        // ──── 总览卡片 ────
        item {
            StatsOverviewCard(
                title = if (isYearlyView) "${selectedYear}年总览" else "${selectedMonth}月总览",
                totalExpense = viewModel.formatAmount(displayExpense),
                totalIncome = viewModel.formatAmount(displayIncome),
                count = displayCount
            )
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ──── 收支结余 ────
        item {
            val balance = displayIncome - displayExpense
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BalanceItem(
                        icon = "💸",
                        label = "总支出",
                        amount = viewModel.formatAmountPlain(displayExpense),
                        color = ExpenseRed
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Divider)
                    )
                    BalanceItem(
                        icon = "💰",
                        label = "总收入",
                        amount = viewModel.formatAmountPlain(displayIncome),
                        color = IncomeGreen
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Divider)
                    )
                    BalanceItem(
                        icon = "🪙",
                        label = "结余",
                        amount = viewModel.formatAmountPlain(balance),
                        color = if (balance >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ──── 消费分类排行 ────
        item {
            Text(
                if (isYearlyView) "📊 年度分类排行" else "📊 ${selectedMonth}月分类排行",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (displayCategoryStats.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌿", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("暂无消费记录", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        } else {
            val total = displayCategoryStats.sumOf { it.total }.toFloat()
            val maxPct = displayCategoryStats.firstOrNull()?.total?.toFloat() ?: 1f

            displayCategoryStats.forEachIndexed { index, stat ->
                val percentage = if (total > 0) stat.total / total else 0f
                item {
                    CategoryRankItem(
                        rank = index + 1,
                        category = stat.category,
                        amount = viewModel.formatAmount(stat.total),
                        percentage = percentage,
                        maxPercentage = if (maxPct > 0) stat.total.toFloat() / maxPct else 1f,
                        viewModel = viewModel
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════
//  视图切换
// ═══════════════════════════════════════════════════
@Composable
private fun ViewToggle(
    isYearlyView: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                ToggleChip(
                    text = "📅 月度",
                    isSelected = !isYearlyView,
                    onClick = { onToggle(false) }
                )
                ToggleChip(
                    text = "📆 年度",
                    isSelected = isYearlyView,
                    onClick = { onToggle(true) }
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MintPrimary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ═══════════════════════════════════════════════════
//  总览卡片
// ═══════════════════════════════════════════════════
@Composable
private fun StatsOverviewCard(
    title: String,
    totalExpense: String,
    totalIncome: String,
    count: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PinkAccent, SunnyOrange)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "✨ $title",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    totalExpense,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "总支出 · ${count}笔交易",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════
//  收支结余项
// ═══════════════════════════════════════════════════
@Composable
private fun BalanceItem(
    icon: String,
    label: String,
    amount: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(
            "¥$amount",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ═══════════════════════════════════════════════════
//  分类排行项
// ═══════════════════════════════════════════════════
@Composable
private fun CategoryRankItem(
    rank: Int,
    category: String,
    amount: String,
    percentage: Float,
    maxPercentage: Float,
    viewModel: MainViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名圆圈
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> Divider
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$rank",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color.White else TextSecondary
                )
            }

            Spacer(Modifier.width(12.dp))

            // 分类名+emoji
            Text(
                "${getCategoryEmoji(category)} $category",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(90.dp)
            )

            // 进度条
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Divider)
            ) {
                val barWidth = if (maxPercentage > 0) percentage / maxPercentage else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barWidth.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(getCategoryColor(category))
                )
            }

            Spacer(Modifier.width(12.dp))

            // 金额
            Text(
                amount,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.width(70.dp)
            )

            Text(
                " ${(percentage * 100).toInt()}%",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
