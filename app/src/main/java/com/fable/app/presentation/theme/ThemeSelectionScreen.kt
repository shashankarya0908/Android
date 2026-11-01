package com.fable.app.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fable.app.domain.model.FableThemeName

@Composable
fun ThemeSelectionScreen(
    selectedTheme: FableThemeName,
    statusMessage: String?,
    errorMessage: String?,
    onSelect: (FableThemeName) -> Unit,
    onDone: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Choose your Fable", style = MaterialTheme.typography.headlineMedium)
            Text("Each theme changes the app palette, typography, component shapes, and surfaces.")
        }
        statusMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        items(FableThemeName.entries, key = FableThemeName::storageValue) { theme ->
            ThemePreviewCard(theme, selectedTheme == theme, onSelect = { onSelect(theme) })
        }
        item {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@Composable
private fun ThemePreviewCard(theme: FableThemeName, selected: Boolean, onSelect: () -> Unit) {
    FableThemePreview(theme) {
        val tokens = FableThemeTokens.current
        Card(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else tokens.cardBorder,
            ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation),
        ) {
            Column(
                Modifier
                    .background(tokens.pageBrush)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("${tokens.ornament}  ${theme.label}", style = MaterialTheme.typography.titleLarge)
                        Text(theme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Text("Selected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
                Text("The Secret Garden", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("A reading life, arranged around the stories you love.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PaletteSwatch(MaterialTheme.colorScheme.primary)
                    PaletteSwatch(MaterialTheme.colorScheme.secondary)
                    PaletteSwatch(MaterialTheme.colorScheme.tertiary)
                    Box(Modifier.weight(1f))
                    if (selected) {
                        OutlinedButton(onClick = onSelect, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                            Text("Use this theme")
                        }
                    } else {
                        Button(onClick = onSelect, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                            Text("Use this theme")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(color: Color) {
    Box(
        Modifier
            .size(22.dp)
            .background(color, CircleShape),
    )
}
