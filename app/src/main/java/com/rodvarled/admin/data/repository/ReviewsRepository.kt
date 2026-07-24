package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.ReviewsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewsRepository @Inject constructor(
    private val api: ReviewsApi
) {
    suspend fun getAll() = safeApiCall { api.getAll() }
    suspend fun approve(id: Int, approved: Boolean) = safeApiCall { api.approve(id, approved) }
    suspend fun delete(id: Int) = safeApiCall { api.delete(id) }
}
