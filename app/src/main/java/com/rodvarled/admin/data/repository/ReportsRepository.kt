package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.ReportsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportsRepository @Inject constructor(
    private val api: ReportsApi
) {
    suspend fun getPricingReport(from: String? = null, to: String? = null) =
        safeApiCall { api.getPricingReport(from, to) }
}
