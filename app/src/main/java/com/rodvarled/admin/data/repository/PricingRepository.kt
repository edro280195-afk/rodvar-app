package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.PricingApi
import com.rodvarled.admin.data.remote.dto.SavePriceRuleRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricingRepository @Inject constructor(private val api: PricingApi) {
    suspend fun getRules(productId: Int) = safeApiCall { api.getRules(productId) }
    suspend fun createRule(request: SavePriceRuleRequest) = safeApiCall { api.createRule(request) }
    suspend fun updateRule(id: Int, request: SavePriceRuleRequest) = safeApiCall { api.updateRule(id, request) }
    suspend fun deleteRule(id: Int) = safeApiCall { api.deleteRule(id) }
}
