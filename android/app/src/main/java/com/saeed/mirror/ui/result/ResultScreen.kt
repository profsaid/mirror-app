package com.saeed.mirror.ui.result

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.saeed.mirror.data.AppViewModel
import com.saeed.mirror.model.HairstyleCatalog
import com.saeed.mirror.model.Hairstyle
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.ui.components.HeadIllustration
import com.saeed.mirror.ui.components.PrimaryButton
import com.saeed.mirror.ui.components.card
import com.saeed.mirror.ui.theme.MirrorColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

private sealed interface Phase {
    data object Loading : Phase
    data object Ready : Phase
    data class Failed(val message: String) : Phase
}

@Composable
fun ResultScreen(model: AppViewModel, styleId: String, lookId: String?, onBack: () -> Unit) {
    val style = HairstyleCatalog.style(styleId)
    if (style == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Style not found") }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedLook = lookId?.let { model.look(it) }

    var results by remember { mutableStateOf(savedLook?.let { model.images(it) } ?: emptyMap()) }
    var phase by remember { mutableStateOf<Phase>(if (savedLook != null) Phase.Ready else Phase.Loading) }
    var savedId by remember { mutableStateOf(savedLook?.id) }
    var angle by remember { mutableStateOf(HeadAngle.FRONT) }
    var showBefore by remember { mutableStateOf(false) }

    suspend fun generate() {
        phase = Phase.Loading
        savedId = null
        phase = try {
            results = model.tryOn.apply(style, model.scan.toMap())
            Phase.Ready
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Phase.Failed(e.message ?: "Something went wrong. Try again.")
        }
    }

    if (savedLook == null) {
        LaunchedEffect(style.id) { generate() }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MirrorColors.Accent)
            }
            Text(
                style.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            if (savedLook == null) {
                IconButton(onClick = { scope.launch { generate() } }, enabled = phase != Phase.Loading) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Try again", tint = MirrorColors.Accent)
                }
            } else {
                Box(Modifier.width(48.dp))
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Image
            Box(Modifier.fillMaxWidth().height(420.dp).card(24.dp), contentAlignment = Alignment.Center) {
                when (val p = phase) {
                    Phase.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CircularProgressIndicator(color = MirrorColors.Accent)
                        Text("Trying on ${style.name}…", style = MaterialTheme.typography.bodyMedium, color = MirrorColors.Secondary)
                    }
                    is Phase.Failed -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = MirrorColors.Secondary, modifier = Modifier.size(40.dp))
                        Text(p.message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { scope.launch { generate() } }) { Text("Try again", color = MirrorColors.Accent) }
                    }
                    Phase.Ready -> {
                        val image = if (showBefore) model.scan[angle] else results[angle]
                        if (image != null) {
                            Image(
                                image.asImageBitmap(),
                                contentDescription = if (showBefore) "Current hair, ${angle.title}" else "${style.name}, ${angle.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            HeadIllustration(
                                angle = angle,
                                style = if (showBefore) null else style,
                                hairColor = if (showBefore) Color(0xFF8E8E93) else MirrorColors.Accent,
                                modifier = Modifier.padding(40.dp)
                            )
                        }
                        Row(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC1C1C1E))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(13.dp))
                            Text("AI preview", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC000000))
                                .padding(3.dp)
                        ) {
                            ToggleSegment("Before", showBefore) { showBefore = true }
                            ToggleSegment("After", !showBefore) { showBefore = false }
                        }
                    }
                }
            }

            // Angle segmented control
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(MirrorColors.Card)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                HeadAngle.entries.forEach { a ->
                    val active = a == angle
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (active) MirrorColors.Tertiary else Color.Transparent)
                            .selectable(selected = active, role = Role.Tab) { angle = a },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(a.title, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }

            // Barber notes
            Column(Modifier.fillMaxWidth().card(16.dp).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FOR YOUR BARBER", style = MaterialTheme.typography.bodySmall, color = MirrorColors.Secondary)
                style.notes.forEach { note ->
                    Row {
                        Text(note.label, color = MirrorColors.Secondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
                        Text(note.value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val id = savedId
                        val existing = id?.let { model.look(it) }
                        if (existing != null) {
                            model.delete(existing)
                            savedId = null
                        } else {
                            savedId = model.save(results, style).id
                        }
                    },
                    enabled = phase == Phase.Ready && results.isNotEmpty(),
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(MirrorColors.Card)
                ) {
                    if (savedId != null) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Remove from saved", tint = MirrorColors.Red)
                    } else {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Save look", tint = Color.White)
                    }
                }
                PrimaryButton(
                    title = "Send to my barber",
                    icon = Icons.Outlined.Share,
                    enabled = phase == Phase.Ready && results.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { share(context, style, results) }
            }
        }
    }
}

@Composable
private fun ToggleSegment(title: String, isOn: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (isOn) Color.White else Color.Transparent)
            .selectable(selected = isOn, role = Role.Tab, onClick = onClick)
            .heightIn(min = 30.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = if (isOn) Color.Black else Color.White)
    }
}

/** Shares the three photos plus the barber notes through the system share sheet. */
private fun share(context: Context, style: Hairstyle, images: Map<HeadAngle, Bitmap>) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val uris = ArrayList<Uri>()
    HeadAngle.entries.forEach { angle ->
        val bitmap = images[angle] ?: return@forEach
        val file = File(dir, "${style.id}-${angle.key}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
    }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/jpeg"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        putExtra(Intent.EXTRA_SUBJECT, style.name)
        putExtra(Intent.EXTRA_TEXT, style.barberText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Send to my barber"))
}
