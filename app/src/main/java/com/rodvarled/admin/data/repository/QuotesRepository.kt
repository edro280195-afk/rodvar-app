package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.QuotesApi
import com.rodvarled.admin.data.remote.dto.UpdateQuoteRequest
import com.rodvarled.admin.data.remote.dto.UpdateQuoteStatusRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotesRepository @Inject constructor(
    private val api: QuotesApi
) {
    suspend fun getQuotes() = safeApiCall { api.getQuotes() }
    suspend fun getQuote(id: Int) = safeApiCall { api.getQuote(id) }
    suspend fun getItems(id: Int) = safeApiCall { api.getQuoteItems(id) }
    suspend fun update(id: Int, request: UpdateQuoteRequest) = safeApiCall { api.updateQuote(id, request) }
    suspend fun updateStatus(id: Int, status: String) = safeApiCall { api.updateStatus(id, UpdateQuoteStatusRequest(status)) }
}
