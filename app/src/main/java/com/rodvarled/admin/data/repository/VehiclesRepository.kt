package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.VehiclesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehiclesRepository @Inject constructor(
    private val api: VehiclesApi
) {
    suspend fun getMakes() = safeApiCall { api.getMakes() }
    suspend fun getModels(makeId: Int) = safeApiCall { api.getModels(makeId) }
    suspend fun getYears(modelId: Int) = safeApiCall { api.getYears(modelId) }
    suspend fun getTrims(yearId: Int) = safeApiCall { api.getTrims(yearId) }
}
