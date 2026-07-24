package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.CompatibilityResult
import com.rodvarled.admin.data.remote.dto.MessageResponse
import com.rodvarled.admin.data.remote.dto.SaveMappingRequest
import com.rodvarled.admin.data.remote.dto.VehicleSearchResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CompatibilityApi {
    @GET("api/Compatibility/trim/{trimId}")
    suspend fun getByTrim(@Path("trimId") trimId: Int): List<CompatibilityResult>

    @GET("api/Compatibility/search")
    suspend fun search(
        @Query("makeId") makeId: Int,
        @Query("modelId") modelId: Int,
        @Query("year") year: Int
    ): VehicleSearchResult

    @POST("api/Compatibility/mapping")
    suspend fun saveMapping(@Body request: SaveMappingRequest): MessageResponse
}
