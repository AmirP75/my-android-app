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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    tertiary = Color(0xFFCF6679),
    onTertiary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.Black
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
  )


@Composable
fun animateColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animateColorAsState(target.primary, tween(500), label = "").value,
        onPrimary = animateColorAsState(target.onPrimary, tween(500), label = "").value,
        primaryContainer = animateColorAsState(target.primaryContainer, tween(500), label = "").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, tween(500), label = "").value,
        inversePrimary = animateColorAsState(target.inversePrimary, tween(500), label = "").value,
        secondary = animateColorAsState(target.secondary, tween(500), label = "").value,
        onSecondary = animateColorAsState(target.onSecondary, tween(500), label = "").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, tween(500), label = "").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, tween(500), label = "").value,
        tertiary = animateColorAsState(target.tertiary, tween(500), label = "").value,
        onTertiary = animateColorAsState(target.onTertiary, tween(500), label = "").value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, tween(500), label = "").value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, tween(500), label = "").value,
        background = animateColorAsState(target.background, tween(500), label = "").value,
        onBackground = animateColorAsState(target.onBackground, tween(500), label = "").value,
        surface = animateColorAsState(target.surface, tween(500), label = "").value,
        onSurface = animateColorAsState(target.onSurface, tween(500), label = "").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, tween(500), label = "").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, tween(500), label = "").value,
        surfaceTint = animateColorAsState(target.surfaceTint, tween(500), label = "").value,
        inverseSurface = animateColorAsState(target.inverseSurface, tween(500), label = "").value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, tween(500), label = "").value,
        error = animateColorAsState(target.error, tween(500), label = "").value,
        onError = animateColorAsState(target.onError, tween(500), label = "").value,
        errorContainer = animateColorAsState(target.errorContainer, tween(500), label = "").value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, tween(500), label = "").value,
        outline = animateColorAsState(target.outline, tween(500), label = "").value,
        outlineVariant = animateColorAsState(target.outlineVariant, tween(500), label = "").value,
        scrim = animateColorAsState(target.scrim, tween(500), label = "").value,
    )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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

  val animatedColorScheme = animateColorScheme(colorScheme)

  MaterialTheme(colorScheme = animatedColorScheme, typography = Typography, content = content)
}
