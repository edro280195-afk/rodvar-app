package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.PricingReport
import retrofit2.http.GET
import retrofit2.http.Query

interface ReportsApi {
    @GET("api/Reports/pricing")
    suspend fun getPricingReport(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): PricingReport
}
