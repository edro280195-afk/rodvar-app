package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.LowStockItem
import com.rodvarled.admin.data.remote.dto.ProductCategory
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.ProductSaveRequest
import com.rodvarled.admin.data.remote.dto.SimpleLookup
import com.rodvarled.admin.data.remote.dto.StockAdjustRequest
import com.rodvarled.admin.data.remote.dto.StockAdjustResponse
import com.rodvarled.admin.data.remote.dto.StockMovement
import com.rodvarled.admin.data.remote.dto.UploadProductImageRequest
import com.rodvarled.admin.data.remote.dto.UploadProductImageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {
    @GET("api/Catalog/Categories")
    suspend fun getCategories(): List<ProductCategory>

    @GET("api/Catalog/Products")
    suspend fun getProducts(@Query("categoryId") categoryId: Int? = null): List<ProductDetail>

    @GET("api/Catalog/Products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDetail

    @GET("api/Catalog/BulbTypes")
    suspend fun getBulbTypes(): List<SimpleLookup>

    @POST("api/Catalog/Products")
    suspend fun createProduct(@Body request: ProductSaveRequest): ProductDetail

    @PUT("api/Catalog/Products/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body request: ProductSaveRequest)

    @DELETE("api/Catalog/Products/{id}")
    suspend fun deactivateProduct(@Path("id") id: Int)

    @GET("api/Catalog/Products/alerts/low-stock")
    suspend fun getLowStock(): List<LowStockItem>

    @POST("api/Catalog/Products/{id}/stock-adjust")
    suspend fun adjustStock(@Path("id") id: Int, @Body request: StockAdjustRequest): StockAdjustResponse

    @GET("api/Catalog/Products/{id}/stock-movements")
    suspend fun getStockMovements(@Path("id") id: Int): List<StockMovement>

    @POST("api/Catalog/Products/{id}/image")
    suspend fun uploadProductImage(@Path("id") id: Int, @Body request: UploadProductImageRequest): UploadProductImageResponse
}
