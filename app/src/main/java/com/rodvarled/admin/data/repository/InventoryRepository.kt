package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.InventoryApi
import com.rodvarled.admin.data.remote.dto.CreateInventoryLabelsRequest
import com.rodvarled.admin.data.remote.dto.ReceiveInventoryRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(private val api: InventoryApi) {
    suspend fun createLabels(productId: Int, productBulbTypeId: Int?, quantity: Int) = safeApiCall {
        api.createLabels(CreateInventoryLabelsRequest(productId, productBulbTypeId, quantity))
    }

    suspend fun receive(request: ReceiveInventoryRequest) = safeApiCall { api.receive(request) }

    suspend fun getUnit(code: String) = safeApiCall { api.getUnit(code) }
    suspend fun getUnits(productId: Int? = null) = safeApiCall { api.getUnits(productId) }
}
