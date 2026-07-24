package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.RegisterDeviceRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface NotificationsApi {
    @POST("api/Notifications/register-device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest)

    @DELETE("api/Notifications/unregister-device")
    suspend fun unregisterDevice(@Body request: RegisterDeviceRequest)
}
