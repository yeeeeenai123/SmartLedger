package com.smartledger.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.smartledger.app.detector.PaymentDetector

/**
 * 无障碍服务 — 在后台监控屏幕内容，检测付款页面
 *
 * 当检测到付款页面时，启动悬浮窗服务弹出记账界面。
 */
class PaymentAccessibilityService : AccessibilityService() {

    private var isServiceEnabled = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceEnabled = true
        Log.d(TAG, "无障碍服务已连接")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 500 // 500ms 防抖
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 窗口内容变化时检测
                handleWindowChange(event)
            }
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return

        try {
            // 收集屏幕上的所有可见文本
            val screenText = collectScreenText(rootNode)

            if (screenText.isBlank()) return

            // 检测是否为付款页面
            val detection = PaymentDetector.detect(screenText, packageName)

            if (detection.isPaymentPage) {
                Log.d(TAG, "检测到付款页面: ${detection.sourceApp}, 金额: ${detection.amount}")

                // 启动悬浮窗
                showFloatingWindow(detection)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理窗口变化异常", e)
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 递归收集屏幕上的所有可读文本
     */
    private fun collectScreenText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()

        fun traverse(n: AccessibilityNodeInfo) {
            // 收集文本
            n.text?.toString()?.let { text ->
                if (text.isNotBlank()) {
                    sb.append(text).append(" ")
                }
            }
            // 收集 contentDescription
            n.contentDescription?.toString()?.let { desc ->
                if (desc.isNotBlank()) {
                    sb.append(desc).append(" ")
                }
            }

            // 递归遍历子节点
            for (i in 0 until n.childCount) {
                val child = n.getChild(i) ?: continue
                traverse(child)
                child.recycle()
            }
        }

        traverse(node)
        return sb.toString().trim()
    }

    private fun showFloatingWindow(detection: com.smartledger.app.detector.PaymentDetection) {
        val intent = Intent(this, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_SHOW
            putExtra(FloatingWindowService.EXTRA_AMOUNT, detection.amount)
            putExtra(FloatingWindowService.EXTRA_SOURCE_APP, detection.sourceApp)
            putExtra(FloatingWindowService.EXTRA_SOURCE_PACKAGE, detection.sourcePackage)
            putExtra(FloatingWindowService.EXTRA_CONFIDENCE, detection.confidence)
            putExtra(FloatingWindowService.EXTRA_CATEGORY, detection.suggestedCategory)
            putExtra(FloatingWindowService.EXTRA_TYPE, detection.suggestedType)
        }
        startService(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceEnabled = false
        Log.d(TAG, "无障碍服务已销毁")
    }

    companion object {
        private const val TAG = "PaymentA11y"

        var isRunning = false
            private set
    }
}
