package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuoteSummary(
    val id: Int,
    val folio: String,
    val customerName: String,
    val customerPhone: String,
    val vehicleInfo: String? = null,
    val vehicleTrimId: Int? = null,
    val total: Double,
    val status: String,
    val createdAt: String? = null
)

@Serializable
data class QuoteItemDetail(
    val productId: Int,
    val productBulbTypeId: Int? = null,
    val productName: String,
    val quantity: Int,
    val listUnitPrice: Double = 0.0,
    val unitPrice: Double,
    val discountAmount: Double = 0.0,
    val priceRuleName: String? = null,
    val subtotal: Double
)

@Serializable
data class CreateQuoteRequest(
    val customerName: String,
    val customerPhone: String,
    val vehicleTrimId: Int? = null,
    val notes: String? = null,
    val items: List<CreateQuoteItemRequest>
)

@Serializable
data class UpdateQuoteRequest(
    val customerName: String,
    val customerPhone: String,
    val vehicleTrimId: Int? = null,
    val notes: String? = null,
    val status: String,
    val items: List<CreateQuoteItemRequest>
)

@Serializable
data class UpdateQuoteStatusRequest(
    val status: String
)

@Serializable
data class QuoteResponse(
    val id: Int,
    val folio: String,
    val total: Double,
    val status: String,
    val message: String
)
