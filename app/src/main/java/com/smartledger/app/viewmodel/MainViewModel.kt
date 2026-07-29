package com.smartledger.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartledger.app.data.database.ExpenseEntity
import com.smartledger.app.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {

    private val repository = ExpenseRepository()

    // ──── 数据流 ────
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ──── 状态 ────
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    // ──── 统计数据 ────
    val todayExpenses: StateFlow<List<ExpenseEntity>> = flow {
        while (true) {
            val (start, end) = getTodayRange()
            emit(repository.getExpensesByDay(start, end).first())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotal: StateFlow<Long> = todayExpenses.map { list ->
        list.filter { it.type == "expense" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthTotal: StateFlow<Long> = flow {
        while (true) {
            val (start, end) = getMonthRange()
            val total = repository.getTotalExpense(start, end).first() ?: 0
            emit(total)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthIncome: StateFlow<Long> = flow {
        while (true) {
            val (start, end) = getMonthRange()
            val total = repository.getTotalIncome(start, end).first() ?: 0
            emit(total)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoryStats = flow {
        while (true) {
            val (start, end) = getMonthRange()
            val stats = repository.getCategoryStats(start, end).first()
            emit(stats)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ──── 带日期头的列表 ────
    data class ExpenseGroup(val dateLabel: String, val expenses: List<ExpenseEntity>)

    val expenseGroups: StateFlow<List<ExpenseGroup>> = allExpenses.map { list ->
        val dateFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINESE)
        val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        list.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            val today = todayFormat.format(Date())

            if (todayFormat.format(cal.time) == today) "今天"
            else dateFormat.format(Date(it.timestamp))
        }.map { (key, value) -> ExpenseGroup(key, value) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ──── 操作 ────
    fun setSelectedTab(tab: Int) { _selectedTab.value = tab }
    fun setAccessibilityEnabled(enabled: Boolean) { _isAccessibilityEnabled.value = enabled }
    fun setOverlayPermissionGranted(granted: Boolean) { _isOverlayPermissionGranted.value = granted }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun addManualExpense(amount: Double, category: String, note: String) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                amount = (amount * 100).toLong(),
                category = category,
                type = "expense",
                sourceApp = "手动记账",
                note = note.ifBlank { null }
            )
            repository.insert(expense)
        }
    }

    // ──── 工具方法 ────
    fun formatAmount(cents: Long): String {
        val yuan = cents / 100.0
        return if (yuan == yuan.toLong().toDouble()) {
            "¥${yuan.toLong()}"
        } else {
            "¥${String.format("%.2f", yuan)}"
        }
    }

    companion object {
        fun getTodayRange(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val end = cal.timeInMillis
            return start to end
        }

        fun getMonthRange(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            return start to end
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel() as T
        }
    }
}
