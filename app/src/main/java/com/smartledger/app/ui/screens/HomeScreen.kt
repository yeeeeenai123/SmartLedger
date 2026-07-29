package com.smartledger.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartledger.app.data.database.ExpenseEntity
import com.smartledger.app.ui.theme.*
import com.smartledger.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onStartAccessibility: () -> Unit,
    onAddManualExpense: () -> Unit
) {
    val expenseGroups by viewModel.expenseGroups.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val monthTotal by viewModel.monthTotal.collectAsState()
    val monthIncome by viewModel.monthIncome.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ──── 顶部摘要区 ────
        item {
            SummaryHeader(
                todayTotal = viewModel.formatAmount(todayTotal),
                monthTotal = viewModel.formatAmount(monthTotal),
                monthIncome = viewModel.formatAmount(monthIncome),
                onSettingsClick = { showSettingsDialog = true }
            )
        }

        // ──── 本月分类统计 ────
        item {
            CategoryStatsBar(categoryStats = categoryStats, viewModel = viewModel)
        }

        // ──── 记账列表 ────
        if (expenseGroups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "还没有记账记录\n付款时自动弹窗帮你记！",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            expenseGroups.forEach { group ->
                item {
                    Text(
                        group.dateLabel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                items(group.expenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        viewModel = viewModel,
                        onLongClick = { showDeleteDialog = expense.id }
                    )
                }
            }
        }

        // 底部留白（给FAB空间）
        item { Spacer(Modifier.height(88.dp)) }
    }

    // ──── 悬浮按钮 ────
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Green500,
            contentColor = OnBackground
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加记账")
        }
    }

    // ──── 手动添加对话框 ────
    if (showAddDialog) {
        ManualAddDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amount, category, note ->
                viewModel.addManualExpense(amount, category, note)
                showAddDialog = false
            }
        )
    }

    // ──── 删除确认对话框 ────
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(id)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = Red500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    // ──── 设置对话框 ────
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onEnableAccessibility = onStartAccessibility
        )
    }
}

@Composable
private fun SummaryHeader(
    todayTotal: String,
    monthTotal: String,
    monthIncome: String,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Green500)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 第一行：今日支出
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今日支出", color = OnBackground.copy(alpha = 0.85f), fontSize = 14.sp)
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = OnBackground.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                todayTotal,
                color = OnBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 第二行：本月支出 / 本月收入
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "本月支出",
                        color = OnBackground.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        monthTotal,
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "本月收入",
                        color = OnBackground.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        monthIncome,
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryStatsBar(
    categoryStats: List<com.smartledger.app.data.database.CategoryTotal>,
    viewModel: MainViewModel
) {
    if (categoryStats.isEmpty()) return

    val total = categoryStats.sumOf { it.total }
    if (total == 0L) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "本月分类",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryStats.take(5).forEach { stat ->
                    val percentage = (stat.total.toFloat() / total)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stat.category,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Divider)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(getCategoryColor(stat.category))
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            viewModel.formatAmount(stat.total),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseCard(
    expense: ExpenseEntity,
    viewModel: MainViewModel,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .combinedClickable(onClick = {}, onLongClick = { onLongClick() }),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(expense.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getCategoryEmoji(expense.category),
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        expense.category,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = OnSurface
                    )
                    expense.sourceApp?.let { source ->
                        if (source != "手动记账") {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "·$source",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                expense.note?.let {
                    if (it.isNotBlank()) {
                        Text(
                            it,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    formatTime(expense.timestamp),
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }

            Text(
                viewModel.formatAmount(expense.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (expense.type == "expense") ExpenseRed else IncomeGreen
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("餐饮") }
    var note by remember { mutableStateOf("") }

    val categories = listOf("餐饮", "交通", "购物", "娱乐", "住房", "水电", "医疗", "教育", "其他")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动记账", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额 (元)") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 分类选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text("${getCategoryEmoji(cat)} $cat", fontSize = 12.sp)
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.drop(4).forEach { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text("${getCategoryEmoji(cat)} $cat", fontSize = 12.sp)
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onSave(amount, selectedCategory, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green500),
                enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onEnableAccessibility: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "智能记账通过 Android 无障碍服务自动检测付款页面。请确保以下权限已开启：",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                SettingsItem(
                    icon = Icons.Filled.Accessibility,
                    title = "无障碍服务",
                    subtitle = "前往系统设置 → 无障碍 → 智能记账 → 开启",
                    onClick = onEnableAccessibility
                )

                SettingsItem(
                    icon = Icons.Filled.OpenInNew,
                    title = "悬浮窗权限",
                    subtitle = "允许在其他应用上层显示记账窗口",
                    onClick = {} // Android 弹出系统悬浮窗设置
                )

                Divider(color = Divider)

                Text(
                    "隐私说明",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    "本应用仅在本地处理屏幕内容，不会收集、上传或分享您的任何数据。所有记账记录仅存储在您的设备上。",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Green500, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}

// ──── 工具函数 ────
private fun getCategoryEmoji(category: String): String = when (category) {
    "餐饮" -> "🍜"
    "交通" -> "🚗"
    "购物" -> "🛍"
    "娱乐" -> "🎮"
    "住房" -> "🏠"
    "水电" -> "💡"
    "医疗" -> "🏥"
    "教育" -> "📚"
    "其他" -> "📌"
    "收入" -> "💰"
    else -> "📌"
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
