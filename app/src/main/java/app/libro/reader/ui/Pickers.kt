package app.libro.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.libro.reader.R
import app.libro.reader.data.AppThemes
import app.libro.reader.data.Langs

@Composable
private fun langName(key: String): String = when (key) {
    Langs.EN -> stringResource(R.string.lang_en)
    Langs.FR -> stringResource(R.string.lang_fr)
    Langs.AR -> stringResource(R.string.lang_ar)
    else -> stringResource(R.string.lang_system)
}

@Composable
fun LanguagePicker(selected: String, onSelect: (String) -> Unit) {
    val keys = listOf(Langs.SYSTEM) + Langs.all
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    ChoiceCard(selected == key, { onSelect(key) }, Modifier.weight(1f)) {
                        Text(
                            langName(key),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
                if (row.size == 1) Row(Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
fun AppThemePicker(selected: String, onSelect: (String) -> Unit) {
    data class Opt(val key: String, val label: String, val bg: Color, val fg: Color)
    val opts = listOf(
        Opt(AppThemes.SYSTEM, stringResource(R.string.theme_system), Color(0xFFE9E5DC), Color(0xFF1C1B19)),
        Opt(AppThemes.LIGHT, stringResource(R.string.theme_light), Color(0xFFFBF8F3), Color(0xFF1C1B19)),
        Opt(AppThemes.DARK, stringResource(R.string.theme_dark), Color(0xFF121413), Color(0xFFE4E3DE)),
        Opt(AppThemes.SEPIA, stringResource(R.string.theme_sepia), Color(0xFFF4ECD8), Color(0xFF4B3A26)),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        opts.forEach { o ->
            ChoiceCard(selected == o.key, { onSelect(o.key) }, Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ThemeSwatch(o.bg, o.fg, Modifier.fillMaxWidth().height(44.dp))
                    Text(
                        o.label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
