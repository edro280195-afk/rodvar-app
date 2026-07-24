package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.QuoteItemDetail
import com.rodvarled.admin.data.remote.dto.QuoteSummary
import com.rodvarled.admin.data.remote.dto.UpdateQuoteRequest
import com.rodvarled.admin.data.remote.dto.UpdateQuoteStatusRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

interface QuotesApi {
    @GET("api/Quotes/admin/all")
    suspend fun getQuotes(): List<QuoteSummary>

    @GET("api/Quotes/{id}")
    suspend fun getQuote(@Path("id") id: Int): QuoteSummary

    @GET("api/Quotes/{id}/items")
    suspend fun getQuoteItems(@Path("id") id: Int): List<QuoteItemDetail>

    @PUT("api/Quotes/{id}")
    suspend fun updateQuote(@Path("id") id: Int, @Body request: UpdateQuoteRequest)

    @PATCH("api/Quotes/{id}/status")
    suspend fun updateStatus(@Path("id") id: Int, @Body request: UpdateQuoteStatusRequest)
}
