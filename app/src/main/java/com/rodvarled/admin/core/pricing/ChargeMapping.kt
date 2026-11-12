package com.rodvarled.admin.core.pricing

import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.QuoteItemDetail
import java.util.UUID

/** Precio de lista con el que se ofrece un producto (variante, promoción vigente o precio base). */
fun ProductDetail.listPriceFor(variant: BulbTypeVariant?): Double =
    variant?.priceOverride ?: promotionPrice ?: price

/** Piezas disponibles del producto base o de la variante indicada. */
fun ProductDetail.stockFor(variant: BulbTypeVariant?): Int = variant?.stock ?: stock

fun ProductDetail.toChargeLine(variant: BulbTypeVariant?, quantity: Int = 1): ChargeLine {
    val listPrice = listPriceFor(variant)
    return ChargeLine(
        key = UUID.randomUUID().toString(),
        productId = id,
        variantId = variant?.id,
        name = name,
        variantName = variant?.bulbTypeName?.takeIf { it.isNotBlank() },
        quantity = quantity,
        listUnitPrice = listPrice,
        unitPrice = listPrice
    )
}

/** Renglón que viene de la cotización de una cita: conserva el precio ya ofrecido al cliente. */
fun QuoteItemDetail.toChargeLine(variantName: String?): ChargeLine = ChargeLine(
    key = UUID.randomUUID().toString(),
    productId = productId,
    variantId = productBulbTypeId,
    name = productName,
    variantName = variantName?.takeIf { it.isNotBlank() },
    quantity = quantity,
    listUnitPrice = listUnitPrice.takeIf { it > 0.0 } ?: unitPrice,
    unitPrice = unitPrice
)
