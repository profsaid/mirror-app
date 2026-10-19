package com.saeed.mirror.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saeed.mirror.model.Hairstyle
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.ui.theme.MirrorColors

/** Full-width filled button, 50 dp tall. */
@Composable
fun PrimaryButton(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MirrorColors.Accent, contentColor = Color.Black)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** 44 dp round icon button on the card fill. */
@Composable
fun CircleIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = MirrorColors.Card, contentColor = Color.White)
    ) {
        Icon(icon, contentDescription = label)
    }
}

/** Rounded thumbnail: the photo when there is one, the illustration otherwise. */
@Composable
fun AngleThumbnail(
    angle: HeadAngle,
    image: Bitmap?,
    modifier: Modifier = Modifier,
    style: Hairstyle? = null,
    height: Dp = 120.dp,
    corner: Dp = 12.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(MirrorColors.Fill),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "${angle.title} photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(height)
            )
        } else {
            HeadIllustration(
                angle = angle,
                style = style,
                hairColor = if (style == null) Color(0xFF8E8E93) else MirrorColors.Accent,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** Card surface used across screens. */
fun Modifier.card(corner: Dp = 20.dp): Modifier =
    this.clip(RoundedCornerShape(corner)).background(MirrorColors.Card)

/** Capsule label like the status pills in the mockups. */
@Composable
fun Pill(text: String, dot: Color? = null) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(MirrorColors.Card)
            .padding(horizontal = 12.dp)
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
fun SpacerW(width: Dp) = Box(Modifier.width(width))
