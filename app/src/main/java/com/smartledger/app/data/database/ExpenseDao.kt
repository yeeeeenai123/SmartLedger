package com.smartledger.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getExpensesByDay(startOfDay: Long, endOfDay: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startOfMonth AND timestamp < :endOfMonth ORDER BY timestamp DESC")
    fun getExpensesByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'expense' AND timestamp >= :start AND timestamp < :end")
    fun getTotalExpense(start: Long, end: Long): Flow<Long?>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'income' AND timestamp >= :start AND timestamp < :end")
    fun getTotalIncome(start: Long, end: Long): Flow<Long?>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE type = 'expense' AND timestamp >= :start AND timestamp < :end GROUP BY category ORDER BY total DESC")
    fun getCategoryStats(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class CategoryTotal(
    val category: String,
    val total: Long
)
