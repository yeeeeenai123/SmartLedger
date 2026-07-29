package com.smartledger.app.detector

/**
 * 付款页面检测结果
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
 * 付款页面检测引擎
 *
 * 通过无障碍服务获取的屏幕文本内容，识别是否为付款/收款页面，提取金额并自动分类。
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

    // ──── 付款页面关键词（高置信度） ────
    private val PAYMENT_KEYWORDS_HIGH = listOf(
        "确认支付", "立即支付", "确认付款", "付款确认",
        "请输入支付密码", "支付密码", "指纹支付", "面容支付",
        "刷脸支付", "验证支付密码", "Touch ID",
        "确认交易", "交易确认"
    )

    // ──── 付款页面关键词（中等置信度） ────
    private val PAYMENT_KEYWORDS_MEDIUM = listOf(
        "支付", "付款", "收银台", "确认订单",
        "提交订单", "去支付", "立即付款",
        "应付金额", "实付金额", "合计金额",
        "付款金额", "订单金额", "需付款",
        "支付方式", "选择支付方式", "付款方式",
        "微信支付", "支付宝支付", "银行卡支付", "云闪付",
        "确认下单", "去结算", "结算"
    )

    // ──── 收款/收入关键词 ────
    private val INCOME_KEYWORDS = listOf(
        "收款", "转账给你", "向你转账", "已转账",
        "收到转账", "到账", "已到账", "转账成功",
        "对方已转账", "红包", "你领取了", "已存入",
        "转入成功", "充值到账", "工资到账", "退款到账",
        "收款成功", "已收款"
    )

    // ──── 非付款页面排除关键词 ────
    private val EXCLUDE_KEYWORDS = listOf(
        "充值成功", "支付成功", "付款成功", "交易成功",
        "订单详情", "已支付", "交易记录", "账单",
        "支付记录", "已完成", "待发货", "已发货",
        "退款", "售后", "申请退货", "交易关闭",
        "订单列表", "支付设置", "免密支付设置"
    )

    // ──── 自动分类规则：来源App → 默认分类 ────
    private val APP_CATEGORY_MAP = mapOf(
        "com.sankuai.meituan" to "餐饮",
        "com.jingdong.app.mall" to "购物",
        "com.taobao.taobao" to "购物",
        "com.ss.android.ugc.aweme" to "娱乐",
        "com.tencent.mm" to "餐饮"       // 微信太难判断，默认餐饮
    )

    // ──── 自动分类规则：页面关键词 → 分类 ────
    private val KEYWORD_CATEGORY_MAP = mapOf(
        // 餐饮
        "外卖" to "餐饮", "餐厅" to "餐饮", "饭店" to "餐饮",
        "美食" to "餐饮", "午餐" to "餐饮", "晚餐" to "餐饮",
        "早餐" to "餐饮", "咖啡" to "餐饮", "奶茶" to "餐饮",
        "小吃" to "餐饮", "买菜" to "餐饮", "超市" to "餐饮",

        // 交通
        "打车" to "交通", "滴滴" to "交通", "地铁" to "交通",
        "公交" to "交通", "火车" to "交通", "高铁" to "交通",
        "机票" to "交通", "加油" to "交通", "停车" to "交通",
        "骑行" to "交通", "单车" to "交通",

        // 购物
        "下单" to "购物", "商品" to "购物", "订单" to "购物",
        "购买" to "购物", "购物车" to "购物", "快递" to "购物",
        "包邮" to "购物", "天猫" to "购物",

        // 娱乐
        "电影" to "娱乐", "演出" to "娱乐", "KTV" to "娱乐",
        "景区" to "娱乐", "门票" to "娱乐", "酒店" to "娱乐",
        "旅行" to "娱乐", "游戏" to "娱乐",

        // 住房
        "房租" to "住房", "物业" to "住房", "房贷" to "住房",
        "租房" to "住房",

        // 水电
        "电费" to "水电", "水费" to "水电", "燃气" to "水电",
        "话费" to "水电", "宽带" to "水电", "网费" to "水电",

        // 医疗
        "医院" to "医疗", "药" to "医疗", "挂号" to "医疗",
        "门诊" to "医疗", "体检" to "医疗",

        // 教育
        "学费" to "教育", "课程" to "教育", "培训" to "教育",
        "学习" to "教育", "考试" to "教育",

        // 转账/银行
        "转账" to "转账", "汇款" to "转账", "提现" to "转账"
    )

    // ──── 金额提取正则 ────
    private val AMOUNT_PATTERNS = listOf(
        Regex("""[¥￥]\s*(\d+\.?\d{0,2})"""),
        Regex("""(\d+\.?\d{0,2})\s*元"""),
        Regex("""[¥￥]\s*([\d,]+\.?\d{0,2})"""),
        Regex("""(?:应付|实付|合计|订单|付款|支付|收款|到账|收入)\s*(?:金额|总价|价格|总计)?\s*[:：]?\s*[¥￥]?\s*(\d+\.?\d{0,2})"""),
        Regex("""[-−]\s*[¥￥]?\s*(\d+\.?\d{0,2})"""),
        Regex("""需支付\s*[¥￥]?\s*(\d+\.?\d{0,2})"""),
        Regex("""(\d+\.?\d{0,2})\s*元?\s*(?:已)?(?:到账|存入|入账)""")
    )

    // 上次检测结果缓存
    private var lastDetectionTime = 0L
    private var lastDetectionPackage = ""
    private val MIN_INTERVAL_MS = 3000L

    /**
     * 检测当前屏幕是否为付款/收款页面
     */
    fun detect(screenText: String, packageName: String): PaymentDetection {
        val now = System.currentTimeMillis()

        // 去重
        if (packageName == lastDetectionPackage &&
            now - lastDetectionTime < MIN_INTERVAL_MS
        ) {
            return noDetection(packageName)
        }

        if (packageName == "com.smartledger.app") {
            return noDetection(packageName)
        }

        // 排除非付款页面
        if (EXCLUDE_KEYWORDS.any { screenText.contains(it) }) {
            return noDetection(packageName)
        }

        // ── 先判断是支出还是收入 ──
        val isIncome = INCOME_KEYWORDS.any { screenText.contains(it) }
        val type = if (isIncome) "income" else "expense"

        // 高置信度检测
        val highConfidenceMatch = PAYMENT_KEYWORDS_HIGH.any { screenText.contains(it) }
        if (highConfidenceMatch || isIncome) {
            val amount = extractAmount(screenText)
            if (amount != null) {
                updateCache(now, packageName)
                return PaymentDetection(
                    isPaymentPage = true,
                    amount = amount,
                    sourceApp = getAppName(packageName),
                    sourcePackage = packageName,
                    confidence = if (isIncome) 0.9f else 0.95f,
                    suggestedCategory = suggestCategory(packageName, screenText, isIncome),
                    suggestedType = type
                )
            }
        }

        // 中等置信度
        val mediumMatches = PAYMENT_KEYWORDS_MEDIUM.count { screenText.contains(it) }
        if (mediumMatches >= 2) {
            val amount = extractAmount(screenText)
            if (amount != null) {
                updateCache(now, packageName)
                return PaymentDetection(
                    isPaymentPage = true,
                    amount = amount,
                    sourceApp = getAppName(packageName),
                    sourcePackage = packageName,
                    confidence = 0.8f,
                    suggestedCategory = suggestCategory(packageName, screenText, isIncome),
                    suggestedType = type
                )
            }
        }

        // 低置信度
        if (mediumMatches >= 1) {
            val amount = extractAmount(screenText)
            if (amount != null && amount > 0) {
                updateCache(now, packageName)
                return PaymentDetection(
                    isPaymentPage = true,
                    amount = amount,
                    sourceApp = getAppName(packageName),
                    sourcePackage = packageName,
                    confidence = 0.6f,
                    suggestedCategory = suggestCategory(packageName, screenText, isIncome),
                    suggestedType = type
                )
            }
        }

        return noDetection(packageName)
    }

    /**
     * 自动分类：根据来源App + 页面关键词智能判断
     */
    fun suggestCategory(packageName: String, screenText: String, isIncome: Boolean): String {
        if (isIncome) return "收入"

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

        // 4. 支付宝特殊处理（看有没有外卖、电影等关键词）
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

    private fun updateCache(time: Long, pkg: String) {
        lastDetectionTime = time
        lastDetectionPackage = pkg
    }

    fun resetCache() {
        lastDetectionTime = 0L
        lastDetectionPackage = ""
    }
}
