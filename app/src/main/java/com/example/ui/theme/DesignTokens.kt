package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design Tokens for high-end minimalist Apple / Linear / Notion aesthetic.
 * Focuses on restraint, monochrome harmony, subtle hairline borders, and balanced spacing.
 */
object DesignTokens {
    // Spacing
    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 12.dp
    val SpacingLarge = 16.dp
    val SpacingXLarge = 20.dp
    val Spacing2XLarge = 24.dp
    val Spacing3XLarge = 32.dp

    // Corner Radii (measured & moderate - avoids cartoonish giant pills)
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusCard = 14.dp
    val CornerRadiusLarge = 18.dp

    // Hairline border width
    val HairlineBorder = 0.5.dp
    val ThinBorder = 1.dp

    // Shapes
    val ShapeSmall = RoundedCornerShape(CornerRadiusSmall)
    val ShapeMedium = RoundedCornerShape(CornerRadiusMedium)
    val ShapeCard = RoundedCornerShape(CornerRadiusCard)
    val ShapeLarge = RoundedCornerShape(CornerRadiusLarge)

    @Composable
    fun cardBorder(color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)): BorderStroke {
        return BorderStroke(HairlineBorder, color)
    }

    @Composable
    fun subtleBorder(color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)): BorderStroke {
        return BorderStroke(HairlineBorder, color)
    }
}
