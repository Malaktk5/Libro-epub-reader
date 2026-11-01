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

/** Display name of any app or reader theme key. */
fun themeNameRes(key: String): Int = when (key) {
    "system" -> R.string.theme_system
    "light" -> R.string.theme_light
    "dark" -> R.string.theme_dark
    "sepia" -> R.string.theme_sepia
    "gray" -> R.string.theme_gray
    "black" -> R.string.theme_black
    "amoled" -> R.string.theme_amoled
    "midnight" -> R.string.theme_midnight
    "nord" -> R.string.theme_nord
    "gruvbox" -> R.string.theme_gruvbox
    "forest" -> R.string.theme_forest
    "solar" -> R.string.theme_solar
    "solar_light" -> R.string.theme_solar_light
    "solar_dark" -> R.string.theme_solar_dark
    "rose" -> R.string.theme_rose
    "lavender" -> R.string.theme_lavender
    else -> R.string.theme_auto
}

@Composable
fun AppThemePicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppThemes.all.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val (bg, fg) = appThemeSwatch(key)
                    ChoiceCard(selected == key, { onSelect(key) }, Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            ThemeSwatch(bg, fg, Modifier.fillMaxWidth().height(44.dp))
                            Text(
                                stringResource(themeNameRes(key)),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
                repeat(3 - row.size) { Row(Modifier.weight(1f)) {} }
            }
        }
    }
}
