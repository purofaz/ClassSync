package com.example.classsync.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color // ADDED: Import for Color

private val DarkColorScheme = darkColorScheme(
    primary = PurpleBlueAccent,
    secondary = PurpleBlueMedium,
    tertiary = PurpleBlueLight,

    // Other default colors to override for a consistent dark theme
    background = PurpleBlueDark,
    surface = PurpleBlueMedium,
    onPrimary = Color.White, // Changed to White for readability on dark background
    onSecondary = Color.White, // Changed to White for readability on dark background
    onTertiary = Color.White, // Changed to White for readability on dark background
    onBackground = Color.White, // Changed to White for readability on dark background
    onSurface = Color.White, // Changed to White for readability on dark background
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleBlueLight,
    secondary = PurpleBlueAccent,
    tertiary = PurpleBlueMedium,

    // Other default colors to override for a consistent light theme
    background = Color.White, // Keeping a bright background for light theme
    surface = Color.LightGray,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun ClassSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
