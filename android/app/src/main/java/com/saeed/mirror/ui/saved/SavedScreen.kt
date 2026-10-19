package com.saeed.mirror.ui.saved

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saeed.mirror.data.AppViewModel
import com.saeed.mirror.model.HairstyleCatalog
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.model.SavedLook
import com.saeed.mirror.ui.components.AngleThumbnail
import com.saeed.mirror.ui.components.card
import com.saeed.mirror.ui.theme.MirrorColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedScreen(model: AppViewModel, onOpenLook: (SavedLook) -> Unit, onBrowse: () -> Unit) {
    var pendingDelete by remember { mutableStateOf<SavedLook?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Saved", style = MaterialTheme.typography.displaySmall)
            if (model.saved.isNotEmpty()) {
                Text("Show any of these to your barber. Long-press to delete.", style = MaterialTheme.typography.bodyMedium, color = MirrorColors.Secondary)
            }
        }

        if (model.saved.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.FavoriteBorder, null, tint = MirrorColors.Secondary, modifier = Modifier.size(48.dp))
                Text("No saved looks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                Text(
                    "Tap the heart on a try-on to keep it here and show it to your barber.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MirrorColors.Secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                )
                Button(
                    onClick = onBrowse,
                    colors = ButtonDefaults.buttonColors(containerColor = MirrorColors.Accent, contentColor = Color.Black)
                ) { Text("Browse styles") }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(model.saved, key = { it.id }) { look ->
                    val images = remember(look.id) { model.images(look) }
                    val style = HairstyleCatalog.style(look.styleId)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .card(18.dp)
                            .combinedClickable(onClick = { onOpenLook(look) }, onLongClick = { pendingDelete = look })
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(style?.name ?: "Look", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(look.createdAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MirrorColors.Secondary
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MirrorColors.Tertiary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeadAngle.entries.forEach { angle ->
                                Box(Modifier.weight(1f)) {
                                    AngleThumbnail(angle, images[angle], style = style, height = 96.dp, corner = 10.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { look ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this look?") },
            text = { Text("The three photos will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = { model.delete(look); pendingDelete = null }) {
                    Text("Delete", color = MirrorColors.Red)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            containerColor = MirrorColors.Card
        )
    }
}
