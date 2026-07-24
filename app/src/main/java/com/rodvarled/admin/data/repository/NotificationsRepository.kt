package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.NotificationsApi
import com.rodvarled.admin.data.remote.dto.RegisterDeviceRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val api: NotificationsApi
) {
    suspend fun registerDevice(token: String) = safeApiCall { api.registerDevice(RegisterDeviceRequest(token)) }
    suspend fun unregisterDevice(token: String) = safeApiCall { api.unregisterDevice(RegisterDeviceRequest(token)) }
}
