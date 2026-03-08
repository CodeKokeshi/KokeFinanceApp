package com.codekokeshi.kokefinanceapp.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = Mint200,
    onPrimary = Pine900,
    primaryContainer = Pine700,
    onPrimaryContainer = Sand050,
    secondary = Ember300,
    onSecondary = Slate900,
    tertiary = Gold400,
    background = Slate900,
    onBackground = Sand050,
    surface = Color(0xFF152320),
    onSurface = Sand050,
    surfaceVariant = Color(0xFF1E2E2A),
    onSurfaceVariant = Color(0xFFBECCC7),
    surfaceContainer = Color(0xFF182723),
    surfaceContainerHigh = Color(0xFF1F302B),
    surfaceContainerHighest = Color(0xFF273934),
    outline = Color(0xFF73857F),
    error = Color(0xFFFF8F82)
)

private val LightColorScheme = lightColorScheme(
    primary = Pine700,
    onPrimary = Sand050,
    primaryContainer = Mint200,
    onPrimaryContainer = Pine900,
    secondary = Ember500,
    onSecondary = Sand050,
    secondaryContainer = Ember300,
    onSecondaryContainer = Slate900,
    tertiary = Gold400,
    onTertiary = Slate900,
    background = Sand050,
    onBackground = Slate900,
    surface = Color(0xFFFFFCF7),
    onSurface = Slate900,
    surfaceVariant = Sand100,
    onSurfaceVariant = Slate700,
    surfaceContainerLowest = Color(0xFFFFFDF9),
    surfaceContainerLow = Color(0xFFF9F3E8),
    surfaceContainer = Color(0xFFF5ECDC),
    surfaceContainerHigh = Color(0xFFF0E4D0),
    surfaceContainerHighest = Sand200,
    outline = Color(0xFFA29179),
    error = Color(0xFFB3261E)
)

@Composable
fun KokeFinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}