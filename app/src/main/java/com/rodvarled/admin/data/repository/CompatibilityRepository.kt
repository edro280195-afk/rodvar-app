package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.CompatibilityApi
import com.rodvarled.admin.data.remote.dto.SaveMappingRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatibilityRepository @Inject constructor(
    private val api: CompatibilityApi
) {
    suspend fun getByTrim(trimId: Int) = safeApiCall { api.getByTrim(trimId) }
    suspend fun saveMapping(trimId: Int, positionName: String, bulbTypeId: Int) = safeApiCall {
        api.saveMapping(SaveMappingRequest(trimId, positionName, bulbTypeId))
    }
}
