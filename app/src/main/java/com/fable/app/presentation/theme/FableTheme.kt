package com.fable.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.fable.app.domain.model.FableThemeName

data class FablePalette(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    val dark: Boolean,
)

data class FableThemeTokens(
    val pageBrush: Brush,
    val cardBorder: Color,
    val ornament: String,
    val cardElevation: Dp,
) {
    companion object {
        val current: FableThemeTokens
            @Composable get() = LocalFableThemeTokens.current
    }
}

data class FableThemeDefinition(
    val name: FableThemeName,
    val palette: FablePalette,
    val colorScheme: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val tokens: FableThemeTokens,
)

private val LocalFableThemeTokens = compositionLocalOf {
    FableThemeTokens(
        pageBrush = Brush.verticalGradient(listOf(Color(0xFFF4F1E8), Color(0xFFF8F6F0))),
        cardBorder = Color(0xFFD6D1C4),
        ornament = "✧",
        cardElevation = 1.dp,
    )
}

@Composable
fun FableTheme(theme: FableThemeName = FableThemeName.DEFAULT, content: @Composable () -> Unit) {
    val definition = fableThemeDefinition(theme)
    CompositionLocalProvider(LocalFableThemeTokens provides definition.tokens) {
        MaterialTheme(
            colorScheme = definition.colorScheme,
            typography = definition.typography,
            shapes = definition.shapes,
            content = content,
        )
    }
}

@Composable
fun FableThemePreview(theme: FableThemeName, content: @Composable () -> Unit) {
    val definition = fableThemeDefinition(theme)
    CompositionLocalProvider(LocalFableThemeTokens provides definition.tokens) {
        MaterialTheme(
            colorScheme = definition.colorScheme,
            typography = definition.typography,
            shapes = definition.shapes,
            content = content,
        )
    }
}

fun fableThemeDefinition(name: FableThemeName): FableThemeDefinition = when (name) {
    FableThemeName.GOTHIC -> definition(
        name,
        FablePalette(
            primary = Color(0xFFE4D8BD), onPrimary = Color(0xFF201D19),
            secondary = Color(0xFFB9AD98), onSecondary = Color(0xFF24211C),
            tertiary = Color(0xFFC2A56B), onTertiary = Color(0xFF241E12),
            background = Color(0xFF11100F), onBackground = Color(0xFFECE6DA),
            surface = Color(0xFF1D1B18), onSurface = Color(0xFFECE6DA),
            surfaceVariant = Color(0xFF302D28), onSurfaceVariant = Color(0xFFCCC4B7),
            outline = Color(0xFF71695B), error = Color(0xFFFFB4AB), dark = true,
        ),
        headingFont = FontFamily.Serif,
        headingSpacing = 0.35.sp,
        corner = 4.dp,
        ornament = "♜",
        brushEnd = Color(0xFF191714),
    )
    FableThemeName.GOTHIC_ROMANCE -> definition(
        name,
        FablePalette(
            primary = Color(0xFFDDA1B1), onPrimary = Color(0xFF31151F),
            secondary = Color(0xFF6F3647), onSecondary = Color(0xFFFFE9EF),
            tertiary = Color(0xFFE4D4C9), onTertiary = Color(0xFF2B1B1F),
            background = Color(0xFF170F13), onBackground = Color(0xFFF2E5E8),
            surface = Color(0xFF24171D), onSurface = Color(0xFFF2E5E8),
            surfaceVariant = Color(0xFF38252E), onSurfaceVariant = Color(0xFFD9C1C8),
            outline = Color(0xFF80616C), error = Color(0xFFFFB4AB), dark = true,
        ),
        headingFont = FontFamily.Serif,
        headingSpacing = 0.25.sp,
        corner = 14.dp,
        ornament = "❦",
        brushEnd = Color(0xFF21151A),
    )
    FableThemeName.DARK_ACADEMIA -> definition(
        name,
        FablePalette(
            primary = Color(0xFF3C5843), onPrimary = Color(0xFFF7F0DF),
            secondary = Color(0xFF775C37), onSecondary = Color(0xFFFFF3DE),
            tertiary = Color(0xFF846D3F), onTertiary = Color(0xFFFFF4D9),
            background = Color(0xFFE9DFC8), onBackground = Color(0xFF30281F),
            surface = Color(0xFFF5EDD9), onSurface = Color(0xFF30281F),
            surfaceVariant = Color(0xFFE2D5BD), onSurfaceVariant = Color(0xFF514637),
            outline = Color(0xFF81745E), error = Color(0xFF9C3932), dark = false,
        ),
        headingFont = FontFamily.Serif,
        headingSpacing = 0.15.sp,
        corner = 8.dp,
        ornament = "❧",
        brushEnd = Color(0xFFF0E7D4),
    )
    FableThemeName.DREAMSCAPE -> definition(
        name,
        FablePalette(
            primary = Color(0xFF4A527C), onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF707AA5), onSecondary = Color(0xFFFFFFFF),
            tertiary = Color(0xFF79638E), onTertiary = Color(0xFFFFFFFF),
            background = Color(0xFFECEAF5), onBackground = Color(0xFF292D48),
            surface = Color(0xFFF7F6FC), onSurface = Color(0xFF292D48),
            surfaceVariant = Color(0xFFDFDEED), onSurfaceVariant = Color(0xFF4A4D67),
            outline = Color(0xFF777B99), error = Color(0xFF9C3E4E), dark = false,
        ),
        headingFont = FontFamily.SansSerif,
        headingSpacing = 0.1.sp,
        corner = 20.dp,
        ornament = "☾",
        brushEnd = Color(0xFFF3F1FA),
    )
    FableThemeName.MINIMALIST -> definition(
        name,
        FablePalette(
            primary = Color(0xFF405A48), onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF626B61), onSecondary = Color(0xFFFFFFFF),
            tertiary = Color(0xFF7A6848), onTertiary = Color(0xFFFFFFFF),
            background = Color(0xFFF4F1E8), onBackground = Color(0xFF292D29),
            surface = Color(0xFFFBF9F2), onSurface = Color(0xFF292D29),
            surfaceVariant = Color(0xFFE7E3D8), onSurfaceVariant = Color(0xFF4F544D),
            outline = Color(0xFF77796F), error = Color(0xFFB3261E), dark = false,
        ),
        headingFont = FontFamily.SansSerif,
        headingSpacing = 0.sp,
        corner = 10.dp,
        ornament = "—",
        brushEnd = Color(0xFFF8F6F0),
    )
}

private fun definition(
    name: FableThemeName,
    palette: FablePalette,
    headingFont: FontFamily,
    headingSpacing: androidx.compose.ui.unit.TextUnit,
    corner: Dp,
    ornament: String,
    brushEnd: Color,
): FableThemeDefinition {
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.primary, onPrimary = palette.onPrimary,
            secondary = palette.secondary, onSecondary = palette.onSecondary,
            tertiary = palette.tertiary, onTertiary = palette.onTertiary,
            background = palette.background, onBackground = palette.onBackground,
            surface = palette.surface, onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant, onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline, error = palette.error,
        )
    } else {
        lightColorScheme(
            primary = palette.primary, onPrimary = palette.onPrimary,
            secondary = palette.secondary, onSecondary = palette.onSecondary,
            tertiary = palette.tertiary, onTertiary = palette.onTertiary,
            background = palette.background, onBackground = palette.onBackground,
            surface = palette.surface, onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant, onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline, error = palette.error,
        )
    }
    val defaultTypography = Typography()
    val typography = defaultTypography.copy(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = headingFont, letterSpacing = headingSpacing),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = headingFont, letterSpacing = headingSpacing),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = headingFont, letterSpacing = headingSpacing),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = headingFont, letterSpacing = headingSpacing),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = headingFont, letterSpacing = headingSpacing),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = headingFont, letterSpacing = headingSpacing),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = headingFont, fontWeight = FontWeight.SemiBold),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = headingFont, fontWeight = FontWeight.Medium),
    )
    val shape = RoundedCornerShape(corner)
    return FableThemeDefinition(
        name = name,
        palette = palette,
        colorScheme = scheme,
        typography = typography,
        shapes = Shapes(small = shape, medium = shape, large = shape),
        tokens = FableThemeTokens(
            pageBrush = Brush.verticalGradient(listOf(palette.background, brushEnd)),
            cardBorder = palette.outline.copy(alpha = 0.48f),
            ornament = ornament,
            cardElevation = if (name == FableThemeName.MINIMALIST) 0.dp else 2.dp,
        ),
    )
}
