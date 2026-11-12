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
    val items: List<WarrantyItem> = emptyList(),
    val claims: List<WarrantyClaim> = emptyList()
)

@Serializable
data class WarrantyClaim(
    val id: Int,
    val folio: String,
    val reason: String,
    val status: String,
    val resolution: String? = null,
    val productName: String? = null,
    val quantity: Int = 1,
    val productCost: Double? = null,
    val laborCost: Double? = null,
    val totalCost: Double? = null,
    val createdAt: String? = null,
    val resolvedAt: String? = null
)

@Serializable
data class CreateWarrantyClaimRequest(
    val reason: String,
    val resolution: String? = null,
    val status: String = "Aplicada",
    val productId: Int? = null,
    val productBulbTypeId: Int? = null,
    val quantity: Int = 1,
    val laborCost: Double = 0.0
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
