package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BulbTypeVariant(
    val id: Int = 0,
    val bulbTypeId: Int,
    val bulbTypeName: String = "",
    val stock: Int,
    val priceOverride: Double? = null,
    val lowStockThreshold: Int? = null
)

@Serializable
data class ProductDetail(
    val id: Int,
    val categoryName: String = "",
    val bulbTypeName: String? = null,
    val name: String,
    val slug: String,
    val description: String? = null,
    val price: Double,
    val warrantyMonths: Int,
    val lumens: Int? = null,
    val colorTemperature: Int? = null,
    val wattage: Int? = null,
    val voltageRange: String? = null,
    val coolingSystem: String? = null,
    val mainImageUrl: String? = null,
    val categoryId: Int,
    val bulbTypeId: Int? = null,
    val stock: Int,
    val lowStockThreshold: Int? = null,
    val variants: List<BulbTypeVariant> = emptyList()
)

@Serializable
data class BulbTypeVariantSaveRequest(
    val bulbTypeId: Int,
    val stock: Int,
    val priceOverride: Double? = null,
    val lowStockThreshold: Int? = null
)

@Serializable
data class ProductSaveRequest(
    val categoryId: Int,
    val bulbTypeId: Int? = null,
    val name: String,
    val slug: String,
    val description: String? = null,
    val price: Double,
    val warrantyMonths: Int,
    val lumens: Int? = null,
    val colorTemperature: Int? = null,
    val wattage: Int? = null,
    val voltageRange: String? = null,
    val coolingSystem: String? = null,
    val mainImageUrl: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val stock: Int = 0,
    val lowStockThreshold: Int? = null,
    val variants: List<BulbTypeVariantSaveRequest>? = null
)

@Serializable
data class ProductCategory(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    val iconUrl: String? = null
)

@Serializable
data class SimpleLookup(
    val id: Int,
    val name: String
)

@Serializable
data class StockAdjustRequest(
    val bulbTypeId: Int? = null,
    val delta: Int,
    val reason: String,
    val note: String? = null
)

@Serializable
data class StockAdjustResponse(
    val productId: Int,
    val bulbTypeId: Int? = null,
    val newStock: Int
)

@Serializable
data class StockMovement(
    val id: Int,
    val delta: Int,
    val reason: String,
    val note: String? = null,
    val bulbTypeName: String? = null,
    val createdByUserName: String? = null,
    val createdAt: String? = null
)

@Serializable
data class LowStockItem(
    val productId: Int,
    val productName: String,
    val mainImageUrl: String? = null,
    val bulbTypeId: Int? = null,
    val bulbTypeName: String? = null,
    val stock: Int,
    val lowStockThreshold: Int
)

@Serializable
data class UploadProductImageRequest(
    val imageBase64: String
)

@Serializable
data class UploadProductImageResponse(
    val imageUrl: String
)

@Serializable
data class ProductImportResult(
    val creados: Int = 0,
    val actualizados: Int = 0,
    val errores: List<String> = emptyList()
)
