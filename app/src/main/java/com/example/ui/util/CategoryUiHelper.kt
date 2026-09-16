package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
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
            "shopping" -> CategoryStyle(
                icon = Icons.Outlined.ShoppingBag,
                accentColor = Color(0xFFE65100),
                lightBgColor = Color(0xFFFFF3E0),
                displayName = "购物"
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
                accentColor = Color(0xFFEF6C00),
                lightBgColor = Color(0xFFFFF8E1),
                displayName = "社交"
            )
            "other" -> CategoryStyle(
                icon = Icons.Outlined.FolderOpen,
                accentColor = Color(0xFF455A64),
                lightBgColor = Color(0xFFECEFF1),
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
