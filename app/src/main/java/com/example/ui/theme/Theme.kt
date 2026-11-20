package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudyWinDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF1F1600),
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = GoldBright,
    secondary = PurpleVibrant,
    onSecondary = Color.White,
    secondaryContainer = PurpleContainerDark,
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = CyanAccent,
    onTertiary = Color(0xFF002438),
    background = MidnightBg,
    onBackground = Color(0xFFF3F5FF),
    surface = MidnightSurface,
    onSurface = Color(0xFFF3F5FF),
    surfaceVariant = MidnightSurfaceElevated,
    onSurfaceVariant = Color(0xFFB3BDE0),
    outline = MidnightBorder,
    error = RubyError,
    onError = Color.White
)

private val StudyWinLightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = GoldContainerLight,
    onPrimaryContainer = Color(0xFF382600),
    secondary = GoldDeep,
    onSecondary = Color.White,
    secondaryContainer = PurpleContainerLight,
    onSecondaryContainer = PurpleDeep,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFD0D7EE),
    error = RubyError,
    onError = Color.White
)

val StudyWinShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun StudyWinTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudyWinDarkColorScheme else StudyWinLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudyWinTypography,
        shapes = StudyWinShapes,
        content = content
    )
}
