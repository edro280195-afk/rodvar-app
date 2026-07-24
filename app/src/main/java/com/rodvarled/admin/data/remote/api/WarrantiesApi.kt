package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.MessageResponse
import com.rodvarled.admin.data.remote.dto.SignWarrantyRequest
import com.rodvarled.admin.data.remote.dto.UpdateWarrantyRequest
import com.rodvarled.admin.data.remote.dto.WarrantyDetail
import com.rodvarled.admin.data.remote.dto.WarrantySummary
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

interface WarrantiesApi {
    @GET("api/Warranties")
    suspend fun getWarranties(): List<WarrantySummary>

    @GET("api/Warranties/{id}")
    suspend fun getWarranty(@Path("id") id: Int): WarrantyDetail

    // Firma capturada en el momento por el instalador dentro del asistente "Completar instalación".
    @PATCH("api/Warranties/{id}/sign")
    suspend fun signWarranty(@Path("id") id: Int, @Body request: SignWarrantyRequest): MessageResponse

    @PUT("api/Warranties/{id}")
    suspend fun updateWarranty(@Path("id") id: Int, @Body request: UpdateWarrantyRequest)

    @DELETE("api/Warranties/{id}")
    suspend fun deleteWarranty(@Path("id") id: Int): MessageResponse

    @PATCH("api/Warranties/{id}/revoke")
    suspend fun revokeWarranty(@Path("id") id: Int): MessageResponse

    @PATCH("api/Warranties/{id}/reset-signature")
    suspend fun resetSignature(@Path("id") id: Int): MessageResponse
}
