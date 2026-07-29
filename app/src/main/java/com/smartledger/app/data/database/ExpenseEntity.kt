package com.smartledger.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 金额，单位：分 */
    val amount: Long,
    /** 分类 */
    val category: String,
    /** 类型：expense 支出 / income 收入 */
    val type: String = "expense",
    /** 来源App包名（如 com.tencent.mm） */
    val sourcePackage: String? = null,
    /** 来源App名称（如 微信） */
    val sourceApp: String? = null,
    /** 备注 */
    val note: String? = null,
    /** 记账时间戳（毫秒） */
    val timestamp: Long = System.currentTimeMillis()
)
