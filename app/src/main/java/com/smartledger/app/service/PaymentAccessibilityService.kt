package com.smartledger.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.smartledger.app.detector.PaymentDetector

/**
 * 无障碍服务 — 在后台监控屏幕内容，检测支付成功页面
 *
 * 核心策略：使用 findAccessibilityNodeInfosByText 直接搜索节点树中的「支付成功」等关键词，
 * 这比遍历采集文本可靠得多，尤其对微信/支付宝等使用自定义渲染引擎的 App。
 *
 * 同时配合延迟二次检测，应对页面内容异步加载的情况。
 */
class PaymentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PaymentA11y"
        var isRunning = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastCheckedText = ""
    private var lastCheckedTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceEnabled = true
        isRunning = true
        Log.d(TAG, "=== 无障碍服务已连接 ===")

        val info = AccessibilityServiceInfo().apply {
            // 监听所有可能的事件类型
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY

            // 800ms 防抖 — 给页面内容加载留足够时间
            notificationTimeout = 800
        }
        serviceInfo = info

        Toast.makeText(this, "✅ 智能记账监控已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        val packageName = event.packageName?.toString() ?: return

        // 忽略自己的App
        if (packageName == "com.smartledger.app") return

        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "[窗口切换] $packageName")
                // 窗口切换时立即检测 + 延迟检测（等页面内容加载完）
                checkForPayment(event, packageName)
                scheduleDelayedCheck(event, packageName, delayMs = 600)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 内容变化时检测（但不延迟，因为窗口事件已经设了延迟）
                checkForPayment(event, packageName)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // 文字变化也检测（比如支付金额动态显示）
                val text = event.text?.joinToString(" ") ?: ""
                if (text.length > 2) {
                    Log.d(TAG, "[文字变化] $packageName | $text")
                    checkForPayment(event, packageName)
                }
            }
        }
    }

    /**
     * 核心检测方法
     *
     * 策略1（优先）：直接用 findAccessibilityNodeInfosByText 搜索关键词
     *    — 这是最可靠的方式，不管微信/支付宝用什么渲染引擎，只要无障碍文本存在就能找到
     *
     * 策略2（备用）：采集屏幕文本后用 PaymentDetector 分析
     *    — 用于提取金额和分类
     */
    private fun checkForPayment(event: AccessibilityEvent, packageName: String) {
        // 防重：相同包 3 秒内不重复检测文本收集
        val now = System.currentTimeMillis()
        if (now - lastCheckedTime < 3000 && packageName != "com.tencent.mm") {
            // 微信经常频繁触发事件，使用更短的防重时间
            if (now - lastCheckedTime < 500) return
        }

        try {
            // ════════════════════════════════════════════════
            //  策略1：直接搜索关键词节点（最可靠的方法！）
            // ════════════════════════════════════════════════
            val successKeywords = listOf(
                "支付成功", "付款成功", "交易成功", "支付完成",
                "收款成功", "已收款", "收款到账",
                "充值成功", "交易完成"
            )

            var foundKeyword: String? = null
            val rootNode = getRootNode(event)

            if (rootNode != null) {
                try {
                    for (keyword in successKeywords) {
                        val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
                        if (nodes.isNotEmpty()) {
                            foundKeyword = keyword
                            Log.d(TAG, "✅ [策略1命中] 找到关键词: $keyword (节点数: ${nodes.size})")
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "findAccessibilityNodeInfosByText 异常", e)
                } finally {
                    try { rootNode.recycle() } catch (_: Exception) {}
                }
            }

            // ════════════════════════════════════════════════
            //  策略2：采集所有文本（用于提取金额和分类）
            // ════════════════════════════════════════════════
            var screenText = ""
            if (foundKeyword != null) {
                // 找到了关键词，再采集文本提取金额
                val rootNode2 = getRootNode(event)
                if (rootNode2 != null) {
                    try {
                        screenText = collectScreenText(rootNode2)
                        Log.d(TAG, "采集到文本: ${screenText.take(200)}")
                    } catch (e: Exception) {
                        Log.e(TAG, "文本采集异常", e)
                    } finally {
                        try { rootNode2.recycle() } catch (_: Exception) {}
                    }
                }
            }

            // 如果没有用策略1找到关键词，尝试用策略2检测
            if (foundKeyword == null) {
                val rootNode3 = getRootNode(event)
                if (rootNode3 != null) {
                    try {
                        screenText = collectScreenText(rootNode3)
                        if (screenText.length > 5) {
                            val found = successKeywords.any { screenText.contains(it) }
                            if (found) {
                                foundKeyword = successKeywords.first { screenText.contains(it) }
                                Log.d(TAG, "✅ [策略2命中] 文本中找到: $foundKeyword")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "策略2采集异常", e)
                    } finally {
                        try { rootNode3.recycle() } catch (_: Exception) {}
                    }
                }
            }

            // 没检测到支付页面
            if (foundKeyword == null) {
                if (screenText.length > 10) {
                    Log.d(TAG, "未检测到支付关键词 | 包: $packageName | 文本片段: ${screenText.take(80)}")
                }
                return
            }

            // ════════════════════════════════════════════════
            //  检测到了！开始分析
            // ════════════════════════════════════════════════
            lastCheckedTime = now
            lastCheckedText = screenText

            Log.d(TAG, """
                ╔══════════════════════════════════════╗
                ║ ✅ 检测到支付页面！
                ║ 关键词: $foundKeyword
                ║ 来源包: $packageName
                ║ 文本长度: ${screenText.length}
                ╚══════════════════════════════════════╝
            """.trimIndent())

            // 检测
            val detection = PaymentDetector.detect(screenText, packageName)

            if (detection.isPaymentPage) {
                Log.d(TAG, """
                    ╔══════════════════════════════════════╗
                    ║ ✅ 确认为支付成功页面
                    ║ 来源: ${detection.sourceApp}
                    ║ 金额: ${detection.amount}
                    ║ 类型: ${detection.suggestedType}
                    ║ 分类: ${detection.suggestedCategory}
                    ║ 置信度: ${detection.confidence}
                    ╚══════════════════════════════════════╝
                """.trimIndent())

                showFloatingWindow(detection)
            }

        } catch (e: Exception) {
            Log.e(TAG, "checkForPayment 异常", e)
        }
    }

    /**
     * 获取 rootNode，兼容 rootInActiveWindow 为 null 的情况
     */
    private fun getRootNode(event: AccessibilityEvent): AccessibilityNodeInfo? {
        // 方法1：从 Service 获取
        rootInActiveWindow?.let { return it }

        // 方法2：从事件获取
        event.source?.let { return it }

        return null
    }

    /**
     * 延迟检测 — 等页面内容加载完再查一次
     */
    private fun scheduleDelayedCheck(event: AccessibilityEvent, packageName: String, delayMs: Long) {
        handler.removeCallbacksAndMessages(null)

        // 保存事件的关键信息，延迟后重新检测
        handler.postDelayed({
            Log.d(TAG, "[延迟检测] $packageName (${delayMs}ms后)")
            checkForPaymentDelayed(packageName)
        }, delayMs)
    }

    /**
     * 延迟检测时没有 AccessibilityEvent，构造一个简化版的检测
     */
    private fun checkForPaymentDelayed(packageName: String) {
        try {
            val rootNode = rootInActiveWindow ?: return

            val successKeywords = listOf(
                "支付成功", "付款成功", "交易成功", "支付完成",
                "收款成功", "已收款", "收款到账",
                "充值成功", "交易完成"
            )

            var foundKeyword: String? = null
            var screenText = ""

            try {
                // 直接搜索关键词
                for (keyword in successKeywords) {
                    val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
                    if (nodes.isNotEmpty()) {
                        foundKeyword = keyword
                        Log.d(TAG, "✅ [延迟命中] 找到关键词: $keyword")
                        break
                    }
                }

                // 采集文本
                if (foundKeyword != null) {
                    screenText = collectScreenText(rootNode)
                }
            } finally {
                try { rootNode.recycle() } catch (_: Exception) {}
            }

            if (foundKeyword != null && screenText.isNotBlank()) {
                val detection = PaymentDetector.detect(screenText, packageName)
                if (detection.isPaymentPage) {
                    Log.d(TAG, "✅ [延迟检测] 弹窗!")
                    showFloatingWindow(detection)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "延迟检测异常", e)
        }
    }

    /**
     * 递归收集屏幕上的所有可读文本
     * 增强版：支持更多类型的内容提取
     */
    private fun collectScreenText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()

        fun traverse(n: AccessibilityNodeInfo, depth: Int) {
            if (depth > 30) return // 防止无限递归

            // 提取 text
            n.text?.toString()?.let { text ->
                if (text.isNotBlank()) {
                    sb.append(text).append(" ")
                }
            }

            // 提取 contentDescription
            n.contentDescription?.toString()?.let { desc ->
                if (desc.isNotBlank()) {
                    sb.append(desc).append(" ")
                }
            }

            // 提取 hint（输入框提示文字）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                n.hintText?.toString()?.let { hint ->
                    if (hint.isNotBlank()) {
                        sb.append(hint).append(" ")
                    }
                }
            }

            // 遍历子节点
            for (i in 0 until n.childCount) {
                val child = n.getChild(i) ?: continue
                try {
                    traverse(child, depth + 1)
                } catch (e: Exception) {
                    Log.w(TAG, "遍历子节点异常 depth=$depth", e)
                } finally {
                    try { child.recycle() } catch (_: Exception) {}
                }
            }
        }

        try {
            traverse(node, 0)
        } catch (e: Exception) {
            Log.e(TAG, "collectScreenText 异常", e)
        }

        return sb.toString().trim()
    }

    private fun showFloatingWindow(detection: com.smartledger.app.detector.PaymentDetection) {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "⚠️ 悬浮窗权限未开启！")
            Toast.makeText(this, "请在智能记账中开启悬浮窗权限", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, ">>> 启动悬浮窗服务: 金额=${detection.amount}, 来源=${detection.sourceApp}")

        val intent = Intent(this, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_SHOW
            putExtra(FloatingWindowService.EXTRA_AMOUNT, detection.amount)
            putExtra(FloatingWindowService.EXTRA_SOURCE_APP, detection.sourceApp)
            putExtra(FloatingWindowService.EXTRA_SOURCE_PACKAGE, detection.sourcePackage)
            putExtra(FloatingWindowService.EXTRA_CONFIDENCE, detection.confidence)
            putExtra(FloatingWindowService.EXTRA_CATEGORY, detection.suggestedCategory)
            putExtra(FloatingWindowService.EXTRA_TYPE, detection.suggestedType)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "✅ 悬浮窗服务启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 悬浮窗服务启动失败", e)
            Toast.makeText(this, "启动悬浮窗失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        isServiceEnabled = false
        isRunning = false
        Log.d(TAG, "=== 无障碍服务已销毁 ===")
    }

    private var isServiceEnabled = false
}
