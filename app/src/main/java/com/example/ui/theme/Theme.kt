package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFF4F4F5),
    onPrimary = Color(0xFF18181B),
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color(0xFFF4F4F5),
    secondary = Color(0xFFD4D4D8),
    onSecondary = Color(0xFF18181B),
    secondaryContainer = Color(0xFF2E2E35),
    onSecondaryContainer = Color(0xFFE4E4E7),
    tertiary = BrandAccentAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3B2816),
    onTertiaryContainer = Color(0xFFFED7AA),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandBlack,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF52525B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEBE3),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = BrandAccentAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

