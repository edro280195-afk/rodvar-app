package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VehicleMakeDto(
    val id: Int,
    val name: String
)

@Serializable
data class VehicleModelDto(
    val id: Int,
    val makeId: Int = 0,
    val name: String
)

@Serializable
data class VehicleYearDto(
    val id: Int,
    val year: Int
)

@Serializable
data class VehicleTrimDto(
    val id: Int,
    val name: String
)
