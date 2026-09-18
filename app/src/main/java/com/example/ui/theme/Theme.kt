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
    primary = Color(0xFFFAFAFA),
    onPrimary = Neutral1000,
    primaryContainer = Neutral800,
    onPrimaryContainer = Color(0xFFFAFAFA),
    secondary = Neutral300,
    onSecondary = Neutral1000,
    secondaryContainer = Neutral800,
    onSecondaryContainer = Color(0xFFEDEDF0),
    tertiary = Neutral400,
    onTertiary = Neutral1000,
    tertiaryContainer = Neutral800,
    onTertiaryContainer = Color(0xFFEDEDF0),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerHigh = Color(0xFF1E1E24),
    surfaceContainerHighest = Color(0xFF282830),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = StatusRedDark,
    onError = Color.White,
    errorContainer = StatusRedDarkSubtle,
    onErrorContainer = StatusRedDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Neutral1000,
    onPrimary = Color.White,
    primaryContainer = Neutral900,
    onPrimaryContainer = Color.White,
    secondary = Neutral700,
    onSecondary = Color.White,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Neutral1000,
    tertiary = Neutral600,
    onTertiary = Color.White,
    tertiaryContainer = Neutral100,
    onTertiaryContainer = Neutral900,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerHigh = Neutral100,
    surfaceContainerHighest = Neutral150,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedSubtle,
    onErrorContainer = StatusRed
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
