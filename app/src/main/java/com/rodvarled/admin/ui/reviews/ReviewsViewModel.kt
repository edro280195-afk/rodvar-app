package com.rodvarled.admin.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.ReviewDto
import com.rodvarled.admin.data.repository.ReviewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewsUiState(
    val isLoading: Boolean = true,
    val reviews: List<ReviewDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val repository: ReviewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAll()
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, reviews = it.sortedBy { r -> r.isApproved }) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun approve(id: Int, approved: Boolean) {
        viewModelScope.launch {
            repository.approve(id, approved).onSuccess { refresh() }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            repository.delete(id).onSuccess { refresh() }
        }
    }
}
