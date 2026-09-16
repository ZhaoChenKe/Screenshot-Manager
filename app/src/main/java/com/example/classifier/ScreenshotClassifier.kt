package com.example.classifier

import java.util.regex.Pattern

data class ClassificationResult(
    val categoryId: String,
    val title: String,
    val summary: String,
    val tags: List<String>
)

object ScreenshotClassifier {

    private val SHOPPING_KEYWORDS = listOf(
        "京东", "淘宝", "天猫", "拼多多", "唯品会", "苏宁", "购物车", "实付款", "待付款", "加入购物车",
        "立即购买", "商品详情", "店铺", "包邮", "满减", "定金", "到手价", "现价", "原价", "促销",
        "折后", "旗舰店", "自营", "券后", "专营店", "销量", "好评率", "免运费"
    )

    private val CHAT_KEYWORDS = listOf(
        "微信", "qq", "撤回了一条消息", "对方正在输入", "发送", "按住说话", "语音通话", "视频通话",
        "聊天记录", "拍了拍", "已读", "[表情]", "[图片]", "[语音]", "表情包", "群聊", "发来一个红包"
    )

    private val EXPRESS_KEYWORDS = listOf(
        "顺丰", "中通", "圆通", "申通", "韵达", "极兔", "菜鸟", "取件码", "运单号", "快递单号",
        "已签收", "派送中", "丰巢", "快递超市", "驿站", "派件员", "包裹", "物流信息"
    )

    private val ORDER_KEYWORDS = listOf(
        "订单编号", "订单号", "交易单号", "支付成功", "微信支付", "支付宝", "商户单号", "交易时间",
        "账单详情", "已付款", "付款方式", "收款方", "转账成功", "扣款"
    )

    private val FINANCE_KEYWORDS = listOf(
        "银行", "招商银行", "中国工商", "建设银行", "农业银行", "信用卡", "储蓄卡", "可用余额",
        "理财", "基金", "股票", "收益率", "持仓", "账单", "开户", "还款日", "分期"
    )

    private val WORK_KEYWORDS = listOf(
        "会议", "腾讯会议", "钉钉", "飞书", "work", "meeting", "日程", "审批", "周报", "月报",
        "汇报", "总结", "方案", "需求文档", "项目", "里程碑", "看板", "研发", "发布", "bug", "jira",
        "confluence", "slack", "gitlab", "github", "zoom"
    )

    private val STUDY_KEYWORDS = listOf(
        "课程", "笔记", "作业", "选择题", "填空题", "答案", "解析", "错题", "复习", "考试",
        "英语", "数学", "物理", "化学", "讲义", "知识点", "考研", "托福", "雅思", "论文"
    )

    private val GAME_KEYWORDS = listOf(
        "胜率", "排位", "段位", "战绩", "赛季", "英雄", "击杀", "助攻", "王者荣耀", "原神",
        "和平精英", "绝地求生", "英雄联盟", "steam", "switch", "ps5", "xbox", "通关", "成就",
        "装备", "暴击", "副本", "boss"
    )

    private val TRAVEL_KEYWORDS = listOf(
        "酒店", "机票", "火车票", "高铁", "预订", "入住", "退房", "门票", "景区", "行程",
        "携程", "去哪儿", "飞猪", "同程", "航班", "候机楼", "登机口", "航站楼", "旅行攻略"
    )

    private val LOCATION_KEYWORDS = listOf(
        "地址", "收货地址", "收件人", "省", "市", "区", "街道", "号楼", "单元", "门牌号",
        "高德地图", "百度地图", "腾讯地图", "导航", "目的地", "起点", "距离", "路况"
    )

    private val SOCIAL_KEYWORDS = listOf(
        "小红书", "微博", "抖音", "快手", "朋友圈", "点赞", "评论", "关注", "粉丝", "转发",
        "小红薯", "热搜", "话题", "动态", "笔记详情", "发布于"
    )

    private val WEB_KEYWORDS = listOf(
        "http://", "https://", "www.", ".com", ".cn", ".net", ".org", "浏览器", "网页",
        "输入网址", "刷新", "书签", "历史记录"
    )

    private val DOC_KEYWORDS = listOf(
        "pdf", "doc", "docx", "word", "excel", "ppt", "合同", "证明", "发票", "清单",
        "说明书", "通知", "红头文件", "公文", "规范", "标准"
    )

    // Regex patterns for entity extraction
    private val PRICE_PATTERN = Pattern.compile("([¥￥]\\s*\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?\\s*元)")
    private val PICKUP_CODE_PATTERN = Pattern.compile("(?:取件码|提货码)[:：\\s]*([A-Za-z0-9\\-]+)")
    private val DATE_PATTERN = Pattern.compile("(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}(?:日)?)")

    fun classify(ocrText: String, fileName: String): ClassificationResult {
        val lowerText = ocrText.lowercase()
        val lowerFileName = fileName.lowercase()

        // 1. Calculate scores for each category
        val scores = mutableMapOf<String, Int>()

        fun scoreCategory(categoryId: String, keywords: List<String>, weightMultiplier: Int = 1) {
            var count = 0
            for (kw in keywords) {
                val lowerKw = kw.lowercase()
                if (lowerText.contains(lowerKw)) {
                    count += 2 * weightMultiplier
                }
                if (lowerFileName.contains(lowerKw)) {
                    count += 3 * weightMultiplier
                }
            }
            if (count > 0) {
                scores[categoryId] = count
            }
        }

        scoreCategory("shopping", SHOPPING_KEYWORDS, 2)
        scoreCategory("chat", CHAT_KEYWORDS, 2)
        scoreCategory("express", EXPRESS_KEYWORDS, 3)
        scoreCategory("order", ORDER_KEYWORDS, 2)
        scoreCategory("finance", FINANCE_KEYWORDS, 2)
        scoreCategory("work", WORK_KEYWORDS, 2)
        scoreCategory("study", STUDY_KEYWORDS, 2)
        scoreCategory("game", GAME_KEYWORDS, 2)
        scoreCategory("travel", TRAVEL_KEYWORDS, 2)
        scoreCategory("location", LOCATION_KEYWORDS, 2)
        scoreCategory("social", SOCIAL_KEYWORDS, 2)
        scoreCategory("web", WEB_KEYWORDS, 1)
        scoreCategory("doc", DOC_KEYWORDS, 1)

        // Select best category
        val bestCategory = scores.maxByOrNull { it.value }?.key ?: "other"

        // 2. Extract Tags
        val detectedTags = mutableSetOf<String>()
        val popularEntities = listOf(
            "京东", "淘宝", "天猫", "拼多多", "微信", "QQ", "顺丰", "菜鸟", "美团", "饿了么",
            "小红书", "抖音", "微博", "支付宝", "钉钉", "飞书", "腾讯会议", "高德地图", "携程",
            "显卡", "手机", "电脑", "键盘", "酒店", "机票", "门票", "快递", "订单", "发票"
        )
        for (entity in popularEntities) {
            if (lowerText.contains(entity.lowercase()) || lowerFileName.contains(entity.lowercase())) {
                detectedTags.add(entity)
            }
        }

        // Add category name as tag if not other
        val categoryChineseName = when (bestCategory) {
            "shopping" -> "购物"
            "chat" -> "聊天"
            "work" -> "工作"
            "study" -> "学习"
            "game" -> "游戏"
            "travel" -> "旅行"
            "finance" -> "消费"
            "express" -> "快递"
            "social" -> "社交"
            "doc" -> "资料"
            "location" -> "地址"
            "order" -> "订单"
            "web" -> "网页"
            else -> "其他"
        }
        if (bestCategory != "other") {
            detectedTags.add(categoryChineseName)
        }

        // 3. Extract Title
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() && it.length > 2 }
        val titleCandidate = when {
            lines.isNotEmpty() -> {
                // Find a line that looks like a heading or subject (skip pure timestamps or battery bars)
                val cleanLine = lines.firstOrNull {
                    !it.contains(":") && !it.contains("%") && it.length in 3..40
                } ?: lines.first().take(30)
                cleanLine
            }
            fileName.isNotBlank() -> fileName.substringBeforeLast(".")
            else -> "截图 $categoryChineseName"
        }

        // 4. Extract Summary or key highlights
        val summaryParts = mutableListOf<String>()
        val priceMatcher = PRICE_PATTERN.matcher(ocrText)
        if (priceMatcher.find()) {
            val price = priceMatcher.group().trim()
            summaryParts.add("价格: $price")
            detectedTags.add("价格")
        }

        val pickupMatcher = PICKUP_CODE_PATTERN.matcher(ocrText)
        if (pickupMatcher.find()) {
            val code = pickupMatcher.group(1)?.trim() ?: ""
            summaryParts.add("取件码: $code")
            detectedTags.add("取件码")
        }

        val dateMatcher = DATE_PATTERN.matcher(ocrText)
        if (dateMatcher.find()) {
            val date = dateMatcher.group().trim()
            summaryParts.add("日期: $date")
        }

        if (summaryParts.isEmpty() && lines.size > 1) {
            summaryParts.add(lines.take(3).joinToString(" · "))
        }

        val summary = if (summaryParts.isNotEmpty()) {
            summaryParts.joinToString(" | ")
        } else {
            if (ocrText.isNotBlank()) ocrText.take(60) else "已记录截图"
        }

        return ClassificationResult(
            categoryId = bestCategory,
            title = titleCandidate,
            summary = summary,
            tags = detectedTags.take(8).toList()
        )
    }
}
