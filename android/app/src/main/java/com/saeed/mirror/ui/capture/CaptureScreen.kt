package com.saeed.mirror.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saeed.mirror.camera.HeadTracker
import com.saeed.mirror.camera.TrackStatus
import com.saeed.mirror.data.AppViewModel
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.tryon.scaledToMax
import com.saeed.mirror.ui.components.CircleIconButton
import com.saeed.mirror.ui.components.HeadIllustration
import com.saeed.mirror.ui.components.Pill
import com.saeed.mirror.ui.components.PrimaryButton
import com.saeed.mirror.ui.theme.MirrorColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Guided three-angle scan: front, right side, left side. */
@Composable
fun CaptureScreen(model: AppViewModel, onClose: () -> Unit, onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val tracker = remember { HeadTracker() }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var step by remember { mutableStateOf(HeadAngle.FRONT) }
    val captured = remember { mutableStateMapOf<HeadAngle, Bitmap>() }
    var shooting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isDone = HeadAngle.entries.all { captured.containsKey(it) }
    val cameraUsable = hasPermission && tracker.status != TrackStatus.Unavailable

    fun store(photo: Bitmap) {
        captured[step] = photo.scaledToMax(1600)
        val next = HeadAngle.entries.firstOrNull { !captured.containsKey(it) }
        if (next == null) {
            tracker.paused = true
        } else {
            scope.launch {
                delay(600)
                step = next
            }
        }
    }

    fun shoot() {
        // Read state fresh: this runs from the tracker callback, long after the first composition.
        val usable = hasPermission && tracker.status != TrackStatus.Unavailable
        if (!usable || shooting || captured.containsKey(step)) return
        shooting = true
        scope.launch {
            try {
                store(tracker.takePhoto(context))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Couldn't take the photo. Try again."
                tracker.target = step
            } finally {
                shooting = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) tracker.status = TrackStatus.Denied
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bitmap != null) store(bitmap) else error = "That photo couldn't be opened."
        }
    }
    fun pick() = pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    LaunchedEffect(Unit) {
        tracker.onReady = { shoot() }
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(step) { tracker.target = step }
    DisposableEffect(Unit) { onDispose { tracker.close() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Filled.Close, "Close", onClose)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.clip(CircleShape).background(MirrorColors.Card).padding(horizontal = 14.dp).height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isDone) "All set" else "Step ${step.ordinal + 1} of 3",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (cameraUsable) {
                Spacer(Modifier.size(44.dp))
            } else {
                CircleIconButton(Icons.Outlined.Photo, "Choose a photo") { pick() }
            }
        }

        // Viewport
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E0E10))
                .border(BorderStroke(0.5.dp, MirrorColors.Fill), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            val photo = captured[step]
            when {
                isDone -> DoneGrid(captured)
                photo != null -> Image(photo.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                cameraUsable -> {
                    AndroidView(
                        factory = { ctx ->
                            val view = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        tracker.imageCapture,
                                        tracker.analysis
                                    )
                                    tracker.status = TrackStatus.NoFace
                                } catch (e: Exception) {
                                    tracker.status = TrackStatus.Unavailable
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            view
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    GuideOval(tracker.status.isAligned, tracker.holdProgress)
                }
                else -> NoCamera(step, denied = !hasPermission, onPick = { pick() }, onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                })
            }

            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 20.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (isDone) "All 3 angles captured" else step.instruction,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.shadow(0.dp)
                )
                Text(
                    if (isDone) "Your scan is ready. Pick a haircut to try on." else step.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                val text = when {
                    isDone -> "Scan saved on this phone"
                    photo != null -> "Photo taken"
                    else -> tracker.status.message
                }
                Pill(text, dot = if (isDone || tracker.status.isAligned) MirrorColors.Green else MirrorColors.Accent)
            }
        }

        // Angle tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeadAngle.entries.forEach { angle ->
                val active = !isDone && angle == step
                val done = captured.containsKey(angle)
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MirrorColors.Card)
                        .border(1.5.dp, if (active) MirrorColors.Accent else Color.Transparent, RoundedCornerShape(12.dp))
                        .selectable(selected = active, role = Role.Tab) { step = angle },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (done) Icon(Icons.Filled.Check, null, tint = MirrorColors.Green, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                    Text(
                        angle.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active || done) Color.White else MirrorColors.Secondary
                    )
                }
            }
        }

        // Bottom controls
        if (isDone) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PrimaryButton("Choose a style", Icons.Outlined.ContentCut) {
                    model.replaceScan(captured.toMap())
                    onFinished()
                }
                TextButton(onClick = {
                    captured.clear()
                    step = HeadAngle.FRONT
                    tracker.paused = false
                }) { Text("Retake all", color = MirrorColors.Accent) }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    if (cameraUsable) "Takes the photo automatically when you're lined up, or tap the shutter."
                    else "No camera here. Pick a photo for each angle instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MirrorColors.Secondary,
                    textAlign = TextAlign.Center
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(64.dp)) {
                        val previous = HeadAngle.entries.lastOrNull { captured.containsKey(it) && it != step }
                        val thumb = previous?.let { captured[it] }
                        if (thumb != null) {
                            Image(
                                thumb.asImageBitmap(), null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 48.dp, height = 58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                            )
                        }
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val enabled = cameraUsable && !shooting && !captured.containsKey(step)
                        Box(
                            Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White.copy(alpha = if (enabled) 1f else 0.3f), CircleShape)
                                .selectable(selected = false, enabled = enabled, role = Role.Button) { shoot() }
                                .semantics { contentDescription = "Take photo" }
                                .padding(9.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (enabled) 1f else 0.3f))
                        )
                    }
                    Box(Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
                        TextButton(
                            onClick = {
                                captured.remove(step)
                                tracker.paused = false
                                tracker.target = step
                            },
                            enabled = captured.containsKey(step)
                        ) { Text("Retake", color = if (captured.containsKey(step)) MirrorColors.Accent else MirrorColors.Tertiary) }
                    }
                }
            }
        }
    }

    error?.let { message ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
            title = { Text("Couldn't take the photo") },
            text = { Text(message) },
            containerColor = MirrorColors.Card
        )
    }
}

@Composable
private fun GuideOval(aligned: Boolean, progress: Float) {
    Canvas(Modifier.size(width = 230.dp, height = 300.dp).offset(y = 30.dp)) {
        val color = if (aligned) MirrorColors.Green else MirrorColors.Accent
        drawOval(
            color = color,
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = if (aligned) null else PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
            )
        )
        if (progress > 0f) {
            drawArc(
                color = MirrorColors.Green,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun NoCamera(step: HeadAngle, denied: Boolean, onPick: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeadIllustration(step, null, Modifier.height(240.dp), hairColor = Color(0xFF48484A))
        OutlinedButton(onClick = onPick) {
            Icon(Icons.Outlined.Photo, null, tint = Color.White)
            Text("  Choose ${step.label.lowercase()} photo", color = Color.White)
        }
        if (denied) {
            TextButton(onClick = onOpenSettings) { Text("Turn on camera in Settings", color = MirrorColors.Accent) }
        }
    }
}

@Composable
private fun DoneGrid(captured: Map<HeadAngle, Bitmap>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 60.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HeadAngle.entries.forEach { angle ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                captured[angle]?.let {
                    Image(
                        it.asImageBitmap(), "${angle.title} photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                Text(angle.label, style = MaterialTheme.typography.bodySmall, color = MirrorColors.Secondary)
            }
        }
    }
}
