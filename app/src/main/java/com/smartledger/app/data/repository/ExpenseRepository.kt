package com.smartledger.app.data.repository

import com.smartledger.app.SmartLedgerApp
import com.smartledger.app.data.database.ExpenseDao
import com.smartledger.app.data.database.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository {

    private val dao: ExpenseDao = SmartLedgerApp.instance.database.expenseDao()

    fun getAllExpenses(): Flow<List<ExpenseEntity>> = dao.getAllExpenses()

    fun getExpensesByDay(start: Long, end: Long): Flow<List<ExpenseEntity>> =
        dao.getExpensesByDay(start, end)

    fun getExpensesByMonth(start: Long, end: Long): Flow<List<ExpenseEntity>> =
        dao.getExpensesByMonth(start, end)

    fun getExpensesByRange(start: Long, end: Long): Flow<List<ExpenseEntity>> =
        dao.getExpensesByRange(start, end)

    fun getTotalExpense(start: Long, end: Long): Flow<Long?> =
        dao.getTotalExpense(start, end)

    fun getTotalIncome(start: Long, end: Long): Flow<Long?> =
        dao.getTotalIncome(start, end)

    fun getAllTimeExpense(): Flow<Long?> = dao.getAllTimeExpense()

    fun getAllTimeIncome(): Flow<Long?> = dao.getAllTimeIncome()

    fun getCategoryStats(start: Long, end: Long) =
        dao.getCategoryStats(start, end)

    fun getIncomeCategoryStats(start: Long, end: Long) =
        dao.getIncomeCategoryStats(start, end)

    fun getExpenseCount(start: Long, end: Long): Flow<Int?> =
        dao.getExpenseCount(start, end)

    suspend fun insert(expense: ExpenseEntity): Long = dao.insert(expense)

    suspend fun update(expense: ExpenseEntity) = dao.update(expense)

    suspend fun delete(expense: ExpenseEntity) = dao.delete(expense)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
