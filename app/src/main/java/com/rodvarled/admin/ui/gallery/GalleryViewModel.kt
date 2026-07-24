package com.rodvarled.admin.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.CreateGalleryItemRequest
import com.rodvarled.admin.data.remote.dto.GalleryItemDto
import com.rodvarled.admin.data.remote.dto.UpdateGalleryMetadataRequest
import com.rodvarled.admin.data.repository.GalleryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val isLoading: Boolean = true,
    val items: List<GalleryItemDto> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAll()
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, items = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun toggleFeatured(item: GalleryItemDto) {
        viewModelScope.launch {
            repository.updateMetadata(item.id, UpdateGalleryMetadataRequest(item.title, !item.isFeatured, item.isActive)).onSuccess { refresh() }
        }
    }

    fun toggleActive(item: GalleryItemDto) {
        viewModelScope.launch {
            repository.updateMetadata(item.id, UpdateGalleryMetadataRequest(item.title, item.isFeatured, !item.isActive)).onSuccess { refresh() }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repository.delete(id).onSuccess { refresh() } }
    }

    fun create(title: String?, beforeBase64: String?, afterBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.create(CreateGalleryItemRequest(title, beforeBase64, afterBase64))
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false); refresh() }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage()) }
        }
    }
}
