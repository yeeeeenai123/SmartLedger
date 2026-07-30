package com.smartledger.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartledger.app.data.database.ExpenseEntity
import com.smartledger.app.ui.theme.*
import com.smartledger.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onStartAccessibility: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onTestPopup: () -> Unit,
    isOverlayPermissionGranted: Boolean,
    isAccessibilityEnabled: Boolean,
    onAddManualExpense: () -> Unit
) {
    val expenseGroups by viewModel.expenseGroups.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val selectedMonthTotal by viewModel.selectedMonthTotal.collectAsState()
    val selectedMonthIncome by viewModel.selectedMonthIncome.collectAsState()
    val selectedCategoryStats by viewModel.selectedCategoryStats.collectAsState()
    val selectedExpenseCount by viewModel.selectedExpenseCount.collectAsState()

    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ──── 月份选择器 ────
        item {
            MonthYearSelector(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onYearSelected = { viewModel.setSelectedYear(it) },
                onMonthSelected = { viewModel.setSelectedMonth(it) }
            )
        }

        // ──── 摘要卡片 ────
        item {
            SummaryCard(
                todayTotal = viewModel.formatAmount(todayTotal),
                monthTotal = viewModel.formatAmount(selectedMonthTotal),
                monthIncome = viewModel.formatAmount(selectedMonthIncome),
                expenseCount = selectedExpenseCount,
                selectedMonth = selectedMonth,
                onSettingsClick = { showSettingsDialog = true },
                onTestPopup = onTestPopup
            )
        }

        // ──── 分类统计条 ────
        item {
            CategoryStatsBar(categoryStats = selectedCategoryStats, viewModel = viewModel)
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐣", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${selectedMonth}月还没有记账记录\n付款时自动弹窗帮你记！",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        } else {
            expenseGroups.forEach { group ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📅 ${group.dateLabel}",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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

        item { Spacer(Modifier.height(88.dp)) }
    }

    // ──── 悬浮按钮 ────
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape,
            containerColor = MintPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加记账")
        }
    }

    if (showAddDialog) {
        ManualAddDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amount, category, note ->
                viewModel.addManualExpense(amount, category, note)
                showAddDialog = false
            }
        )
    }

    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除记录", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这条记录吗？删除后无法恢复～") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(id)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onEnableAccessibility = onStartAccessibility,
            onEnableOverlay = onOpenOverlaySettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onTestPopup = onTestPopup,
            isOverlayPermissionGranted = isOverlayPermissionGranted,
            isAccessibilityEnabled = isAccessibilityEnabled
        )
    }
}

// ═══════════════════════════════════════════════════
//  月份/年份选择器
// ═══════════════════════════════════════════════════
@Composable
private fun MonthYearSelector(
    selectedYear: Int,
    selectedMonth: Int,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val years = (currentYear - 2..currentYear + 1).toList()
    val months = (1..12).toList()

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // 年份选择
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(years) { year ->
                val isSelected = year == selectedYear
                val isFuture = year > currentYear
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Lavender else SurfaceVariant,
                    modifier = Modifier.clickable { onYearSelected(year) }
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

        Spacer(Modifier.height(6.dp))

        // 月份选择
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(months) { month ->
                val isSelected = month == selectedMonth
                val isCurrent = month == currentMonth && selectedYear == currentYear
                val isFuture = selectedYear == currentYear && month > currentMonth

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isSelected -> MintPrimary
                        isFuture -> SurfaceVariant.copy(alpha = 0.5f)
                        else -> SurfaceVariant
                    },
                    modifier = Modifier.clickable(enabled = !isFuture) { onMonthSelected(month) }
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

// ═══════════════════════════════════════════════════
//  摘要卡片 — 渐变背景，可爱设计
// ═══════════════════════════════════════════════════
@Composable
private fun SummaryCard(
    todayTotal: String,
    monthTotal: String,
    monthIncome: String,
    expenseCount: Int,
    selectedMonth: Int,
    onSettingsClick: () -> Unit,
    onTestPopup: () -> Unit
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
                        colors = listOf(MintPrimary, SkyBlue)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // 第一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🌸 ${selectedMonth}月总结",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 本月支出总额
                Text(
                    monthTotal,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "本月支出 · 共${expenseCount}笔",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(16.dp))

                // 支出 / 收入 / 今日
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem(label = "💸 支出", value = monthTotal)
                    SummaryItem(label = "💰 收入", value = monthIncome)
                    SummaryItem(label = "📅 今日", value = todayTotal)
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(
            value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ═══════════════════════════════════════════════════
//  分类统计条
// ═══════════════════════════════════════════════════
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "📊 分类详情",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            categoryStats.take(5).forEach { stat ->
                val percentage = (stat.total.toFloat() / total)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${getCategoryEmoji(stat.category)} ${stat.category}",
                        fontSize = 12.sp,
                        color = OnSurface,
                        modifier = Modifier.width(80.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Divider)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(getCategoryColor(stat.category))
                        )
                    }
                    Text(
                        viewModel.formatAmount(stat.total),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurface,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(70.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
//  记账卡片
// ═══════════════════════════════════════════════════
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
            // 分类图标圆圈
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(expense.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getCategoryEmoji(expense.category),
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        expense.category,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = OnSurface
                    )
                    expense.sourceApp?.let { source ->
                        if (source != "手动记账") {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = getCategoryColor(expense.category).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    source,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = getCategoryColor(expense.category)
                                )
                            }
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
                if (expense.type == "expense")
                    "-${viewModel.formatAmountPlain(expense.amount)}"
                else
                    "+${viewModel.formatAmountPlain(expense.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (expense.type == "expense") ExpenseRed else IncomeGreen
            )
        }
    }
}

// ═══════════════════════════════════════════════════
//  手动添加对话框
// ═══════════════════════════════════════════════════
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
        shape = RoundedCornerShape(24.dp),
        title = { Text("✏️ 手动记账", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额") },
                    prefix = { Text("¥", fontSize = 18.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("选择分类", fontSize = 13.sp, color = TextSecondary)
                // 分类 chips
                val rows = categories.chunked(4)
                rows.forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowCats.forEach { cat ->
                            FilterChip(
                                selected = cat == selectedCategory,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text("${getCategoryEmoji(cat)} $cat", fontSize = 12.sp)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
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
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ═══════════════════════════════════════════════════
//  设置对话框
// ═══════════════════════════════════════════════════
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onTestPopup: () -> Unit,
    isOverlayPermissionGranted: Boolean,
    isAccessibilityEnabled: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("⚙️ 设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "智能记账会在你支付成功后自动弹出记账窗口～请确保以下权限已开启：",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                SettingsItem(
                    icon = "🔓",
                    title = "无障碍服务",
                    subtitle = if (isAccessibilityEnabled) "已开启 ✅" else "未开启！点击去开启",
                    onClick = onEnableAccessibility,
                    isDone = isAccessibilityEnabled
                )

                SettingsItem(
                    icon = "🪟",
                    title = "悬浮窗权限",
                    subtitle = if (isOverlayPermissionGranted) "已开启 ✅" else "未开启！点击去开启",
                    onClick = onEnableOverlay,
                    isDone = isOverlayPermissionGranted
                )

                Divider(color = Divider)

                Text("⚠️ 国产手机必看", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Orange500)

                SettingsItem(
                    icon = "🔋",
                    title = "关闭电池优化",
                    subtitle = "防止系统杀死后台监控 (OPPO/vivo必做!)",
                    onClick = onOpenBatterySettings,
                    isDone = false,
                    actionColor = Orange500
                )

                Divider(color = Divider)

                Text("🧪 功能测试", fontWeight = FontWeight.Medium, fontSize = 14.sp)

                SettingsItem(
                    icon = "🧪",
                    title = "测试弹窗",
                    subtitle = "点击测试悬浮窗是否能正常弹出",
                    onClick = onTestPopup,
                    isDone = false,
                    actionColor = SkyBlue
                )

                Divider(color = Divider)

                Text("🔒 隐私说明", fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDone: Boolean,
    actionColor: Color = if (isDone) IncomeGreen else ExpenseRed
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = actionColor, lineHeight = 16.sp)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ═══════════════════════════════════════════════════
//  工具函数
// ═══════════════════════════════════════════════════
fun getCategoryEmoji(category: String): String = when (category) {
    "餐饮" -> "🍜"
    "交通" -> "🚗"
    "购物" -> "🛍"
    "娱乐" -> "🎮"
    "住房" -> "🏠"
    "水电" -> "💡"
    "医疗" -> "🏥"
    "教育" -> "📚"
    "转账" -> "💸"
    "其他" -> "📌"
    "收入" -> "💰"
    "工资" -> "💰"
    "奖金" -> "🎉"
    "兼职" -> "💼"
    "退款" -> "↩️"
    "理财" -> "📈"
    "报销" -> "🧾"
    "红包" -> "🧧"
    "其他收入" -> "💝"
    else -> "📌"
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
