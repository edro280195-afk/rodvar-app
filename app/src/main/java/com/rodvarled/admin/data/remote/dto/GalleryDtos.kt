package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GalleryItemDto(
    val id: Int,
    val title: String? = null,
    val beforeImageUrl: String? = null,
    val afterImageUrl: String,
    val productId: Int? = null,
    val productName: String? = null,
    val isFeatured: Boolean = false,
    val isActive: Boolean = true
)

@Serializable
data class CreateGalleryItemRequest(
    val title: String? = null,
    val beforeImageBase64: String? = null,
    val afterImageBase64: String,
    val productId: Int? = null,
    val isFeatured: Boolean = false
)

@Serializable
data class UpdateGalleryMetadataRequest(
    val title: String? = null,
    val isFeatured: Boolean,
    val isActive: Boolean
)
