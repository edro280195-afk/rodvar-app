package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.SettingsApi
import com.rodvarled.admin.data.remote.dto.DEFAULT_HOME_VISIT_FEE
import com.rodvarled.admin.data.remote.dto.ServiceFees
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val api: SettingsApi
) {
    suspend fun getServiceFees() = safeApiCall { api.getServiceFees() }

    /** Tarifa de revisión a domicilio vigente; si no hay conexión, la tarifa por omisión. */
    suspend fun getHomeVisitFee(): Double =
        getServiceFees().getOrNull()?.homeVisitFee ?: DEFAULT_HOME_VISIT_FEE

    suspend fun updateHomeVisitFee(fee: Double) = safeApiCall { api.updateServiceFees(ServiceFees(fee)) }
}
