package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.ServiceFees
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface SettingsApi {
    @GET("api/Settings/service-fees")
    suspend fun getServiceFees(): ServiceFees

    @PUT("api/Settings/service-fees")
    suspend fun updateServiceFees(@Body request: ServiceFees): ServiceFees
}
