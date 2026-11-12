package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.VehicleMakeDto
import com.rodvarled.admin.data.remote.dto.VehicleSearchHit
import com.rodvarled.admin.data.remote.dto.VehicleModelDto
import com.rodvarled.admin.data.remote.dto.VehicleTrimDto
import com.rodvarled.admin.data.remote.dto.VehicleYearDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VehiclesApi {
    @GET("api/Vehicles/Search")
    suspend fun search(@Query("q") query: String): List<VehicleSearchHit>

    @GET("api/Vehicles/Makes")
    suspend fun getMakes(): List<VehicleMakeDto>

    @GET("api/Vehicles/Models/{makeId}")
    suspend fun getModels(@Path("makeId") makeId: Int): List<VehicleModelDto>

    @GET("api/Vehicles/Years/{modelId}")
    suspend fun getYears(@Path("modelId") modelId: Int): List<VehicleYearDto>

    @GET("api/Vehicles/Trims/{yearId}")
    suspend fun getTrims(@Path("yearId") yearId: Int): List<VehicleTrimDto>
}
