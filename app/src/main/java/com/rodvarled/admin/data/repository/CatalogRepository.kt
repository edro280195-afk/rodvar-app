package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.local.CachedProductEntity
import com.rodvarled.admin.data.local.ProductCacheDao
import com.rodvarled.admin.data.remote.api.CatalogApi
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.ProductSaveRequest
import com.rodvarled.admin.data.remote.dto.StockAdjustRequest
import com.rodvarled.admin.data.remote.dto.UploadProductImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val api: CatalogApi,
    private val cacheDao: ProductCacheDao,
    private val json: Json
) {
    fun observeProducts(): Flow<List<ProductDetail>> =
        cacheDao.observeAll().map { rows -> rows.map { json.decodeFromString(ProductDetail.serializer(), it.json) } }

    fun observeLowStock(): Flow<List<ProductDetail>> =
        cacheDao.observeLowStock().map { rows -> rows.map { json.decodeFromString(ProductDetail.serializer(), it.json) } }

    suspend fun refresh(): Result<Unit> = safeApiCall {
        val products = api.getProducts()
        cacheDao.replaceAll(products.map { it.toCacheEntity() })
    }

    suspend fun getCategories() = safeApiCall { api.getCategories() }

    suspend fun getBulbTypes() = safeApiCall { api.getBulbTypes() }

    suspend fun getProduct(id: Int) = safeApiCall { api.getProduct(id) }

    suspend fun createProduct(request: ProductSaveRequest) = safeApiCall {
        val result = api.createProduct(request)
        refresh()
        result
    }

    suspend fun updateProduct(id: Int, request: ProductSaveRequest) = safeApiCall {
        api.updateProduct(id, request)
        refresh()
        Unit
    }

    suspend fun deactivateProduct(id: Int) = safeApiCall {
        api.deactivateProduct(id)
        refresh()
        Unit
    }

    suspend fun getLowStockAlerts() = safeApiCall { api.getLowStock() }

    suspend fun adjustStock(productId: Int, bulbTypeId: Int?, delta: Int, reason: String, note: String? = null) = safeApiCall {
        val result = api.adjustStock(productId, StockAdjustRequest(bulbTypeId, delta, reason, note))
        refresh()
        result
    }

    suspend fun getStockMovements(productId: Int) = safeApiCall { api.getStockMovements(productId) }

    suspend fun uploadProductImage(productId: Int, imageBase64: String) = safeApiCall {
        api.uploadProductImage(productId, UploadProductImageRequest(imageBase64))
    }

    private fun ProductDetail.toCacheEntity(): CachedProductEntity {
        val effectiveStock = if (variants.isNotEmpty()) variants.sumOf { it.stock } else stock
        val isLow = if (variants.isNotEmpty()) {
            variants.any { it.lowStockThreshold != null && it.stock <= it.lowStockThreshold }
        } else {
            lowStockThreshold != null && stock <= lowStockThreshold
        }
        return CachedProductEntity(
            id = id,
            name = name,
            categoryId = categoryId,
            stock = effectiveStock,
            isLowStock = isLow,
            isActive = true,
            json = json.encodeToString(ProductDetail.serializer(), this)
        )
    }
}
