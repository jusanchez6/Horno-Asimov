package com.hornoreflow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val AsimovBackground = Color(0xFF0A0C10)
private val AsimovSurface = Color(0xFF12161C)
private val AsimovSurfaceVariant = Color(0xFF1B212B)
private val AsimovCyan = Color(0xFF00E5FF)
private val AsimovAmber = Color(0xFFFFB300)
private val AsimovRed = Color(0xFFFF3B5C)
private val AsimovTextPrimary = Color(0xFFE4F7FF)
private val AsimovTextSecondary = Color(0xFF8FA3AE)

private val AsimovColorScheme = darkColorScheme(
    primary = AsimovCyan,
    onPrimary = Color(0xFF00232B),
    secondary = AsimovAmber,
    onSecondary = Color(0xFF2B1D00),
    background = AsimovBackground,
    onBackground = AsimovTextPrimary,
    surface = AsimovSurface,
    onSurface = AsimovTextPrimary,
    surfaceVariant = AsimovSurfaceVariant,
    onSurfaceVariant = AsimovTextSecondary,
    error = AsimovRed,
    onError = Color.White,
    outline = Color(0xFF00E5FF).copy(alpha = 0.4f),
    outlineVariant = Color(0xFF00E5FF).copy(alpha = 0.18f)
)

private val baseTypography = Typography()

/** Todo el tipeo en monoespaciado: es la firma visual "terminal retro-futurista" de Asimov. */
private val AsimovTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = FontFamily.Monospace),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = FontFamily.Monospace),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = FontFamily.Monospace),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = FontFamily.Monospace),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = FontFamily.Monospace),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
)

/**
 * Tema unico de Asimov: siempre oscuro con acentos neon (cian/ambar). No sigue el
 * claro/oscuro del sistema a proposito, para mantener la identidad "terminal
 * retro-futurista" consistente en cualquier dispositivo.
 */
@Composable
fun AsimovTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AsimovColorScheme, typography = AsimovTypography, content = content)
}
