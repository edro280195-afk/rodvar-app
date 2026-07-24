package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.InstallationsApi
import com.rodvarled.admin.data.remote.dto.CreateDirectInstallationRequest
import com.rodvarled.admin.data.remote.dto.CreateInstallationPhotoRequest
import com.rodvarled.admin.data.remote.dto.UpdateInstallationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallationsRepository @Inject constructor(
    private val api: InstallationsApi
) {
    suspend fun getInstallations() = safeApiCall { api.getInstallations() }
    suspend fun getInstallation(id: Int) = safeApiCall { api.getInstallation(id) }
    suspend fun createDirectInstallation(request: CreateDirectInstallationRequest) = safeApiCall { api.createDirectInstallation(request) }
    suspend fun updateInstallation(id: Int, request: UpdateInstallationRequest) = safeApiCall { api.updateInstallation(id, request) }
    suspend fun addPhoto(id: Int, title: String?, beforeBase64: String?, afterBase64: String) = safeApiCall {
        api.addPhoto(id, CreateInstallationPhotoRequest(title, beforeBase64, afterBase64))
    }
}
