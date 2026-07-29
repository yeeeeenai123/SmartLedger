package com.smartledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartledger.app.data.database.CategoryTotal
import com.smartledger.app.ui.theme.*
import com.smartledger.app.viewmodel.MainViewModel

@Composable
fun StatsScreen(viewModel: MainViewModel) {
    val monthTotal by viewModel.monthTotal.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()

    val total = categoryStats.sumOf { it.total }.toFloat()
    val totalIncome = 0L // 后续可扩展

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 月度概览
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "本月总支出",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        viewModel.formatAmount(monthTotal),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }

        // 消费分类排行
        item {
            Text(
                "消费分类排行",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (categoryStats.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("本月暂无消费记录", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            categoryStats.forEachIndexed { index, stat ->
                val percentage = if (total > 0) stat.total / total else 0f
                item {
                    CategoryRankItem(
                        rank = index + 1,
                        category = stat.category,
                        amount = viewModel.formatAmount(stat.total),
                        percentage = percentage,
                        maxPercentage = if (categoryStats.isNotEmpty())
                            categoryStats[0].total / total else 1f
                    )
                }
            }
        }

        // 底部装饰
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategoryRankItem(
    rank: Int,
    category: String,
    amount: String,
    percentage: Float,
    maxPercentage: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Text(
                "$rank",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Green500 else TextSecondary,
                modifier = Modifier.width(28.dp)
            )

            Spacer(Modifier.width(8.dp))

            // 分类名
            Text(
                when (category) {
                    "餐饮" -> "🍜 餐饮"
                    "交通" -> "🚗 交通"
                    "购物" -> "🛍 购物"
                    "娱乐" -> "🎮 娱乐"
                    "住房" -> "🏠 住房"
                    "水电" -> "💡 水电"
                    "医疗" -> "🏥 医疗"
                    "教育" -> "📚 教育"
                    "其他" -> "📌 其他"
                    "收入" -> "💰 收入"
                    else -> "📌 $category"
                },
                fontSize = 14.sp,
                modifier = Modifier.width(100.dp)
            )

            Spacer(Modifier.width(12.dp))

            // 进度条
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Divider)
            ) {
                val barWidth = if (maxPercentage > 0) percentage / maxPercentage else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barWidth.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(getCategoryColor(category))
                )
            }

            Spacer(Modifier.width(12.dp))

            // 金额
            Text(
                amount,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )

            Spacer(Modifier.width(4.dp))

            Text(
                "${(percentage * 100).toInt()}%",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
