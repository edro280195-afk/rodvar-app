package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppointmentSummary(
    val id: Int,
    val customerName: String,
    val customerPhone: String,
    val requestedDate: String? = null,
    val requestedTime: String? = null,
    val status: String,
    val source: String,
    val notes: String? = null,
    val vehicleInfo: String = "No especificado",
    val createdAt: String? = null,
    val durationMinutes: Int? = null,
    val cancellationReason: String? = null,
    val itinerarySentAt: String? = null,
    val quoteId: Int? = null,
    val quoteTotal: Double? = null,
    val warrantyId: Int? = null,
    val warrantyPublicToken: String? = null,
    val vehicleTrimId: Int? = null
)

@Serializable
data class CreateQuoteItemRequest(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class CreateAppointmentRequest(
    val customerName: String,
    val customerPhone: String,
    val source: String,
    val requestedDate: String? = null,
    val requestedTime: String? = null,
    val notes: String? = null,
    val vehicleTrimId: Int? = null,
    val items: List<CreateQuoteItemRequest>? = null
)

@Serializable
data class UpdateAppointmentRequest(
    val customerName: String,
    val customerPhone: String,
    val source: String,
    val requestedDate: String? = null,
    val requestedTime: String? = null,
    val notes: String? = null,
    val vehicleTrimId: Int? = null,
    val status: String
)

@Serializable
data class UpdateAppointmentStatusRequest(
    val status: String,
    val cancellationReason: String? = null
)

@Serializable
data class AppointmentResponse(
    val id: Int,
    val customerName: String,
    val requestedDate: String? = null,
    val requestedTime: String? = null,
    val status: String,
    val message: String,
    val quoteId: Int? = null
)

@Serializable
data class SendItineraryResponse(
    val message: String,
    val whatsAppUrl: String,
    val portalUrl: String
)
