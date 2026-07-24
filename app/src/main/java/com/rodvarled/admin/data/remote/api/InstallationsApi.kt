package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.CreateDirectInstallationRequest
import com.rodvarled.admin.data.remote.dto.CreateInstallationPhotoRequest
import com.rodvarled.admin.data.remote.dto.DirectInstallationResponse
import com.rodvarled.admin.data.remote.dto.InstallationDetail
import com.rodvarled.admin.data.remote.dto.InstallationPhoto
import com.rodvarled.admin.data.remote.dto.InstallationSummary
import com.rodvarled.admin.data.remote.dto.UpdateInstallationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface InstallationsApi {
    @GET("api/Installations")
    suspend fun getInstallations(): List<InstallationSummary>

    @GET("api/Installations/{id}")
    suspend fun getInstallation(@Path("id") id: Int): InstallationDetail

    @POST("api/Installations/direct")
    suspend fun createDirectInstallation(@Body request: CreateDirectInstallationRequest): DirectInstallationResponse

    @PATCH("api/Installations/{id}")
    suspend fun updateInstallation(@Path("id") id: Int, @Body request: UpdateInstallationRequest)

    @GET("api/Installations/{id}/photos")
    suspend fun getPhotos(@Path("id") id: Int): List<InstallationPhoto>

    @POST("api/Installations/{id}/photos")
    suspend fun addPhoto(@Path("id") id: Int, @Body request: CreateInstallationPhotoRequest): InstallationPhoto
}
