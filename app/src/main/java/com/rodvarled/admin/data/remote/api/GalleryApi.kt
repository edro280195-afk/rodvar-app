package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.CreateGalleryItemRequest
import com.rodvarled.admin.data.remote.dto.GalleryItemDto
import com.rodvarled.admin.data.remote.dto.MessageResponse
import com.rodvarled.admin.data.remote.dto.UpdateGalleryMetadataRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface GalleryApi {
    @GET("api/Gallery")
    suspend fun getAll(): List<GalleryItemDto>

    @POST("api/Gallery")
    suspend fun create(@Body request: CreateGalleryItemRequest): GalleryItemDto

    @PATCH("api/Gallery/{id}")
    suspend fun updateMetadata(@Path("id") id: Int, @Body request: UpdateGalleryMetadataRequest)

    @DELETE("api/Gallery/{id}")
    suspend fun delete(@Path("id") id: Int): MessageResponse
}
