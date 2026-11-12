package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.PriceRule
import com.rodvarled.admin.data.remote.dto.SavePriceRuleRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PricingApi {
    @GET("api/Pricing/rules")
    suspend fun getRules(@Query("productId") productId: Int? = null): List<PriceRule>

    @POST("api/Pricing/rules")
    suspend fun createRule(@Body request: SavePriceRuleRequest): PriceRule

    @PUT("api/Pricing/rules/{id}")
    suspend fun updateRule(@Path("id") id: Int, @Body request: SavePriceRuleRequest)

    @DELETE("api/Pricing/rules/{id}")
    suspend fun deleteRule(@Path("id") id: Int)
}
