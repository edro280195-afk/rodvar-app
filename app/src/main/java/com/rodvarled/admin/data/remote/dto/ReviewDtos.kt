package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: Int,
    val customerName: String,
    val vehicleName: String? = null,
    val rating: Int,
    val text: String,
    val imageUrl: String? = null,
    val isApproved: Boolean,
    val createdAt: String? = null
)
