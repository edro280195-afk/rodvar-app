package com.rodvarled.admin.core.media

import com.rodvarled.admin.BuildConfig
import com.rodvarled.admin.data.remote.dto.InstallationMedia
import com.rodvarled.admin.di.AuthenticatedClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Falló la subida. `retryable` indica si tiene sentido volver a intentarlo más tarde (red, servidor caído, sesión). */
class MediaUploadException(val code: Int?, override val message: String, val retryable: Boolean) : Exception(message)

/**
 * Sube una foto o un video a la instalación como formulario multipart (el servidor lo pasa a Cloudinary
 * como flujo). Usa un cliente con tiempos largos: un video de decenas de MB puede tardar minutos en móvil.
 */
@Singleton
class MediaUploader @Inject constructor(
    @AuthenticatedClient baseClient: OkHttpClient,
    private val json: Json
) {
    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES)
        .build()

    suspend fun upload(
        installationId: Int,
        file: File,
        kind: MediaKind,
        stage: MediaStage,
        clientId: String,
        title: String? = null
    ): InstallationMedia = withContext(Dispatchers.IO) {
        if (!file.exists()) throw MediaUploadException(null, "El archivo ya no está en el teléfono.", retryable = false)

        val contentType = (if (kind == MediaKind.VIDEO) "video/mp4" else "image/jpeg").toMediaType()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("Kind", kind.api)
            .addFormDataPart("Stage", stage.api)
            .addFormDataPart("ClientId", clientId)
            .apply { if (!title.isNullOrBlank()) addFormDataPart("Title", title) }
            .addFormDataPart("File", file.name, file.asRequestBody(contentType))
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}api/Installations/$installationId/media")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw MediaUploadException(response.code, errorMessage(text, response.code), retryable = isRetryable(response.code))
                }
                json.decodeFromString(InstallationMedia.serializer(), text)
            }
        } catch (e: MediaUploadException) {
            throw e
        } catch (e: java.io.IOException) {
            // Sin señal, tiempo agotado o servidor dormido: se reintenta solo.
            throw MediaUploadException(null, "Sin conexión con el servidor.", retryable = true)
        }
    }

    private fun isRetryable(code: Int): Boolean = code == 401 || code == 408 || code == 429 || code >= 500

    // El servidor responde { "message": "..." }; si no, se usa un texto genérico según el código.
    private fun errorMessage(body: String, code: Int): String {
        val fromServer = runCatching { json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
        return fromServer ?: when (code) {
            401 -> "La sesión expiró."
            413 -> "El archivo es demasiado grande."
            in 500..599 -> "El servidor no pudo guardar el archivo."
            else -> "No se pudo subir el archivo ($code)."
        }
    }
}
