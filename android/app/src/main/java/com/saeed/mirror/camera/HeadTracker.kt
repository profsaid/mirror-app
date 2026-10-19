package com.saeed.mirror.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.saeed.mirror.Config
import com.saeed.mirror.model.HeadAngle
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

sealed interface TrackStatus {
    val message: String
    val isAligned: Boolean get() = false

    data object Starting : TrackStatus { override val message = "Starting camera…" }
    data object Unavailable : TrackStatus { override val message = "Camera isn't available here" }
    data object Denied : TrackStatus { override val message = "Camera access is off" }
    data object NoFace : TrackStatus { override val message = "Line your face up with the outline" }
    data class Adjust(override val message: String) : TrackStatus
    data object HoldStill : TrackStatus {
        override val message = "Hold still…"
        override val isAligned = true
    }
}

/**
 * Front camera head-pose tracking with ML Kit. Publishes whether the head is lined up for
 * [target] and calls [onReady] once it has held still long enough. Same rules as the iOS app.
 */
class HeadTracker {
    var status by mutableStateOf<TrackStatus>(TrackStatus.Starting)
    var holdProgress by mutableFloatStateOf(0f)
        private set

    var onReady: (() -> Unit)? = null

    var target: HeadAngle = HeadAngle.FRONT
        set(value) { field = value; reset() }

    var paused: Boolean = false
        set(value) { field = value; reset() }

    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    private val executor = Executors.newSingleThreadExecutor()
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    val analysis: ImageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { it.setAnalyzer(executor) { proxy -> analyze(proxy) } }

    private var alignedSince = 0L
    private var requested = false

    private fun reset() {
        alignedSince = 0L
        requested = false
        holdProgress = 0f
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (paused || media == null) {
            proxy.close()
            return
        }
        val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        // ML Kit delivers results on the main thread, where Compose state can be updated.
        detector.process(input)
            .addOnSuccessListener { faces ->
                val face = faces.maxByOrNull { it.boundingBox.width() }
                handle(face?.headEulerAngleY?.times(Config.YAW_SIGN))
            }
            .addOnFailureListener { handle(null) }
            .addOnCompleteListener { proxy.close() }
    }

    private fun handle(yaw: Float?) {
        if (paused) return
        val next = evaluate(yaw)
        status = next
        if (!next.isAligned) {
            reset()
            return
        }
        val now = System.currentTimeMillis()
        if (alignedSince == 0L) alignedSince = now
        holdProgress = ((now - alignedSince).toFloat() / Config.HOLD_STILL_MS).coerceAtMost(1f)
        if (holdProgress >= 1f && !requested) {
            requested = true
            onReady?.invoke()
        }
    }

    private fun evaluate(yaw: Float?): TrackStatus {
        if (yaw == null) return TrackStatus.NoFace
        val threshold = Config.SIDE_YAW_THRESHOLD
        return when (target) {
            HeadAngle.FRONT ->
                if (abs(yaw) <= Config.FRONT_YAW_TOLERANCE) TrackStatus.HoldStill
                else TrackStatus.Adjust("Face the camera straight on")
            HeadAngle.RIGHT -> when {
                yaw >= threshold -> TrackStatus.HoldStill
                yaw < -10 -> TrackStatus.Adjust("Other way: turn left")
                else -> TrackStatus.Adjust("Keep turning left")
            }
            HeadAngle.LEFT -> when {
                yaw <= -threshold -> TrackStatus.HoldStill
                yaw > 10 -> TrackStatus.Adjust("Other way: turn right")
                else -> TrackStatus.Adjust("Keep turning right")
            }
        }
    }

    suspend fun takePhoto(context: Context): Bitmap = suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap().rotated(image.imageInfo.rotationDegrees)
                    image.close()
                    if (continuation.isActive) continuation.resume(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }
        )
    }

    fun close() {
        detector.close()
        executor.shutdown()
    }
}

fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
