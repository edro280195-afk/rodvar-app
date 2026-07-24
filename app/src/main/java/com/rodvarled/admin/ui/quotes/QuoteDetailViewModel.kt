package com.rodvarled.admin.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.CreateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.CreateQuoteItemRequest
import com.rodvarled.admin.data.remote.dto.QuoteItemDetail
import com.rodvarled.admin.data.remote.dto.QuoteSummary
import com.rodvarled.admin.data.repository.AppointmentsRepository
import com.rodvarled.admin.data.repository.QuotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class QuoteDetailUiState(
    val isLoading: Boolean = true,
    val quote: QuoteSummary? = null,
    val items: List<QuoteItemDetail> = emptyList(),
    val error: String? = null,
    val actionInProgress: Boolean = false,
    val snackbarMessage: String? = null,
    val convertedAppointmentId: Int? = null
)

@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    private val repository: QuotesRepository,
    private val appointmentsRepository: AppointmentsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quoteId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(QuoteDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getQuote(quoteId)
                .onSuccess { quote -> _uiState.value = _uiState.value.copy(isLoading = false, quote = quote) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
            repository.getItems(quoteId).onSuccess { _uiState.value = _uiState.value.copy(items = it) }
        }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.updateStatus(quoteId, status)
                .onSuccess { load(); _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = "Cotización actualizada.") }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun convertToAppointment() {
        val quote = _uiState.value.quote ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            appointmentsRepository.createAppointment(
                CreateAppointmentRequest(
                    customerName = quote.customerName,
                    customerPhone = quote.customerPhone,
                    source = "Local",
                    requestedDate = LocalDate.now().toString(),
                    requestedTime = null,
                    notes = "Generada desde cotización ${quote.folio}",
                    vehicleTrimId = quote.vehicleTrimId,
                    items = _uiState.value.items.map { CreateQuoteItemRequest(it.productId, it.quantity) }
                )
            )
                .onSuccess { _uiState.value = _uiState.value.copy(actionInProgress = false, convertedAppointmentId = it.id) }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun consumeSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
