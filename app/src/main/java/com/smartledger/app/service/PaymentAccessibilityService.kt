package com.smartledger.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.smartledger.app.detector.PaymentDetector

/**
 * 无障碍服务 — 在后台监控屏幕内容，检测支付成功页面
 *
 * 当检测到「支付成功 / 付款成功 / 收款到账」时，自动弹出记账悬浮窗。
 */
class PaymentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PaymentA11y"
        var isRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceEnabled = true
        isRunning = true
        Log.d(TAG, "=== 无障碍服务已连接 ===")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 300 // 300ms 防抖，更快响应
        }
        serviceInfo = info

        Toast.makeText(this, "智能记账已开始监控", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowChange(event)
            }
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // 忽略自己的App
        if (packageName == "com.smartledger.app") return

        val rootNode = rootInActiveWindow ?: return

        try {
            val screenText = collectScreenText(rootNode)

            if (screenText.isBlank()) return

            Log.d(TAG, "扫描: $packageName | 文本长度: ${screenText.length} | 片段: ${screenText.take(100)}")

            // 检测是否为支付成功 / 收款成功页面
            val detection = PaymentDetector.detect(screenText, packageName)

            if (detection.isPaymentPage) {
                Log.d(TAG, """
                    ╔══════════════════════════════════════╗
                    ║ ✅ 检测到${if (detection.suggestedType == "income") "收款" else "支付"}成功！
                    ║ 来源: ${detection.sourceApp}
                    ║ 金额: ${detection.amount}
                    ║ 分类: ${detection.suggestedCategory}
                    ║ 置信度: ${detection.confidence}
                    ╚══════════════════════════════════════╝
                """.trimIndent())

                // 检查悬浮窗权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Log.w(TAG, "⚠️ 悬浮窗权限未开启！无法弹窗")
                    Toast.makeText(this, "请先在智能记账App中开启悬浮窗权限", Toast.LENGTH_LONG).show()
                    return
                }

                // 弹出悬浮窗
                showFloatingWindow(detection)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理窗口变化异常", e)
        } finally {
            try {
                rootNode.recycle()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * 递归收集屏幕上的所有可读文本
     */
    private fun collectScreenText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()

        fun traverse(n: AccessibilityNodeInfo) {
            n.text?.toString()?.let { text ->
                if (text.isNotBlank()) {
                    sb.append(text).append(" ")
                }
            }
            n.contentDescription?.toString()?.let { desc ->
                if (desc.isNotBlank()) {
                    sb.append(desc).append(" ")
                }
            }

            for (i in 0 until n.childCount) {
                val child = n.getChild(i) ?: continue
                traverse(child)
                try { child.recycle() } catch (e: Exception) {}
            }
        }

        traverse(node)
        return sb.toString().trim()
    }

    private fun showFloatingWindow(detection: com.smartledger.app.detector.PaymentDetection) {
        Log.d(TAG, ">>> 正在启动悬浮窗服务...")
        val intent = Intent(this, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_SHOW
            putExtra(FloatingWindowService.EXTRA_AMOUNT, detection.amount)
            putExtra(FloatingWindowService.EXTRA_SOURCE_APP, detection.sourceApp)
            putExtra(FloatingWindowService.EXTRA_SOURCE_PACKAGE, detection.sourcePackage)
            putExtra(FloatingWindowService.EXTRA_CONFIDENCE, detection.confidence)
            putExtra(FloatingWindowService.EXTRA_CATEGORY, detection.suggestedCategory)
            putExtra(FloatingWindowService.EXTRA_TYPE, detection.suggestedType)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d(TAG, ">>> 悬浮窗服务已启动")
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceEnabled = false
        isRunning = false
        Log.d(TAG, "=== 无障碍服务已销毁 ===")
    }

    private var isServiceEnabled = false
}
