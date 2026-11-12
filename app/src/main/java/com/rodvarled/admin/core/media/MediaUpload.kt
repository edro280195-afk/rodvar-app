package com.rodvarled.admin.core.media

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.Constraints
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_INSTALLATION_ID = "installationId"
private const val KEY_PATH = "path"
private const val KEY_KIND = "kind"
private const val KEY_STAGE = "stage"
private const val KEY_CLIENT_ID = "clientId"
private const val KEY_ERROR = "error"

/** Después de este número de intentos fallidos se deja de reintentar y se avisa al usuario. */
private const val MAX_ATTEMPTS = 8

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MediaUploadEntryPoint {
    fun uploader(): MediaUploader
}

/**
 * Sube un archivo a una instalación. WorkManager lo conserva aunque se cierre la app y lo reintenta
 * con espera creciente cuando falla la red; si todo falla, deja los datos para poder reintentarlo a mano.
 */
class MediaUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val installationId = inputData.getInt(KEY_INSTALLATION_ID, -1)
        val path = inputData.getString(KEY_PATH)
        val clientId = inputData.getString(KEY_CLIENT_ID)
        if (installationId <= 0 || path == null || clientId == null) return Result.failure(failureData("Faltan datos del archivo."))

        val kind = MediaKind.fromApi(inputData.getString(KEY_KIND))
        val stage = MediaStage.fromApi(inputData.getString(KEY_STAGE))
        val file = File(path)

        val uploader = EntryPointAccessors.fromApplication(applicationContext, MediaUploadEntryPoint::class.java).uploader()

        return try {
            uploader.upload(installationId, file, kind, stage, clientId)
            file.delete()
            Result.success()
        } catch (error: MediaUploadException) {
            if (error.retryable && runAttemptCount < MAX_ATTEMPTS) Result.retry()
            else Result.failure(failureData(error.message))
        }
    }

    // Se devuelven los mismos datos de entrada más el motivo, para que la app pueda reintentar desde ahí.
    private fun failureData(message: String): Data = Data.Builder()
        .putAll(inputData)
        .putString(KEY_ERROR, message)
        .build()
}

/** Archivo que no se pudo subir tras varios intentos. */
data class FailedUpload(
    val installationId: Int,
    val path: String,
    val kind: MediaKind,
    val stage: MediaStage,
    val clientId: String,
    val message: String
)

/** Resumen para mostrar al usuario: cuántos archivos van subiendo y cuáles fallaron. */
data class MediaUploadStatus(
    val uploading: Int = 0,
    val waiting: Int = 0,
    val failed: List<FailedUpload> = emptyList()
) {
    val isIdle: Boolean get() = uploading == 0 && waiting == 0 && failed.isEmpty()
    val pending: Int get() = uploading + waiting
}

@Singleton
class MediaUploadScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Manda a subir en segundo plano los archivos de una instalación ya creada. */
    fun enqueue(installationId: Int, items: List<LocalMedia>) {
        items.forEach { enqueueOne(installationId, it.path, it.kind, it.stage, it.id, replace = false) }
    }

    fun retry(failed: List<FailedUpload>) {
        failed.forEach { enqueueOne(it.installationId, it.path, it.kind, it.stage, it.clientId, replace = true) }
    }

    /** Descarta los que fallaron: se borran del teléfono y desaparecen del aviso. */
    fun discard(failed: List<FailedUpload>) {
        failed.forEach { runCatching { File(it.path).delete() } }
        workManager.pruneWork()
    }

    val status: Flow<MediaUploadStatus>
        get() = workManager.getWorkInfosByTagFlow(TAG).map { infos ->
            MediaUploadStatus(
                uploading = infos.count { it.state == WorkInfo.State.RUNNING },
                waiting = infos.count { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED },
                failed = infos.filter { it.state == WorkInfo.State.FAILED }.mapNotNull { it.toFailedUpload() }
            )
        }

    private fun enqueueOne(installationId: Int, path: String, kind: MediaKind, stage: MediaStage, clientId: String, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<MediaUploadWorker>()
            .setInputData(
                workDataOf(
                    KEY_INSTALLATION_ID to installationId,
                    KEY_PATH to path,
                    KEY_KIND to kind.api,
                    KEY_STAGE to stage.api,
                    KEY_CLIENT_ID to clientId
                )
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()

        workManager.enqueueUniqueWork("media-$clientId", if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, request)
    }

    private fun WorkInfo.toFailedUpload(): FailedUpload? {
        val data = outputData
        val installationId = data.getInt(KEY_INSTALLATION_ID, -1)
        val path = data.getString(KEY_PATH) ?: return null
        val clientId = data.getString(KEY_CLIENT_ID) ?: return null
        if (installationId <= 0) return null
        return FailedUpload(
            installationId = installationId,
            path = path,
            kind = MediaKind.fromApi(data.getString(KEY_KIND)),
            stage = MediaStage.fromApi(data.getString(KEY_STAGE)),
            clientId = clientId,
            message = data.getString(KEY_ERROR) ?: "No se pudo subir."
        )
    }

    private companion object {
        const val TAG = "rodvar-media-upload"
    }
}
