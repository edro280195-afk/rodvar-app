package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductLight(
    val id: Int,
    val name: String,
    val slug: String,
    val price: Double,
    val lumens: Int? = null,
    val colorTemperature: Int? = null,
    val wattage: Int? = null,
    val warrantyMonths: Int = 12,
    val stock: Int = 0,
    val mainImageUrl: String? = null
)

@Serializable
data class CompatibilityResult(
    val positionName: String? = null,
    val bulbTypeId: Int = 0,
    val bulbTypeName: String? = null,
    val notes: String? = null,
    val importantNotes: String? = null,
    val vehicleDisplayName: String? = null,
    val compatibleProducts: List<ProductLight> = emptyList()
)

@Serializable
data class SaveMappingRequest(
    val trimId: Int,
    val positionName: String,
    val bulbTypeId: Int
)

@Serializable
data class TrimCompatibility(
    val trimId: Int,
    val trimName: String,
    val bulbs: List<CompatibilityResult> = emptyList()
)

@Serializable
data class VehicleSearchResult(
    val vehicleInfo: String? = null,
    val yearId: Int = 0,
    val trims: List<TrimCompatibility> = emptyList()
)
