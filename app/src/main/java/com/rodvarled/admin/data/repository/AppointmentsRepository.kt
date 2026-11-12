package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.local.AppointmentCacheDao
import com.rodvarled.admin.data.local.CachedAppointmentEntity
import com.rodvarled.admin.data.remote.api.AppointmentsApi
import com.rodvarled.admin.data.remote.dto.AppointmentSummary
import com.rodvarled.admin.data.remote.dto.CreateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.UpdateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.InstallationItemSelectionRequest
import com.rodvarled.admin.data.remote.dto.UpdateAppointmentStatusRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentsRepository @Inject constructor(
    private val api: AppointmentsApi,
    private val cacheDao: AppointmentCacheDao,
    private val json: Json
) {
    fun observeAppointments(): Flow<List<AppointmentSummary>> =
        cacheDao.observeAll().map { rows -> rows.map { json.decodeFromString(AppointmentSummary.serializer(), it.json) } }

    suspend fun refresh(): Result<Unit> = safeApiCall {
        val appointments = api.getAppointments()
        cacheDao.replaceAll(appointments.map { it.toCacheEntity() })
    }

    suspend fun getAppointment(id: Int) = safeApiCall { api.getAppointment(id) }

    suspend fun createAppointment(request: CreateAppointmentRequest) = safeApiCall {
        val result = api.createAppointment(request)
        refresh()
        result
    }

    suspend fun updateAppointment(id: Int, request: UpdateAppointmentRequest) = safeApiCall {
        api.updateAppointment(id, request)
        refresh()
        Unit
    }

    suspend fun updateStatus(
        id: Int,
        status: String,
        cancellationReason: String? = null,
        items: List<InstallationItemSelectionRequest> = emptyList(),
        itemsAreFinal: Boolean? = null,
        totalAmount: Double? = null,
        visitFeeCharged: Double? = null,
        visitFeeCredit: Double? = null,
        paymentMethod: String? = null,
        vehiclePlate: String? = null,
        vehicleColor: String? = null,
        technicianNotes: String? = null
    ): Result<AppointmentSummary> {
        val result = safeApiCall {
            api.updateStatus(
                id,
                UpdateAppointmentStatusRequest(
                    status = status,
                    cancellationReason = cancellationReason,
                    items = items,
                    itemsAreFinal = itemsAreFinal,
                    totalAmount = totalAmount,
                    visitFeeCharged = visitFeeCharged,
                    visitFeeCredit = visitFeeCredit,
                    paymentMethod = paymentMethod,
                    vehiclePlate = vehiclePlate,
                    vehicleColor = vehicleColor,
                    technicianNotes = technicianNotes
                )
            )
        }

        if (result.isSuccess) {
            refresh()
            return result
        }

        // Si la respuesta se perdió después del commit, confirmar el estado real evita
        // mostrar "error al actualizar" cuando la cita sí cambió en el servidor.
        val current = getAppointment(id).getOrNull()
        return if (current?.status == status) {
            refresh()
            Result.success(current)
        } else {
            result
        }
    }

    suspend fun sendItinerary(id: Int) = safeApiCall { api.sendItinerary(id) }

    suspend fun deleteAppointment(id: Int) = safeApiCall {
        api.deleteAppointment(id)
        refresh()
        Unit
    }

    private fun AppointmentSummary.toCacheEntity() = CachedAppointmentEntity(
        id = id,
        status = status,
        requestedDate = requestedDate ?: "",
        customerName = customerName,
        customerPhone = customerPhone,
        createdAt = createdAt,
        json = json.encodeToString(AppointmentSummary.serializer(), this)
    )
}
