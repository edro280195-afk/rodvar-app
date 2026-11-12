package com.rodvarled.admin.ui.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.media.LocalMedia
import com.rodvarled.admin.core.media.MediaStage
import com.rodvarled.admin.core.media.MediaUploadScheduler
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.InstallationMedia
import com.rodvarled.admin.data.repository.InstallationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstallationMediaUiState(
    val isLoading: Boolean = true,
    val folio: String? = null,
    val customerName: String? = null,
    val media: List<InstallationMedia> = emptyList(),
    val error: String? = null,
    val message: String? = null
) {
    val knownStages: List<MediaStage> get() = media.map { MediaStage.fromApi(it.stage) }
}

@HiltViewModel
class InstallationMediaViewModel @Inject constructor(
    private val installationsRepository: InstallationsRepository,
    private val scheduler: MediaUploadScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val installationId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(InstallationMediaUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
        // Cuando termina una tanda de subidas, se recarga la lista para que aparezcan los archivos nuevos.
        viewModelScope.launch {
            var hadPending = false
            scheduler.status.collect { status ->
                if (hadPending && status.pending == 0) load(showLoading = false)
                hadPending = status.pending > 0
            }
        }
    }

    fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            installationsRepository.getInstallation(installationId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        folio = it.folio,
                        customerName = it.customerName,
                        media = it.media,
                        error = null
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    /** Manda a subir de inmediato lo que se acaba de tomar o elegir. */
    fun addAndUpload(items: List<LocalMedia>) {
        if (items.isEmpty()) return
        scheduler.enqueue(installationId, items)
        _uiState.value = _uiState.value.copy(message = "Subiendo ${items.size} archivo(s) en segundo plano…")
    }

    fun delete(media: InstallationMedia) {
        viewModelScope.launch {
            installationsRepository.deleteMedia(installationId, media.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        media = _uiState.value.media.filterNot { it.id == media.id },
                        message = "Archivo eliminado."
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.toUserMessage()) }
        }
    }

    fun consumeMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
