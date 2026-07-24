package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.local.CachedCustomerEntity
import com.rodvarled.admin.data.local.CustomerCacheDao
import com.rodvarled.admin.data.remote.api.CustomersApi
import com.rodvarled.admin.data.remote.dto.AdjustPointsRequest
import com.rodvarled.admin.data.remote.dto.CreateCustomerRequest
import com.rodvarled.admin.data.remote.dto.CustomerListItem
import com.rodvarled.admin.data.remote.dto.UpdateCustomerRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomersRepository @Inject constructor(
    private val api: CustomersApi,
    private val cacheDao: CustomerCacheDao,
    private val json: Json
) {
    fun observeCustomers(): Flow<List<CustomerListItem>> =
        cacheDao.observeAll().map { rows -> rows.map { json.decodeFromString(CustomerListItem.serializer(), it.json) } }

    suspend fun refresh(search: String? = null): Result<Unit> = safeApiCall {
        val customers = api.getCustomers(search)
        if (search.isNullOrBlank()) {
            cacheDao.replaceAll(customers.map { it.toCacheEntity() })
        }
    }

    suspend fun search(term: String) = safeApiCall { api.getCustomers(term) }

    suspend fun getCustomer(id: Int) = safeApiCall { api.getCustomer(id) }

    suspend fun createCustomer(request: CreateCustomerRequest) = safeApiCall {
        val result = api.createCustomer(request)
        refresh()
        result
    }

    suspend fun updateCustomer(id: Int, request: UpdateCustomerRequest) = safeApiCall {
        api.updateCustomer(id, request)
        refresh()
        Unit
    }

    suspend fun adjustPoints(id: Int, delta: Int, reason: String) = safeApiCall {
        api.adjustPoints(id, AdjustPointsRequest(delta, reason))
    }

    suspend fun getPortalLink(id: Int) = safeApiCall { api.getPortalLink(id) }

    private fun CustomerListItem.toCacheEntity() = CachedCustomerEntity(
        id = id,
        name = name,
        phone = phone,
        json = json.encodeToString(CustomerListItem.serializer(), this)
    )
}
