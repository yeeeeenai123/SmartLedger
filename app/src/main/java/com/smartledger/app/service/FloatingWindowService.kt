package com.smartledger.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import com.smartledger.app.R
import com.smartledger.app.data.database.ExpenseEntity
import com.smartledger.app.data.repository.ExpenseRepository
import com.smartledger.app.detector.PaymentDetector
import kotlinx.coroutines.*

/**
 * 悬浮窗服务 — 在检测到付款/收款页面后弹出快速记账窗口
 *
 * 自动填入金额、分类，用户可选择支出/收入，修改分类后保存。
 */
class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private val repository = ExpenseRepository()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var detectedAmount: Double? = null
    private var sourceApp: String? = null
    private var sourcePackage: String? = null
    private var suggestedCategory: String = "其他"
    private var suggestedType: String = "expense"  // "expense" | "income"
    private var selectedType: String = "expense"
    private var selectedCategory: String = "其他"

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                detectedAmount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
                sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP) ?: "未知"
                sourcePackage = intent.getStringExtra(EXTRA_SOURCE_PACKAGE)
                suggestedCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: "其他"
                suggestedType = intent.getStringExtra(EXTRA_TYPE) ?: "expense"

                selectedCategory = suggestedCategory
                selectedType = suggestedType

                showFloatingWindow()
            }
            ACTION_HIDE -> {
                hideFloatingWindow()
            }
        }
        return START_STICKY
    }

    private fun showFloatingWindow() {
        hideFloatingWindow()

        val amount = detectedAmount ?: return

        try {
            floatingView = LayoutInflater.from(this).inflate(
                R.layout.floating_expense_window, null
            )

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 120
            }

            setupFloatingUI(amount)

            floatingView?.apply {
                alpha = 0f
                translationY = -100f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }

            windowManager.addView(floatingView, layoutParams)
            Log.d(TAG, "悬浮窗已显示")
        } catch (e: Exception) {
            Log.e(TAG, "显示悬浮窗失败", e)
            hideFloatingWindow()
        }
    }

    private fun setupFloatingUI(amount: Double) {
        val view = floatingView ?: return

        val tvSource: TextView = view.findViewById(R.id.tv_source)
        val etAmount: EditText = view.findViewById(R.id.et_amount)
        val spinnerCategory: Spinner = view.findViewById(R.id.spinner_category)
        val etNote: EditText = view.findViewById(R.id.et_note)
        val btnSave: Button = view.findViewById(R.id.btn_save)
        val btnDismiss: Button = view.findViewById(R.id.btn_dismiss)
        val toggleType: ToggleButton = view.findViewById(R.id.toggle_type)

        // 标题：显示来源和类型
        val typeLabel = if (selectedType == "income") "收款" else "付款"
        tvSource.text = "检测到来自「${sourceApp}」的${typeLabel}"

        etAmount.setText(String.format("%.2f", amount))
        etAmount.setSelection(etAmount.text.length)

        // 收入/支出切换按钮
        toggleType.apply {
            textOn = "💰 收入"
            textOff = "💸 支出"
            isChecked = selectedType == "income"
            setOnCheckedChangeListener { _, isChecked ->
                selectedType = if (isChecked) "income" else "expense"
                tvSource.text = "检测到来自「${sourceApp}」的${if (isChecked) "收款" else "付款"}"
                updateSpinner()
            }
        }

        // 分类选择器（包含收入分类）
        val expenseCategories = listOf(
            "餐饮", "交通", "购物", "娱乐", "住房",
            "水电", "医疗", "教育", "转账", "其他"
        )
        val incomeCategories = listOf(
            "工资", "奖金", "兼职", "退款", "理财", "报销", "红包", "其他收入"
        )

        fun updateSpinner() {
            val cats = if (selectedType == "income") incomeCategories else expenseCategories
            val adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, cats
            )
            spinnerCategory.adapter = adapter

            // 找到最匹配的分类索引
            val idx = cats.indexOfFirst { it == selectedCategory || it.contains(selectedCategory) }
            if (idx >= 0) {
                spinnerCategory.setSelection(idx)
                selectedCategory = cats[idx]
            } else {
                selectedCategory = cats[0]
            }
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedCategory = (spinnerCategory.adapter.getItem(pos) as String)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateSpinner()

        // 保存按钮
        btnSave.setOnClickListener {
            val inputAmount = etAmount.text.toString().toDoubleOrNull()
            if (inputAmount == null || inputAmount <= 0) {
                Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveExpense(inputAmount, etNote.text.toString())

            floatingView?.animate()
                ?.alpha(0f)
                ?.scaleX(0.8f)
                ?.scaleY(0.8f)
                ?.setDuration(200)
                ?.withEndAction { hideFloatingWindow() }
                ?.start()
        }

        // 忽略按钮
        btnDismiss.setOnClickListener {
            floatingView?.animate()
                ?.alpha(0f)
                ?.translationY(-100f)
                ?.setDuration(200)
                ?.withEndAction { hideFloatingWindow() }
                ?.start()
        }
    }

    private fun saveExpense(amount: Double, note: String) {
        scope.launch {
            val amountInCents = (amount * 100).toLong()
            val expense = ExpenseEntity(
                amount = amountInCents,
                category = selectedCategory,
                type = selectedType,
                sourcePackage = sourcePackage,
                sourceApp = sourceApp,
                note = note.ifBlank { null }
            )
            repository.insert(expense)
            PaymentDetector.resetCache()
        }
    }

    private fun hideFloatingWindow() {
        try {
            floatingView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        floatingView = null
        stopSelf()
    }

    private fun startForegroundNotification() {
        val channelId = "floating_window"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "智能记账监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于维持悬浮窗服务运行"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("智能记账")
            .setContentText("正在监控付款页面…")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        hideFloatingWindow()
    }

    companion object {
        private const val TAG = "FloatingWindow"
        const val ACTION_SHOW = "com.smartledger.SHOW_FLOATING"
        const val ACTION_HIDE = "com.smartledger.HIDE_FLOATING"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_SOURCE_APP = "source_app"
        const val EXTRA_SOURCE_PACKAGE = "source_package"
        const val EXTRA_CONFIDENCE = "confidence"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_TYPE = "type"
        private const val NOTIFICATION_ID = 1001
    }
}
