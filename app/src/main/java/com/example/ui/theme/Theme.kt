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

private val DarkColorScheme = darkColorScheme(
    primary = TurfNeonGreen,
    onPrimary = Color(0xFF003912),
    primaryContainer = TurfGreenDark,
    onPrimaryContainer = Color(0xFF6CFF9F),
    secondary = BallAmber,
    onSecondary = Color(0xFF432C00),
    tertiary = CricketLeatherRed,
    onTertiary = Color.White,
    background = AsphaltDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SurfaceBorderDark,
    error = CricketLeatherRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TurfGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF003912),
    secondary = Color(0xFFE65100),
    onSecondary = Color.White,
    tertiary = CricketLeatherRed,
    onTertiary = Color.White,
    background = SunlightBackground,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCFD8DC),
    error = CricketLeatherRed,
    onError = Color.White
)

@Composable
fun GaliCricketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep athletic branding consistent by default
    content: @Composable () -> Unit,
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    GaliCricketTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
