package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BulbTypeVariant(
    val id: Int = 0,
    val bulbTypeId: Int,
    val bulbTypeName: String = "",
    val stock: Int,
    val priceOverride: Double? = null,
    val costOverride: Double? = null,
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
    val cost: Double = 0.0,
    val promotionPrice: Double? = null,
    val promotionLabel: String? = null,
    val promotionEndsAt: String? = null,
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
    val isActive: Boolean = true,
    val variants: List<BulbTypeVariant> = emptyList()
)

@Serializable
data class BulbTypeVariantSaveRequest(
    val bulbTypeId: Int,
    val stock: Int,
    val priceOverride: Double? = null,
    val costOverride: Double? = null,
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
    val cost: Double = 0.0,
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
    val note: String? = null,
    val unitCostUsd: Double? = null,
    val exchangeRate: Double? = null,
    val unitCostMxn: Double? = null,
    val purchaseDate: String? = null,
    val receivedDate: String? = null
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
    val createdAt: String? = null,
    val unitCostUsd: Double? = null,
    val exchangeRate: Double? = null,
    val unitCostMxn: Double? = null,
    val purchaseDate: String? = null,
    val receivedDate: String? = null
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

@Serializable
data class PriceRule(
    val id: Int,
    val productId: Int,
    val productBulbTypeId: Int? = null,
    val customerId: Int? = null,
    val name: String,
    val ruleType: String,
    val discountType: String,
    val value: Double,
    val minimumQuantity: Int = 1,
    val priority: Int = 0,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val isActive: Boolean = true,
    val productName: String = "",
    val variantName: String? = null,
    val customerName: String? = null
)

@Serializable
data class SavePriceRuleRequest(
    val productId: Int,
    val productBulbTypeId: Int? = null,
    val customerId: Int? = null,
    val name: String,
    val ruleType: String = "Promotion",
    val discountType: String = "FixedPrice",
    val value: Double,
    val minimumQuantity: Int = 1,
    val priority: Int = 0,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class InventoryUnit(
    val id: Int,
    val productId: Int,
    val productBulbTypeId: Int? = null,
    val code: String,
    val qrPayload: String,
    val status: String,
    val productName: String,
    val variantName: String? = null,
    val createdAt: String,
    val consumedAt: String? = null,
    val inventoryLotId: Int? = null,
    val purchaseUnitCostUsd: Double? = null,
    val purchaseExchangeRate: Double? = null,
    val purchaseUnitCostMxn: Double? = null,
    val purchaseDate: String? = null,
    val receivedDate: String? = null
)

/** Entrada de mercancía en un solo paso: stock + costo por pieza + (opcional) etiquetas QR. */
@Serializable
data class ReceiveInventoryRequest(
    val productId: Int,
    /** Id de la variante (`BulbTypeVariant.id`), no el del tipo de foco. */
    val productBulbTypeId: Int? = null,
    val quantity: Int,
    val unitCostMxn: Double? = null,
    val unitCostUsd: Double? = null,
    val exchangeRate: Double? = null,
    val note: String? = null,
    val createLabels: Boolean = true
)

@Serializable
data class ReceiveInventoryResult(
    val productId: Int,
    val productBulbTypeId: Int? = null,
    val quantityReceived: Int,
    val unitCostMxn: Double,
    val newStock: Int,
    val units: List<InventoryUnit> = emptyList()
)

@Serializable
data class CreateInventoryLabelsRequest(
    val productId: Int,
    val productBulbTypeId: Int? = null,
    val quantity: Int
)
