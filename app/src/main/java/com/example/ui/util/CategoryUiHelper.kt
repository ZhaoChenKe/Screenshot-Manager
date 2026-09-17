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

    fun getStyle(categoryId: String): CategoryStyle {
        return when (categoryId.lowercase()) {
            "game" -> CategoryStyle(
                icon = Icons.Outlined.SportsEsports,
                accentColor = Color(0xFF7C3AED),
                lightBgColor = Color(0xFFEDE9FE),
                displayName = "游戏"
            )
            "shopping" -> CategoryStyle(
                icon = Icons.Outlined.ShoppingBag,
                accentColor = Color(0xFFE65100),
                lightBgColor = Color(0xFFFFF3E0),
                displayName = "购物"
            )
            "chat" -> CategoryStyle(
                icon = Icons.Outlined.ChatBubbleOutline,
                accentColor = Color(0xFF0D9488),
                lightBgColor = Color(0xFFCCFBF1),
                displayName = "聊天"
            )
            "express" -> CategoryStyle(
                icon = Icons.Outlined.LocalShipping,
                accentColor = Color(0xFF0277BD),
                lightBgColor = Color(0xFFE1F5FE),
                displayName = "快递"
            )
            "food" -> CategoryStyle(
                icon = Icons.Outlined.Restaurant,
                accentColor = Color(0xFFC2185B),
                lightBgColor = Color(0xFFFCE4EC),
                displayName = "餐饮"
            )
            "work" -> CategoryStyle(
                icon = Icons.Outlined.WorkOutline,
                accentColor = Color(0xFF283593),
                lightBgColor = Color(0xFFE8EAF6),
                displayName = "工作"
            )
            "study" -> CategoryStyle(
                icon = Icons.Outlined.School,
                accentColor = Color(0xFF15803D),
                lightBgColor = Color(0xFFDCFCE7),
                displayName = "学习"
            )
            "finance" -> CategoryStyle(
                icon = Icons.Outlined.AccountBalanceWallet,
                accentColor = Color(0xFFB45309),
                lightBgColor = Color(0xFFFEF3C7),
                displayName = "消费"
            )
            "order" -> CategoryStyle(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                accentColor = Color(0xFF4F46E5),
                lightBgColor = Color(0xFFEEF2FF),
                displayName = "订单"
            )
            "travel" -> CategoryStyle(
                icon = Icons.Outlined.Flight,
                accentColor = Color(0xFF00695C),
                lightBgColor = Color(0xFFE0F2F1),
                displayName = "旅行"
            )
            "doc" -> CategoryStyle(
                icon = Icons.Outlined.Description,
                accentColor = Color(0xFF6A1B9A),
                lightBgColor = Color(0xFFF3E5F5),
                displayName = "资料"
            )
            "social" -> CategoryStyle(
                icon = Icons.Outlined.ChatBubbleOutline,
                accentColor = Color(0xFFEA580C),
                lightBgColor = Color(0xFFFFEDD5),
                displayName = "社交"
            )
            "location" -> CategoryStyle(
                icon = Icons.Outlined.Place,
                accentColor = Color(0xFFDC2626),
                lightBgColor = Color(0xFFFEE2E2),
                displayName = "地址"
            )
            "web" -> CategoryStyle(
                icon = Icons.Outlined.Language,
                accentColor = Color(0xFF2563EB),
                lightBgColor = Color(0xFFDBEAFE),
                displayName = "网页"
            )
            "other" -> CategoryStyle(
                icon = Icons.Outlined.FolderOpen,
                accentColor = Color(0xFF475569),
                lightBgColor = Color(0xFFF1F5F9),
                displayName = "其他"
            )
            else -> CategoryStyle(
                icon = Icons.Outlined.Category,
                accentColor = Color(0xFF374151),
                lightBgColor = Color(0xFFF3F4F6),
                displayName = "未分类"
            )
        }
    }
}

