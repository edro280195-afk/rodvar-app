package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WarrantySummary(
    val id: Int,
    val folio: String,
    val publicToken: String? = null,
    val customerName: String,
    val customerPhone: String,
    val warrantyStart: String,
    val warrantyEnd: String,
    val isActive: Boolean,
    val isSigned: Boolean,
    val signedAt: String? = null,
    val signatureImageUrl: String? = null
)

@Serializable
data class WarrantyItem(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val warrantyMonths: Int = 12,
    val manufacturerWarrantyMonths: Int = 24
)

@Serializable
data class WarrantyDetail(
    val id: Int,
    val folio: String,
    val publicToken: String? = null,
    val customerName: String,
    val customerPhone: String,
    val warrantyStart: String,
    val warrantyEnd: String,
    val isActive: Boolean,
    val isSigned: Boolean,
    val signedAt: String? = null,
    val signatureImageUrl: String? = null,
    val notes: String? = null,
    val installationId: Int,
    val vehicleInfo: String = "No especificado",
    val items: List<WarrantyItem> = emptyList()
)

@Serializable
data class SignWarrantyRequest(
    val signatureBase64: String,
    val acceptedTerms: Boolean
)

@Serializable
data class UpdateWarrantyRequest(
    val warrantyStart: String,
    val warrantyEnd: String,
    val isActive: Boolean,
    val notes: String? = null
)
