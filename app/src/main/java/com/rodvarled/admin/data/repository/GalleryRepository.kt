package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.GalleryApi
import com.rodvarled.admin.data.remote.dto.CreateGalleryItemRequest
import com.rodvarled.admin.data.remote.dto.UpdateGalleryMetadataRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    private val api: GalleryApi
) {
    suspend fun getAll() = safeApiCall { api.getAll() }
    suspend fun create(request: CreateGalleryItemRequest) = safeApiCall { api.create(request) }
    suspend fun updateMetadata(id: Int, request: UpdateGalleryMetadataRequest) = safeApiCall { api.updateMetadata(id, request) }
    suspend fun delete(id: Int) = safeApiCall { api.delete(id) }
}
