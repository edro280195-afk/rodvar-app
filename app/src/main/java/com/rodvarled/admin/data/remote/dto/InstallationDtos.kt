package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InstallationItemDto(
    val productId: Int = 0,
    val productBulbTypeId: Int? = null,
    val productName: String,
    val quantity: Int,
    val listUnitPrice: Double = 0.0,
    val unitPrice: Double,
    val discountAmount: Double = 0.0,
    val priceRuleName: String? = null
)

@Serializable
data class InstallationPhoto(
    val id: Int,
    val title: String? = null,
    val beforeImageUrl: String? = null,
    val afterImageUrl: String
)

/** Foto o video de evidencia de una instalación (cualquier cantidad por trabajo). */
@Serializable
data class InstallationMedia(
    val id: Int,
    /** "Foto" o "Video". */
    val kind: String = "Foto",
    /** "Antes", "Durante", "Despues" u "Otra". */
    val stage: String = "Otra",
    val url: String,
    val thumbnailUrl: String? = null,
    val title: String? = null,
    val durationSeconds: Int? = null,
    val sizeBytes: Long? = null,
    val createdAt: String? = null
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
    val serviceType: String = SERVICE_TYPE_INSTALLATION,
    val visitFeeCharged: Double = 0.0,
    val visitFeeCredit: Double = 0.0,
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
    val serviceType: String = SERVICE_TYPE_INSTALLATION,
    val visitFeeCharged: Double = 0.0,
    val visitFeeCredit: Double = 0.0,
    val vehicleInfo: String = "No especificado",
    val photosCount: Int = 0,
    val warrantyId: Int? = null,
    val warrantyPublicToken: String? = null,
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val technicianNotes: String? = null,
    val items: List<InstallationItemDto> = emptyList(),
    val photos: List<InstallationPhoto> = emptyList(),
    val media: List<InstallationMedia> = emptyList()
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
    val serviceType: String? = null,
    val visitFeeCharged: Double? = null,
    val visitFeeCredit: Double? = null,
    val items: List<CreateDirectInstallationItem> = emptyList()
)

/** Renglón de una instalación directa; `unitPrice` solo viaja si el precio se cambió a mano. */
@Serializable
data class CreateDirectInstallationItem(
    val productId: Int,
    val quantity: Int,
    val productBulbTypeId: Int? = null,
    val unitPrice: Double? = null,
    val inventoryUnitCodes: List<String> = emptyList()
)

@Serializable
data class DirectInstallationResponse(
    val installationId: Int,
    val folio: String,
    val customerId: Int,
    val portalUrl: String,
    // Nulos cuando solo hubo revisión a domicilio: no hay producto que garantizar.
    val warrantyId: Int? = null,
    val warrantyFolio: String? = null,
    val warrantyPublicToken: String? = null,
    val pointsEarned: Int,
    val totalAmount: Double
)
