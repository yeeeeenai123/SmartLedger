package com.smartledger.app.detector

import android.util.Log

/**
 * 付款检测结果
 */
data class PaymentDetection(
    val isPaymentPage: Boolean,
    val amount: Double?,
    val sourceApp: String,
    val sourcePackage: String,
    val confidence: Float,          // 0.0 ~ 1.0
    val suggestedCategory: String,  // 自动识别的分类
    val suggestedType: String       // "expense" 支出 或 "income" 收入
)

/**
 * 付款检测引擎
 *
 * 核心逻辑：检测到「支付成功 / 付款成功 / 交易成功」后，自动弹出记账悬浮窗。
 * 覆盖：微信、支付宝、银行App、抖音、美团、京东、淘宝等
 */
object PaymentDetector {

    // ──── 各App的包名 ────
    private val APP_NAMES = mapOf(
        "com.tencent.mm" to "微信",
        "com.eg.android.AlipayGphone" to "支付宝",
        "com.ss.android.ugc.aweme" to "抖音",
        "com.sankuai.meituan" to "美团",
        "com.jingdong.app.mall" to "京东",
        "com.taobao.taobao" to "淘宝",
        "com.icbc" to "工商银行",
        "com.chinamworld.boc" to "中国银行",
        "com.chinamworld.bocmbci" to "中国银行",
        "com.android.bankabc" to "农业银行",
        "com.chinamworld.main" to "建设银行",
        "cmb.pb" to "招商银行",
        "com.bankcomm.Bankcomm" to "交通银行",
        "com.spdb.mobilebank" to "浦发银行",
        "com.cmbc.mbank" to "民生银行",
        "com.cib.finance" to "兴业银行",
        "com.pingan.papd" to "平安银行",
        "cn.gov.pbc.dcep" to "数字人民币"
    )

    // ═══════════════════════════════════════════════════
    //  核心：支付成功关键词 — 检测到这些就弹窗！
    // ═══════════════════════════════════════════════════
    private val PAYMENT_SUCCESS_KEYWORDS = listOf(
        "支付成功", "付款成功", "交易成功", "支付完成",
        "支付成功!", "付款成功!", "交易成功!",
        "充值成功", "交易完成", "支付完成!",
        "支付成功页", "付款成功页",
        "支付成功后", "交易成功后",
        "支付成功，", "付款成功，",
        "交易成功，", "支付完成，",
        "支付成功！", "付款成功！", "交易成功！",
        "Payment Successful", "Payment Success",
        "支付成功(自动扣款)"
    )

    // ──── 收款/收入关键词（检测到也弹窗，类型为收入） ────
    private val INCOME_SUCCESS_KEYWORDS = listOf(
        "收款成功", "已收款", "收款到账",
        "转账给你", "向你转账", "已转账",
        "收到转账", "已到账", "转账成功",
        "对方已转账", "你领取了", "已存入",
        "转入成功", "充值到账", "工资到账", "退款到账",
        "红包", "收款成功！"
    )

    // ──── 排除关键词（这些页面不弹窗） ────
    // 注意：支付成功、付款成功、交易成功 不在这里！它们是触发词
    private val EXCLUDE_KEYWORDS = listOf(
        "订单详情", "交易记录", "账单",
        "支付记录", "订单列表", "支付设置",
        "免密支付设置", "自动扣款设置",
        "交易关闭", "交易失败", "支付失败",
        "待发货", "已发货", "申请退货",
        "退款详情", "售后", "退款处理中"
    )

    // ──── 自动分类规则：来源App → 默认分类 ────
    private val APP_CATEGORY_MAP = mapOf(
        "com.sankuai.meituan" to "餐饮",
        "com.jingdong.app.mall" to "购物",
        "com.taobao.taobao" to "购物",
        "com.ss.android.ugc.aweme" to "娱乐",
        "com.tencent.mm" to "餐饮"
    )

    // ──── 自动分类规则：页面关键词 → 分类 ────
    private val KEYWORD_CATEGORY_MAP = mapOf(
        "外卖" to "餐饮", "餐厅" to "餐饮", "饭店" to "餐饮",
        "美食" to "餐饮", "午餐" to "餐饮", "晚餐" to "餐饮",
        "早餐" to "餐饮", "咖啡" to "餐饮", "奶茶" to "餐饮",
        "小吃" to "餐饮", "买菜" to "餐饮", "超市" to "餐饮",
        "打车" to "交通", "滴滴" to "交通", "地铁" to "交通",
        "公交" to "交通", "火车" to "交通", "高铁" to "交通",
        "机票" to "交通", "加油" to "交通", "停车" to "交通",
        "骑行" to "交通", "单车" to "交通",
        "下单" to "购物", "商品" to "购物", "订单" to "购物",
        "购买" to "购物", "购物车" to "购物", "快递" to "购物",
        "包邮" to "购物", "天猫" to "购物",
        "电影" to "娱乐", "演出" to "娱乐", "KTV" to "娱乐",
        "景区" to "娱乐", "门票" to "娱乐", "酒店" to "娱乐",
        "旅行" to "娱乐", "游戏" to "娱乐",
        "房租" to "住房", "物业" to "住房", "房贷" to "住房",
        "租房" to "住房",
        "电费" to "水电", "水费" to "水电", "燃气" to "水电",
        "话费" to "水电", "宽带" to "水电", "网费" to "水电",
        "医院" to "医疗", "药" to "医疗", "挂号" to "医疗",
        "门诊" to "医疗", "体检" to "医疗",
        "学费" to "教育", "课程" to "教育", "培训" to "教育",
        "学习" to "教育", "考试" to "教育",
        "转账" to "转账", "汇款" to "转账", "提现" to "转账"
    )

    // ──── 金额提取正则 ────
    private val AMOUNT_PATTERNS = listOf(
        Regex("""[¥￥]\s*(\d+\.?\d{0,2})"""),
        Regex("""(\d+\.?\d{0,2})\s*元"""),
        Regex("""[¥￥]\s*([\d,]+\.?\d{0,2})"""),
        Regex("""(?:应付|实付|合计|订单|付款|支付|收款|到账|收入|金额|总计|实付金额)\s*[:：]?\s*[¥￥]?\s*(\d+\.?\d{0,2})"""),
        Regex("""[-−]\s*[¥￥]?\s*(\d+\.?\d{0,2})"""),
        Regex("""需支付\s*[¥￥]?\s*(\d+\.?\d{0,2})"""),
        Regex("""(\d+\.?\d{0,2})\s*元?\s*(?:已)?(?:到账|存入|入账)""")
    )

    // 上次检测结果缓存 — 防止同一个成功页面反复弹窗
    private var lastDetectionTime = 0L
    private var lastDetectionPackage = ""
    private var lastDetectedAmount = 0.0
    private val MIN_INTERVAL_MS = 5000L // 同一个成功页面5秒内不重复弹窗

    /**
     * 检测当前屏幕是否为支付成功 / 收款成功页面
     *
     * 核心逻辑（修复版）：
     * 1. 先检测「支付成功/收款成功」关键词（最高优先级！）
     * 2. 排除关键词只在没有成功关键词时才起作用
     * 3. 防重已移到 Service 层，这里做最后防线
     */
    fun detect(screenText: String, packageName: String): PaymentDetection {
        val now = System.currentTimeMillis()

        // 忽略自己的App
        if (packageName == "com.smartledger.app") {
            return noDetection(packageName)
        }

        // ═══════════════════════════════════════════════════
        //  第1优先级：检测收款/收入成功
        // ═══════════════════════════════════════════════════
        val isIncome = INCOME_SUCCESS_KEYWORDS.any { screenText.contains(it) }
        if (isIncome) {
            // 确认不在排除页面（收款成功页面一般不会同时有排除词）
            val hasExcludeWord = EXCLUDE_KEYWORDS.any { screenText.contains(it) }
            if (hasExcludeWord) {
                Log.d("PaymentDetector", "收款关键词命中但页面包含排除词，跳过")
                return noDetection(packageName)
            }

            val amount = extractAmount(screenText)
            val finalAmount = amount ?: 0.0
            updateCache(now, packageName, finalAmount)
            Log.d("PaymentDetector", "✅ 收款成功！金额=$finalAmount, App=${getAppName(packageName)}")
            return PaymentDetection(
                isPaymentPage = true,
                amount = finalAmount,
                sourceApp = getAppName(packageName),
                sourcePackage = packageName,
                confidence = 0.95f,
                suggestedCategory = suggestCategory(packageName, screenText, isIncome = true),
                suggestedType = "income"
            )
        }

        // ═══════════════════════════════════════════════════
        //  第2优先级：检测支付/付款成功（核心功能！）
        //  重要：先检测成功关键词，再检查排除词
        //  因为支付成功页经常也会提到「订单详情」等词
        // ═══════════════════════════════════════════════════
        val isPaymentSuccess = PAYMENT_SUCCESS_KEYWORDS.any { screenText.contains(it) }
        if (isPaymentSuccess) {
            // 检查排除词 — 但只在成功关键词是「弱匹配」时排除
            // 「支付成功」「付款成功」这种核心词命中时，忽略排除词
            val isStrongMatch = listOf("支付成功", "付款成功", "交易成功", "支付完成")
                .any { screenText.contains(it) }

            if (!isStrongMatch) {
                // 弱匹配（如「充值成功」「支付成功后」）才检查排除词
                val hasExcludeWord = EXCLUDE_KEYWORDS.any { screenText.contains(it) }
                if (hasExcludeWord) {
                    Log.d("PaymentDetector", "弱匹配+排除词命中，跳过")
                    return noDetection(packageName)
                }
            }

            // 防重确认
            if (packageName == lastDetectionPackage &&
                now - lastDetectionTime < MIN_INTERVAL_MS) {
                Log.d("PaymentDetector", "防重: 5秒内同一包名已检测过")
                return noDetection(packageName)
            }

            val amount = extractAmount(screenText)
            val finalAmount = if (amount != null && amount > 0.0) amount else 0.0
            updateCache(now, packageName, finalAmount)
            Log.d("PaymentDetector", "✅ 支付成功！金额=$finalAmount, App=${getAppName(packageName)}")
            return PaymentDetection(
                isPaymentPage = true,
                amount = finalAmount,
                sourceApp = getAppName(packageName),
                sourcePackage = packageName,
                confidence = 0.95f,
                suggestedCategory = suggestCategory(packageName, screenText, isIncome = false),
                suggestedType = "expense"
            )
        }

        return noDetection(packageName)
    }

    /**
     * 自动分类：根据来源App + 页面关键词智能判断
     */
    fun suggestCategory(packageName: String, screenText: String, isIncome: Boolean): String {
        if (isIncome) {
            // 收入场景细分
            return when {
                screenText.contains("工资") || screenText.contains("薪水") -> "工资"
                screenText.contains("红包") -> "红包"
                screenText.contains("退款") -> "退款"
                screenText.contains("理财") || screenText.contains("利息") -> "理财"
                screenText.contains("报销") -> "报销"
                else -> "其他收入"
            }
        }

        // 1. 先检查页面关键词（优先级最高）
        for ((keyword, category) in KEYWORD_CATEGORY_MAP) {
            if (screenText.contains(keyword)) return category
        }

        // 2. 根据App包名判断
        APP_CATEGORY_MAP[packageName]?.let { return it }

        // 3. 银行类App → 转账
        if (packageName in APP_NAMES && !APP_CATEGORY_MAP.containsKey(packageName)) {
            return "转账"
        }

        // 4. 支付宝特殊处理
        if (packageName == "com.eg.android.AlipayGphone") {
            return "购物"
        }

        return "其他"
    }

    /**
     * 从屏幕文本中提取金额
     */
    fun extractAmount(text: String): Double? {
        val candidates = mutableListOf<Double>()

        for (pattern in AMOUNT_PATTERNS) {
            pattern.findAll(text).forEach { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0.01) {
                    candidates.add(amount)
                }
            }
        }

        if (candidates.isEmpty()) return null

        val sorted = candidates.sorted()
        return when {
            sorted.size == 1 -> sorted[0]
            sorted.size == 2 -> sorted.min()
            else -> {
                val filtered = sorted.filter { it >= sorted.min() * 0.5 }
                if (filtered.size >= 2) filtered.min() else sorted.min()
            }
        }
    }

    fun getAppName(packageName: String): String {
        return APP_NAMES[packageName] ?: packageName.substringAfterLast(".")
    }

    private fun noDetection(packageName: String) = PaymentDetection(
        isPaymentPage = false,
        amount = null,
        sourceApp = getAppName(packageName),
        sourcePackage = packageName,
        confidence = 0f,
        suggestedCategory = "其他",
        suggestedType = "expense"
    )

    private fun updateCache(time: Long, pkg: String, amount: Double) {
        lastDetectionTime = time
        lastDetectionPackage = pkg
        lastDetectedAmount = amount
    }

    fun resetCache() {
        lastDetectionTime = 0L
        lastDetectionPackage = ""
        lastDetectedAmount = 0.0
    }
}
