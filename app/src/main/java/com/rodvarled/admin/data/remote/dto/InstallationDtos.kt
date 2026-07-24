package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InstallationItemDto(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
)

@Serializable
data class InstallationPhoto(
    val id: Int,
    val title: String? = null,
    val beforeImageUrl: String? = null,
    val afterImageUrl: String
)

@Serializable
data class InstallationSummary(
    val id: Int,
    val folio: String,
    val customerName: String,
    val customerPhone: String,
    val installedAt: String,
    val totalAmount: Double,
    val paymentMethod: String,
    val vehicleInfo: String = "No especificado",
    val photosCount: Int = 0,
    val warrantyId: Int? = null,
    val warrantyPublicToken: String? = null
)

@Serializable
data class InstallationDetail(
    val id: Int,
    val folio: String,
    val customerName: String,
    val customerPhone: String,
    val installedAt: String,
    val totalAmount: Double,
    val paymentMethod: String,
    val vehicleInfo: String = "No especificado",
    val photosCount: Int = 0,
    val warrantyId: Int? = null,
    val warrantyPublicToken: String? = null,
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val technicianNotes: String? = null,
    val items: List<InstallationItemDto> = emptyList(),
    val photos: List<InstallationPhoto> = emptyList()
)

@Serializable
data class UpdateInstallationRequest(
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val technicianNotes: String? = null,
    val paymentMethod: String
)

@Serializable
data class CreateInstallationPhotoRequest(
    val title: String? = null,
    val beforeImageBase64: String? = null,
    val afterImageBase64: String
)

// ===== Instalación directa (walk-in, sin cita previa) =====

@Serializable
data class CreateDirectInstallationRequest(
    val customerName: String,
    val customerPhone: String,
    val vehicleTrimId: Int? = null,
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val technicianNotes: String? = null,
    val paymentMethod: String,
    val totalAmount: Double,
    val items: List<CreateQuoteItemRequest> = emptyList()
)

@Serializable
data class DirectInstallationResponse(
    val installationId: Int,
    val folio: String,
    val customerId: Int,
    val portalUrl: String,
    val warrantyId: Int,
    val warrantyFolio: String,
    val warrantyPublicToken: String,
    val pointsEarned: Int,
    val totalAmount: Double
)
