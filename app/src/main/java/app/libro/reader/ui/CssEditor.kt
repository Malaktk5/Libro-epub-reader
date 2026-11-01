package app.libro.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.libro.reader.R
import app.libro.reader.data.Prefs

private const val VARS_REFERENCE = """--app-bg  --app-fg  --app-surface  --app-outline
--app-primary  --app-on-primary  --app-secondary
--page-bg  --page-fg  --page-link  --page-selection
--libro-bg  --libro-fg  --libro-link  --libro-size  --libro-margin  (read-only, usable with var())"""

/** Free-form stylesheet editor. Changes are saved with Apply so the app is not restyled on every keystroke. */
@Composable
fun CustomCssEditor(prefs: Prefs, modifier: Modifier = Modifier) {
    val saved by prefs.state.collectAsState()
    var text by remember { mutableStateOf(saved.customCss) }
    val dirty = text != saved.customCss

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.custom_css_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp),
            placeholder = { Text("body { ... }", fontFamily = FontFamily.Monospace) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { text = CSS_EXAMPLE }) { Text(stringResource(R.string.custom_css_example)) }
            TextButton(onClick = { text = ""; prefs.update { it.copy(customCss = "") } }) {
                Text(stringResource(R.string.custom_css_clear))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Button(onClick = { prefs.update { it.copy(customCss = text) } }, enabled = dirty) {
                    Text(stringResource(R.string.custom_css_apply))
                }
            }
        }
        Text(
            stringResource(R.string.custom_css_vars),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            VARS_REFERENCE,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
