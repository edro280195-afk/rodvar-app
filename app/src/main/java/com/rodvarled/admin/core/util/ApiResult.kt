package com.rodvarled.admin.core.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.IOException
import retrofit2.HttpException
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    Result.failure(e)
}

/** Extrae un mensaje legible de una excepción de red, priorizando el `{ "message": "..." }` que manda la API. */
fun Throwable.toUserMessage(): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            runCatching {
                val json = Json { ignoreUnknownKeys = true }
                json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
            }.getOrNull()?.let { return it }
        }
        return when (code()) {
            401 -> "Sesión expirada. Inicia sesión de nuevo."
            403 -> "No tienes permiso para hacer esto."
            404 -> "No se encontró la información solicitada."
            429 -> "Demasiados intentos. Espera un momento."
            in 500..599 -> "Error del servidor. Intenta de nuevo en unos minutos."
            else -> "Ocurrió un error inesperado (${code()})."
        }
    }
    if (this is SocketTimeoutException) return "El servidor tardó demasiado en responder. Revisa tu conexión."
    if (this is IOException) return "Sin conexión a internet. Revisa tu señal."
    return message ?: "Ocurrió un error inesperado."
}
