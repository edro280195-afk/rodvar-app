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
    val serviceType: String = SERVICE_TYPE_INSTALLATION,
    val visitFeeAmount: Double? = null,
    val notes: String? = null,
    val serviceAddress: String? = null,
    val serviceLatitude: Double? = null,
    val serviceLongitude: Double? = null,
    val googlePlaceId: String? = null,
    val vehicleInfo: String = "No especificado",
    val createdAt: String? = null,
    val durationMinutes: Int? = null,
    val cancellationReason: String? = null,
    val itinerarySentAt: String? = null,
    val quoteId: Int? = null,
    val quoteTotal: Double? = null,
    val installationId: Int? = null,
    val warrantyId: Int? = null,
    val warrantyPublicToken: String? = null,
    val vehicleTrimId: Int? = null
) {
    val isRevision: Boolean get() = serviceType == SERVICE_TYPE_REVISION
}

/** Valores de `serviceType` que entiende la API (se guardan sin acentos). */
const val SERVICE_TYPE_INSTALLATION = "Instalacion"
const val SERVICE_TYPE_REVISION = "Revision"

@Serializable
data class CreateQuoteItemRequest(
    val productId: Int,
    val quantity: Int,
    val productBulbTypeId: Int? = null,
    val inventoryUnitCodes: List<String> = emptyList()
)

@Serializable
data class CreateAppointmentRequest(
    val customerName: String,
    val customerPhone: String,
    val source: String,
    val requestedDate: String? = null,
    val requestedTime: String? = null,
    val notes: String? = null,
    val serviceAddress: String? = null,
    val serviceLatitude: Double? = null,
    val serviceLongitude: Double? = null,
    val googlePlaceId: String? = null,
    val vehicleTrimId: Int? = null,
    val serviceType: String? = null,
    val visitFeeAmount: Double? = null,
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
    val serviceAddress: String? = null,
    val serviceLatitude: Double? = null,
    val serviceLongitude: Double? = null,
    val googlePlaceId: String? = null,
    val vehicleTrimId: Int? = null,
    val serviceType: String? = null,
    val visitFeeAmount: Double? = null,
    val status: String
)

@Serializable
data class UpdateAppointmentStatusRequest(
    val status: String,
    val cancellationReason: String? = null,
    val items: List<InstallationItemSelectionRequest> = emptyList(),
    // true = `items` es la lista definitiva de lo instalado, aunque venga vacía.
    val itemsAreFinal: Boolean? = null,
    // Datos del cobro al cerrar. Todos opcionales: sin ellos la API usa lo cotizado.
    val totalAmount: Double? = null,
    val visitFeeCharged: Double? = null,
    val visitFeeCredit: Double? = null,
    val paymentMethod: String? = null,
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val technicianNotes: String? = null
)

@Serializable
data class InstallationItemSelectionRequest(
    val productId: Int,
    val productBulbTypeId: Int? = null,
    // 0 = solo informa códigos QR de un producto de la cotización.
    val quantity: Int = 0,
    // Precio unitario acordado; null = usa el de la cotización o el de catálogo.
    val unitPrice: Double? = null,
    val inventoryUnitCodes: List<String> = emptyList()
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
