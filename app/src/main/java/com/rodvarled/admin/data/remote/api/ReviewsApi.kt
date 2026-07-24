package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.ReviewDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewsApi {
    @GET("api/Reviews/all")
    suspend fun getAll(): List<ReviewDto>

    @PATCH("api/Reviews/{id}/approve")
    suspend fun approve(@Path("id") id: Int, @Query("approved") approved: Boolean)

    @DELETE("api/Reviews/{id}")
    suspend fun delete(@Path("id") id: Int)
}
