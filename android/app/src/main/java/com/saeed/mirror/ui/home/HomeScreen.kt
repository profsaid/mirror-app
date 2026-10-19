package com.saeed.mirror.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.saeed.mirror.data.AppViewModel
import com.saeed.mirror.model.HairstyleCatalog
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.model.SavedLook
import com.saeed.mirror.ui.components.AngleThumbnail
import com.saeed.mirror.ui.components.HeadIllustration
import com.saeed.mirror.ui.components.PrimaryButton
import com.saeed.mirror.ui.components.card
import com.saeed.mirror.ui.theme.MirrorColors
import androidx.compose.foundation.background

@Composable
fun HomeScreen(
    model: AppViewModel,
    onScan: () -> Unit,
    onOpenLook: (SavedLook) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Mirror", style = MaterialTheme.typography.displaySmall)
            Text(
                "See a haircut from the front and both sides before the barber picks up the clippers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MirrorColors.Secondary
            )
        }

        // Scan card
        Column(Modifier.card(20.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your head scan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (model.hasCompleteScan) {
                    Icon(Icons.Filled.Check, null, tint = MirrorColors.Green, modifier = Modifier.size(14.dp))
                    Text(" 3 of 3 angles", style = MaterialTheme.typography.bodySmall, color = MirrorColors.Green)
                } else {
                    Text("Not scanned yet", style = MaterialTheme.typography.bodySmall, color = MirrorColors.Secondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeadAngle.entries.forEach { angle ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        AngleThumbnail(angle, model.scan[angle])
                        Spacer(Modifier.padding(top = 6.dp))
                        Text(angle.label, style = MaterialTheme.typography.bodySmall, color = MirrorColors.Secondary)
                    }
                }
            }
            PrimaryButton(
                title = if (model.hasCompleteScan) "New scan" else "Start scan",
                icon = Icons.Outlined.PhotoCamera,
                onClick = onScan
            )
        }

        // Recent
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent try-ons", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (model.saved.isNotEmpty()) {
                    TextButton(onClick = onSeeAll) { Text("See all", color = MirrorColors.Accent) }
                }
            }
            if (model.saved.isEmpty()) {
                Text(
                    "Looks you save show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MirrorColors.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .card(14.dp)
                        .padding(vertical = 22.dp, horizontal = 16.dp)
                )
            } else {
                val recent = model.saved.take(3)
                Column(Modifier.fillMaxWidth().card(14.dp)) {
                    recent.forEachIndexed { index, look ->
                        RecentRow(look) { onOpenLook(look) }
                        if (index < recent.lastIndex) {
                            HorizontalDivider(color = MirrorColors.Separator, modifier = Modifier.padding(start = 76.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRow(look: SavedLook, onClick: () -> Unit) {
    val style = HairstyleCatalog.style(look.styleId)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeadIllustration(
            angle = HeadAngle.FRONT,
            style = style,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MirrorColors.Fill)
        )
        Column(Modifier.weight(1f)) {
            Text(style?.name ?: "Look", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text(
                DateUtils.getRelativeTimeSpanString(look.createdAt).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MirrorColors.Secondary
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MirrorColors.Tertiary)
    }
}
