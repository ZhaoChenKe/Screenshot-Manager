package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryStyle(
    val icon: ImageVector,
    val accentColor: Color,
    val lightBgColor: Color,
    val displayName: String
)

object CategoryUiHelper {

    // Restrained monochrome tones: subtle charcoal icon, minimal neutral badge
    private val DefaultAccent = Color(0xFF27272A)
    private val DefaultLightBg = Color(0xFFF4F4F5)

    fun getStyle(categoryId: String): CategoryStyle {
        val (icon, name) = when (categoryId.lowercase()) {
            "game" -> Pair(Icons.Outlined.SportsEsports, "游戏")
            "shopping" -> Pair(Icons.Outlined.ShoppingBag, "购物")
            "chat" -> Pair(Icons.Outlined.ChatBubbleOutline, "聊天")
            "express" -> Pair(Icons.Outlined.LocalShipping, "快递")
            "food" -> Pair(Icons.Outlined.Restaurant, "餐饮")
            "work" -> Pair(Icons.Outlined.WorkOutline, "工作")
            "study" -> Pair(Icons.Outlined.School, "学习")
            "finance" -> Pair(Icons.Outlined.AccountBalanceWallet, "消费")
            "order" -> Pair(Icons.AutoMirrored.Outlined.ReceiptLong, "订单")
            "travel" -> Pair(Icons.Outlined.Flight, "旅行")
            "doc" -> Pair(Icons.Outlined.Description, "资料")
            "social" -> Pair(Icons.Outlined.ChatBubbleOutline, "社交")
            "location" -> Pair(Icons.Outlined.Place, "地址")
            "web" -> Pair(Icons.Outlined.Language, "网页")
            "other" -> Pair(Icons.Outlined.FolderOpen, "其他")
            else -> Pair(Icons.Outlined.Category, "未分类")
        }

        return CategoryStyle(
            icon = icon,
            accentColor = DefaultAccent,
            lightBgColor = DefaultLightBg,
            displayName = name
        )
    }
}
