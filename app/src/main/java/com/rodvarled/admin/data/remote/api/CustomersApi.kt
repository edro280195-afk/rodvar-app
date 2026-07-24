package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.AdjustPointsRequest
import com.rodvarled.admin.data.remote.dto.CreateCustomerRequest
import com.rodvarled.admin.data.remote.dto.CustomerDetail
import com.rodvarled.admin.data.remote.dto.CustomerListItem
import com.rodvarled.admin.data.remote.dto.MessageResponse
import com.rodvarled.admin.data.remote.dto.PortalLink
import com.rodvarled.admin.data.remote.dto.UpdateCustomerRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CustomersApi {
    @GET("api/Customers")
    suspend fun getCustomers(@Query("search") search: String? = null): List<CustomerListItem>

    @GET("api/Customers/{id}")
    suspend fun getCustomer(@Path("id") id: Int): CustomerDetail

    @POST("api/Customers")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): CustomerDetail

    @PUT("api/Customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Int, @Body request: UpdateCustomerRequest)

    @PATCH("api/Customers/{id}/points")
    suspend fun adjustPoints(@Path("id") id: Int, @Body request: AdjustPointsRequest): MessageResponse

    @GET("api/Customers/{id}/portal-link")
    suspend fun getPortalLink(@Path("id") id: Int): PortalLink
}
