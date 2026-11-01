package com.fable.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FableLightColors = lightColorScheme(
    primary = Color(0xFF52624A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF59635A),
    background = Color(0xFFFAF9F6),
    surface = Color(0xFFFAF9F6),
)

@Composable
fun FableTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FableLightColors,
        typography = Typography(),
        content = content,
    )
}
