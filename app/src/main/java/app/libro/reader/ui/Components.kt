package app.libro.reader.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun rememberCover(file: File?, targetWidth: Int = 420): Bitmap? {
    val bmp by produceState<Bitmap?>(null, file?.path, file?.lastModified()) {
        value = if (file == null) null else withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            } catch (e: Exception) {
                null
            }
        }
    }
    return bmp
}

private val coverTints = listOf(
    Color(0xFF2F5D62), Color(0xFF7A4A22), Color(0xFF5E6B3D), Color(0xFF6B3F5E),
    Color(0xFF34506E), Color(0xFF8A3B3B), Color(0xFF4A4E69), Color(0xFF3D6B4F),
)

@Composable
fun BookCover(
    title: String,
    author: String,
    coverFile: File?,
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp,
) {
    val bmp = rememberCover(coverFile)
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val tint = coverTints[(title.hashCode() and 0x7fffffff) % coverTints.size]
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.78f))))
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    author,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ChoiceCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        content()
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

@Composable
fun ThemeSwatch(bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("Aa", color = fg, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SettingRow(label: String, trailing: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}
