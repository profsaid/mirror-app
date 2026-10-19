package com.saeed.mirror.ui.styles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.saeed.mirror.data.AppViewModel
import com.saeed.mirror.model.HairLength
import com.saeed.mirror.model.HairstyleCatalog
import com.saeed.mirror.model.Hairstyle
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.ui.components.HeadIllustration
import com.saeed.mirror.ui.components.PrimaryButton
import com.saeed.mirror.ui.theme.MirrorColors

@Composable
fun StylesScreen(model: AppViewModel, onTryOn: (Hairstyle) -> Unit, onScan: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf<HairLength?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf(HairstyleCatalog.all.first().id) }
    val selected = HairstyleCatalog.style(selectedId) ?: HairstyleCatalog.all.first()

    val visible = HairstyleCatalog.all.filter { style ->
        (filter == null || style.length == filter) &&
            (search.isBlank() || style.name.contains(search, ignoreCase = true) || style.tag.contains(search, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Styles", style = MaterialTheme.typography.displaySmall)
            TextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search haircuts") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MirrorColors.Card,
                    unfocusedContainerColor = MirrorColors.Card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MirrorColors.Accent,
                    focusedPlaceholderColor = MirrorColors.Secondary,
                    unfocusedPlaceholderColor = MirrorColors.Secondary,
                    focusedLeadingIconColor = MirrorColors.Secondary,
                    unfocusedLeadingIconColor = MirrorColors.Secondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        val chips: List<Pair<String, HairLength?>> = listOf("All" to null) + HairLength.entries.map { it.label to it }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chips) { (label, value) ->
                val active = filter == value
                FilterChip(
                    selected = active,
                    onClick = { filter = value },
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    shape = CircleShape,
                    border = if (active) null else BorderStroke(0.dp, Color.Transparent),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MirrorColors.Card,
                        labelColor = Color.White,
                        selectedContainerColor = MirrorColors.Accent,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(visible, key = { it.id }) { style ->
                StyleCard(style, style.id == selectedId) { selectedId = style.id }
            }
        }

        Box(Modifier.background(Color.Black).padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (model.hasCompleteScan) {
                PrimaryButton("Try on ${selected.name}", Icons.Outlined.AutoAwesome) { onTryOn(selected) }
            } else {
                PrimaryButton("Scan your head first", Icons.Outlined.PhotoCamera, onClick = onScan)
            }
        }
    }
}

@Composable
private fun StyleCard(style: Hairstyle, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .clip(shape)
            .background(MirrorColors.Card)
            .border(2.dp, if (isSelected) MirrorColors.Accent else Color.Transparent, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MirrorColors.Fill),
                contentAlignment = Alignment.Center
            ) {
                HeadIllustration(
                    angle = HeadAngle.FRONT,
                    style = style,
                    hairColor = if (isSelected) MirrorColors.Accent else Color(0xFFC7C7CC),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Column(Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp).heightIn(min = 38.dp)) {
                Text(style.name, style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(style.tag, style = MaterialTheme.typography.bodySmall, color = MirrorColors.Secondary)
            }
        }
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MirrorColors.Accent,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp)
            )
        }
    }
}
