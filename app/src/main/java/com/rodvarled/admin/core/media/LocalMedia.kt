package com.rodvarled.admin.core.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.rodvarled.admin.core.util.compressToJpegFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class MediaKind(val api: String) {
    PHOTO("Foto"),
    VIDEO("Video");

    companion object {
        fun fromApi(value: String?): MediaKind = entries.firstOrNull { it.api.equals(value, ignoreCase = true) } ?: PHOTO
    }
}

/** Momento del trabajo al que pertenece un archivo. `api` es el valor que entiende el servidor (sin acentos). */
enum class MediaStage(val api: String, val label: String) {
    BEFORE("Antes", "Antes"),
    DURING("Durante", "Durante"),
    AFTER("Despues", "Después"),
    OTHER("Otra", "Otra");

    /** Siguiente etapa al tocar la etiqueta: Antes → Durante → Después → Otra → Antes. */
    fun next(): MediaStage = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromApi(value: String?): MediaStage = entries.firstOrNull { it.api.equals(value, ignoreCase = true) } ?: OTHER
    }
}

/**
 * Foto o video ya guardado en el teléfono, listo para subirse cuando exista la instalación.
 * `id` también viaja al servidor como `ClientId`: si una subida se reintenta, el servidor no duplica el archivo.
 */
data class LocalMedia(
    val id: String,
    val path: String,
    val kind: MediaKind,
    val stage: MediaStage,
    val sizeBytes: Long,
    val durationSeconds: Int? = null
) {
    val file: File get() = File(path)
}

/** Copia a la carpeta privada de la app las fotos y videos que el usuario toma o elige. */
object MediaFiles {

    /** El servidor acepta hasta 120 MB; se deja margen por el empaquetado del formulario. */
    const val MAX_VIDEO_BYTES = 100L * 1024 * 1024

    fun pendingDir(context: Context): File = File(context.filesDir, "pending_media").apply { mkdirs() }

    /** Reduce y guarda una foto. Falla con un mensaje legible si no se pudo leer. */
    suspend fun importPhoto(context: Context, uri: Uri, stage: MediaStage = MediaStage.OTHER): Result<LocalMedia> =
        withContext(Dispatchers.IO) {
            runCatching {
                val id = UUID.randomUUID().toString()
                val file = File(pendingDir(context), "$id.jpg")
                if (!uri.compressToJpegFile(context, file)) {
                    file.delete()
                    error("No se pudo leer la foto.")
                }
                LocalMedia(id, file.absolutePath, MediaKind.PHOTO, stage, file.length())
            }
        }

    /** Guarda un video (con tope de tamaño) y lee su duración. */
    suspend fun importVideo(context: Context, uri: Uri, stage: MediaStage = MediaStage.OTHER): Result<LocalMedia> =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val file = File(pendingDir(context), "$id.mp4")
            runCatching {
                val input = context.contentResolver.openInputStream(uri) ?: error("No se pudo abrir el video.")
                input.use { source ->
                    file.outputStream().use { target ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val read = source.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_VIDEO_BYTES) error("El video pesa más de 100 MB. Graba uno más corto.")
                            target.write(buffer, 0, read)
                        }
                    }
                }
                if (file.length() == 0L) error("El video está vacío.")

                LocalMedia(id, file.absolutePath, MediaKind.VIDEO, stage, file.length(), readDurationSeconds(file))
            }.onFailure { file.delete() }
        }

    fun discard(media: LocalMedia) {
        runCatching { media.file.delete() }
    }

    private fun readDurationSeconds(file: File): Int? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { (it / 1000).toInt() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
