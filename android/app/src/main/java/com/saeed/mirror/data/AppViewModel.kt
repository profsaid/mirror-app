package com.saeed.mirror.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.model.Hairstyle
import com.saeed.mirror.model.SavedLook
import com.saeed.mirror.tryon.TryOnService
import com.saeed.mirror.tryon.TryOnServiceFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** App state: the head scan and saved looks. Everything stays on the phone. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dir = File(app.filesDir, "mirror").apply { mkdirs() }
    private val indexFile = File(dir, "saved.json")

    val scan = mutableStateMapOf<HeadAngle, Bitmap>()
    val saved = mutableStateListOf<SavedLook>()

    val tryOn: TryOnService = TryOnServiceFactory.make()

    val hasCompleteScan: Boolean get() = HeadAngle.entries.all { scan.containsKey(it) }

    init {
        HeadAngle.entries.forEach { angle -> load(scanFile(angle))?.let { scan[angle] = it } }
        readIndex()
    }

    fun replaceScan(images: Map<HeadAngle, Bitmap>) {
        scan.clear()
        scan.putAll(images)
        images.forEach { (angle, bitmap) -> write(bitmap, scanFile(angle)) }
    }

    fun save(results: Map<HeadAngle, Bitmap>, style: Hairstyle): SavedLook {
        val look = SavedLook(UUID.randomUUID().toString(), style.id, System.currentTimeMillis())
        results.forEach { (angle, bitmap) -> write(bitmap, File(dir, look.fileName(angle))) }
        saved.add(0, look)
        writeIndex()
        return look
    }

    fun delete(look: SavedLook) {
        HeadAngle.entries.forEach { File(dir, look.fileName(it)).delete() }
        saved.removeAll { it.id == look.id }
        writeIndex()
    }

    fun images(look: SavedLook): Map<HeadAngle, Bitmap> =
        HeadAngle.entries.mapNotNull { angle -> load(File(dir, look.fileName(angle)))?.let { angle to it } }.toMap()

    fun look(id: String): SavedLook? = saved.firstOrNull { it.id == id }

    private fun scanFile(angle: HeadAngle) = File(dir, "scan-${angle.key}.jpg")

    private fun write(bitmap: Bitmap, file: File) {
        runCatching { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) } }
    }

    private fun load(file: File): Bitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

    private fun readIndex() {
        if (!indexFile.exists()) return
        runCatching {
            val array = JSONArray(indexFile.readText())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                saved.add(SavedLook(o.getString("id"), o.getString("styleId"), o.getLong("createdAt")))
            }
        }
    }

    private fun writeIndex() {
        val array = JSONArray()
        saved.forEach {
            array.put(JSONObject().put("id", it.id).put("styleId", it.styleId).put("createdAt", it.createdAt))
        }
        runCatching { indexFile.writeText(array.toString()) }
    }
}
