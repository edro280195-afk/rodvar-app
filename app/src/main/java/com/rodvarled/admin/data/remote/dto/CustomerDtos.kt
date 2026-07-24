package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomerListItem(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String? = null,
    val pointsBalance: Int = 0,
    val appointmentsCount: Int = 0,
    val totalSpent: Double = 0.0,
    val lastActivityAt: String? = null
)

@Serializable
data class CustomerAppointmentSummary(
    val id: Int,
    val requestedDate: String,
    val requestedTime: String? = null,
    val status: String,
    val vehicleInfo: String = "No especificado",
    val quoteTotal: Double? = null
)

@Serializable
data class CustomerInstallationItem(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
)

@Serializable
data class CustomerInstallationSummary(
    val id: Int,
    val folio: String,
    val installedAt: String,
    val totalAmount: Double,
    val paymentMethod: String,
    val vehicleInfo: String = "No especificado",
    val items: List<CustomerInstallationItem> = emptyList()
)

@Serializable
data class CustomerWarrantySummary(
    val id: Int,
    val folio: String,
    val publicToken: String? = null,
    val warrantyStart: String,
    val warrantyEnd: String,
    val isActive: Boolean,
    val isSigned: Boolean
)

@Serializable
data class PointsTransaction(
    val id: Int,
    val delta: Int,
    val reason: String,
    val installationId: Int? = null,
    val createdAt: String? = null
)

@Serializable
data class CustomerDetail(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val referralCode: String? = null,
    val pointsBalance: Int = 0,
    val portalToken: String? = null,
    val createdAt: String? = null,
    val appointments: List<CustomerAppointmentSummary> = emptyList(),
    val installations: List<CustomerInstallationSummary> = emptyList(),
    val warranties: List<CustomerWarrantySummary> = emptyList(),
    val pointsHistory: List<PointsTransaction> = emptyList()
)

@Serializable
data class CreateCustomerRequest(
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null
)

@Serializable
data class UpdateCustomerRequest(
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null
)

@Serializable
data class AdjustPointsRequest(
    val delta: Int,
    val reason: String
)

@Serializable
data class PortalLink(
    val portalToken: String,
    val portalUrl: String
)
