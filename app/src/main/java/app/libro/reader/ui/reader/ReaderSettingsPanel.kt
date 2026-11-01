package app.libro.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.libro.reader.R
import app.libro.reader.data.Aligns
import app.libro.reader.data.FontFamilies
import app.libro.reader.data.Prefs
import app.libro.reader.data.ReadModes
import app.libro.reader.data.ReaderThemes
import app.libro.reader.data.UiPrefs
import app.libro.reader.ui.LocalThemeInfo
import app.libro.reader.ui.Palettes
import app.libro.reader.ui.themeNameRes

@Composable
private fun fontLabel(key: String) = stringResource(
    when (key) {
        FontFamilies.LITERATA -> R.string.font_literata
        FontFamilies.ATKINSON -> R.string.font_atkinson
        FontFamilies.AMIRI -> R.string.font_amiri
        FontFamilies.NASKH -> R.string.font_naskh
        FontFamilies.SERIF -> R.string.font_serif
        FontFamilies.MONO -> R.string.font_mono
        else -> R.string.font_publisher
    }
)

/** Bottom-sheet version used inside the reader. */
@Composable
fun ReaderSettingsPanel(prefs: Prefs, onEditCss: (() -> Unit)?) {
    ReaderSettingsContent(
        prefs,
        onEditCss,
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    )
}

/** The settings controls themselves; the caller decides how they scroll. Also embedded in the app Settings screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsContent(prefs: Prefs, onEditCss: (() -> Unit)?, modifier: Modifier = Modifier) {
    val p by prefs.state.collectAsState()
    val info = LocalThemeInfo.current

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reader_settings), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                val d = UiPrefs()
                prefs.update {
                    it.copy(
                        readerTheme = d.readerTheme, readerMode = d.readerMode, fontFamily = d.fontFamily,
                        fontScale = d.fontScale, lineHeight = d.lineHeight, margin = d.margin, align = d.align,
                        hyphenate = d.hyphenate, forceStyle = d.forceStyle, keepScreenOn = d.keepScreenOn,
                        brightness = d.brightness,
                    )
                }
            }) { Text(stringResource(R.string.reset)) }
        }

        // ---- page theme
        Label(stringResource(R.string.page_theme))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReaderThemes.all.forEach { key ->
                val pal = Palettes.base(key, info)
                val selected = p.readerTheme == key
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp).clickable { prefs.update { it.copy(readerTheme = key) } },
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(pal.bg)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) { Text("Aa", color = pal.fg, fontWeight = FontWeight.SemiBold) }
                    Text(
                        stringResource(themeNameRes(key)),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        // ---- reading mode
        Label(stringResource(R.string.reading_mode))
        val modes = listOf(ReadModes.PAGED to R.string.mode_paged, ReadModes.SCROLL to R.string.mode_scroll)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, (key, label) ->
                SegmentedButton(
                    selected = p.readerMode == key,
                    onClick = { prefs.update { it.copy(readerMode = key) } },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                ) { Text(stringResource(label)) }
            }
        }

        // ---- font
        Label(stringResource(R.string.font))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FontFamilies.all.forEach { key ->
                FilterChip(
                    selected = p.fontFamily == key,
                    onClick = { prefs.update { it.copy(fontFamily = key, forceStyle = if (key == FontFamilies.PUBLISHER) it.forceStyle else true) } },
                    label = { Text(fontLabel(key)) },
                )
            }
        }

        // ---- size / spacing / margins
        Spacer(Modifier.height(8.dp))
        SliderRow(stringResource(R.string.text_size), "${p.fontScale}%", p.fontScale.toFloat(), 60f..260f, 38) {
            prefs.update { s -> s.copy(fontScale = (it / 5f).toInt() * 5) }
        }
        SliderRow(stringResource(R.string.line_spacing), "%.1f".format(java.util.Locale.US, p.lineHeight), p.lineHeight, 1.0f..2.4f, 13) {
            prefs.update { s -> s.copy(lineHeight = (it * 10f).toInt() / 10f) }
        }
        SliderRow(stringResource(R.string.margins), "${p.margin}", p.margin.toFloat(), 0f..72f, 17) {
            prefs.update { s -> s.copy(margin = (it / 4f).toInt() * 4) }
        }

        // ---- alignment
        Label(stringResource(R.string.alignment))
        val aligns = listOf(Aligns.DEFAULT to R.string.align_default, Aligns.START to R.string.align_start, Aligns.JUSTIFY to R.string.align_justify)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            aligns.forEachIndexed { i, (key, label) ->
                SegmentedButton(
                    selected = p.align == key,
                    onClick = { prefs.update { it.copy(align = key) } },
                    shape = SegmentedButtonDefaults.itemShape(i, aligns.size),
                ) { Text(stringResource(label), maxLines = 1) }
            }
        }

        Spacer(Modifier.height(8.dp))
        SwitchRow(stringResource(R.string.hyphenation), p.hyphenate) { v -> prefs.update { it.copy(hyphenate = v) } }
        SwitchRow(stringResource(R.string.force_style), p.forceStyle) { v -> prefs.update { it.copy(forceStyle = v) } }
        SwitchRow(stringResource(R.string.keep_awake), p.keepScreenOn) { v -> prefs.update { it.copy(keepScreenOn = v) } }

        // ---- brightness
        Spacer(Modifier.height(4.dp))
        SwitchRow(stringResource(R.string.brightness_system), p.brightness < 0f) { v ->
            prefs.update { it.copy(brightness = if (v) -1f else 0.6f) }
        }
        if (p.brightness >= 0f) {
            SliderRow(stringResource(R.string.brightness), "${(p.brightness * 100).toInt()}%", p.brightness, 0.02f..1f, 0) {
                prefs.update { s -> s.copy(brightness = it) }
            }
        }

        if (onEditCss != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onEditCss, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.custom_css) +
                        if (p.customCss.isNotBlank()) "  ·  " + stringResource(R.string.custom_css_on) else ""
                )
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = current.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
