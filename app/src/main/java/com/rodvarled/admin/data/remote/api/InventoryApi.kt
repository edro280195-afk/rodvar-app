package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.CreateInventoryLabelsRequest
import com.rodvarled.admin.data.remote.dto.InventoryUnit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryApi {
    @POST("api/Inventory/labels")
    suspend fun createLabels(@Body request: CreateInventoryLabelsRequest): List<InventoryUnit>

    @GET("api/Inventory/units/{code}")
    suspend fun getUnit(@Path("code") code: String): InventoryUnit

    @GET("api/Inventory/units")
    suspend fun getUnits(@Query("productId") productId: Int? = null): List<InventoryUnit>
}
