package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.AppointmentResponse
import com.rodvarled.admin.data.remote.dto.AppointmentSummary
import com.rodvarled.admin.data.remote.dto.CreateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.MessageResponse
import com.rodvarled.admin.data.remote.dto.SendItineraryResponse
import com.rodvarled.admin.data.remote.dto.UpdateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.UpdateAppointmentStatusRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AppointmentsApi {
    @GET("api/Appointments")
    suspend fun getAppointments(): List<AppointmentSummary>

    @GET("api/Appointments/{id}")
    suspend fun getAppointment(@Path("id") id: Int): AppointmentSummary

    @POST("api/Appointments")
    suspend fun createAppointment(@Body request: CreateAppointmentRequest): AppointmentResponse

    @PUT("api/Appointments/{id}")
    suspend fun updateAppointment(@Path("id") id: Int, @Body request: UpdateAppointmentRequest)

    @PATCH("api/Appointments/{id}/status")
    suspend fun updateStatus(@Path("id") id: Int, @Body request: UpdateAppointmentStatusRequest): AppointmentSummary

    @POST("api/Appointments/{id}/send-itinerary")
    suspend fun sendItinerary(@Path("id") id: Int): SendItineraryResponse

    @DELETE("api/Appointments/{id}")
    suspend fun deleteAppointment(@Path("id") id: Int): MessageResponse
}
