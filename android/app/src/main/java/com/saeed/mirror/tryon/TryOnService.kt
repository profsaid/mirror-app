package com.saeed.mirror.tryon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Base64
import com.saeed.mirror.Config
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.model.Hairstyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Turns the three scan photos into three photos wearing the style, in one request. */
interface TryOnService {
    suspend fun apply(style: Hairstyle, images: Map<HeadAngle, Bitmap>): Map<HeadAngle, Bitmap>
}

object TryOnServiceFactory {
    fun make(): TryOnService = Config.tryOnEndpoint?.let { RemoteTryOnService(it) } ?: MockTryOnService()
}

class TryOnException(message: String) : Exception(message)

/** Same prompt as the iOS app, so both talk to the backend identically. */
object TryOnPrompt {
    fun make(style: Hairstyle): String =
        "Edit only the hair of the person in these three photos of the same head " +
            "(front, right side, left side). Give them ${style.description}. " +
            "The haircut must look identical across all three angles. " +
            "Keep the face, identity, skin, expression, ears, beard, clothing, lighting, " +
            "background and camera angle exactly as they are. Photorealistic, no illustration."
}

fun Bitmap.scaledToMax(maxSide: Int): Bitmap {
    val longest = maxOf(width, height)
    if (longest <= maxSide) return this
    val scale = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}

/** Returns the original photos with a label after a short delay, so the flow works without a backend. */
class MockTryOnService : TryOnService {
    override suspend fun apply(style: Hairstyle, images: Map<HeadAngle, Bitmap>): Map<HeadAngle, Bitmap> {
        if (!HeadAngle.entries.all { images.containsKey(it) }) throw TryOnException("Take all three photos first.")
        delay(1500)
        return withContext(Dispatchers.Default) {
            images.mapValues { (_, bitmap) -> stamp(bitmap.scaledToMax(1024), "Preview · ${style.name}") }
        }
    }

    private fun stamp(source: Bitmap, text: String): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val textSize = maxOf(18f, bitmap.width * 0.04f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val padding = textSize * 0.6f
        val width = textPaint.measureText(text)
        val badge = RectF(padding, bitmap.height - textSize - padding * 4, padding * 3 + width, bitmap.height - padding * 2)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF9F0A.toInt() }
        canvas.drawRoundRect(badge, badge.height() / 2, badge.height() / 2, badgePaint)
        canvas.drawText(text, badge.left + padding, badge.bottom - padding * 0.9f, textPaint)
        return bitmap
    }
}

/**
 * Your backend, which holds the AI key. Same contract as the iOS app:
 * POST { "styleId", "prompt", "images": { "front", "right", "left" } } → { "images": { ... } } (base64 JPEG).
 */
class RemoteTryOnService(private val endpoint: String) : TryOnService {
    override suspend fun apply(style: Hairstyle, images: Map<HeadAngle, Bitmap>): Map<HeadAngle, Bitmap> =
        withContext(Dispatchers.IO) {
            if (!HeadAngle.entries.all { images.containsKey(it) }) throw TryOnException("Take all three photos first.")

            val encoded = JSONObject()
            images.forEach { (angle, bitmap) ->
                val bytes = ByteArrayOutputStream().also {
                    bitmap.scaledToMax(1024).compress(Bitmap.CompressFormat.JPEG, 85, it)
                }.toByteArray()
                encoded.put(angle.key, Base64.encodeToString(bytes, Base64.NO_WRAP))
            }
            val body = JSONObject()
                .put("styleId", style.id)
                .put("prompt", TryOnPrompt.make(style))
                .put("images", encoded)
                .toString()

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            try {
                connection.outputStream.use { it.write(body.toByteArray()) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw TryOnException("The try-on server answered with an error ($code). Try again in a moment.")
                }
                val json = JSONObject(connection.inputStream.bufferedReader().readText()).getJSONObject("images")
                HeadAngle.entries.associateWith { angle ->
                    val bytes = Base64.decode(json.optString(angle.key), Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw TryOnException("The ${angle.label.lowercase()} photo came back damaged. Try again.")
                }
            } finally {
                connection.disconnect()
            }
        }
}
