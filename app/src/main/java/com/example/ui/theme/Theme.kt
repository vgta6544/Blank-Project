package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = BinanceYellow,
    onPrimary = Color.Black,
    primaryContainer = BinanceSurface,
    onPrimaryContainer = TextPrimaryDark,
    secondary = CryptoGreen,
    onSecondary = Color.Black,
    tertiary = CryptoRed,
    onTertiary = Color.White,
    background = BinanceDarkBg,
    onBackground = TextPrimaryDark,
    surface = BinanceCardBg,
    onSurface = TextPrimaryDark,
    surfaceVariant = BinanceSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = BinanceBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC99400),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF8E1),
    onPrimaryContainer = Color(0xFF261900),
    secondary = CryptoGreen,
    onSecondary = Color.White,
    tertiary = CryptoRed,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1E2329),
    surface = LightSurface,
    onSurface = Color(0xFF1E2329),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF707A8A),
    outline = LightBorder
)

@Composable
fun CoinAlertTheme(
    darkTheme: Boolean = true, // Default to sleek dark crypto theme
    dynamicColor: Boolean = false, // Keep consistent crypto styling
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

