package com.rodvarled.admin.core.pricing

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Un renglón del cobro: qué se instala, a qué precio de lista y a cuánto se cobra.
 *
 * `variantId` es el id de la variante del producto (el mismo que usa la API como `productBulbTypeId`),
 * no el id del tipo de foco.
 */
data class ChargeLine(
    val key: String,
    val productId: Int,
    val variantId: Int?,
    val name: String,
    val variantName: String?,
    val quantity: Int,
    /** Precio de lista unitario: sirve para mostrar cuánto se rebajó. */
    val listUnitPrice: Double,
    /** Precio unitario que realmente se cobra. */
    val unitPrice: Double,
    val inventoryUnitCodes: List<String> = emptyList()
) {
    val amount: Double get() = ChargeMath.round2(unitPrice * quantity)
    val listAmount: Double get() = ChargeMath.round2(listUnitPrice * quantity)
    val isDiscounted: Boolean get() = unitPrice < listUnitPrice

    /**
     * Hay códigos QR escaneados, pero no uno por cada pieza. La API exige que coincidan
     * (o que no haya ninguno), así que se avisa antes de intentar cerrar el trabajo.
     */
    val hasIncompleteScan: Boolean get() = inventoryUnitCodes.isNotEmpty() && inventoryUnitCodes.size != quantity
}

/**
 * Todo lo que compone el total de un trabajo.
 *
 * total = renglones + revisión cobrada − revisión bonificada, salvo que se haya tecleado
 * un total a mano (`totalOverride`): en ese caso los renglones se repartieron para cuadrar con él.
 */
data class ChargeState(
    val lines: List<ChargeLine> = emptyList(),
    /** Revisión a domicilio que se cobra en este trabajo (ya incluida en el total). */
    val visitFeeCharged: Double = 0.0,
    /** Revisión pagada antes que se bonifica ahora (ya restada del total). */
    val visitFeeCredit: Double = 0.0,
    /** Total tecleado a mano ("el par en 1,800"); null = se calcula con los renglones. */
    val totalOverride: Double? = null
) {
    val linesTotal: Double get() = ChargeMath.round2(lines.sumOf { it.amount })
    val listTotal: Double get() = ChargeMath.round2(lines.sumOf { it.listAmount })
    val computedTotal: Double get() = ChargeMath.round2(max(0.0, linesTotal + visitFeeCharged - visitFeeCredit))
    val total: Double get() = totalOverride ?: computedTotal

    /** Cuánto se rebajó de los productos respecto a su precio de lista (0 si no hubo rebaja). */
    val discount: Double get() = ChargeMath.round2(max(0.0, listTotal - (total - visitFeeCharged + visitFeeCredit)))
}

/** Operaciones sobre el cobro. Todas devuelven un estado nuevo; no tocan nada de Android. */
object ChargeMath {

    fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    /** "1,200.50" → 1200.5; vacío o texto inválido → null. */
    fun parseMoney(text: String): Double? {
        val clean = text.replace(",", "").replace("$", "").trim()
        if (clean.isEmpty()) return null
        return clean.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    }

    /** Mensaje para el usuario si algún renglón tiene QR escaneados que no cuadran con sus piezas; null si todo bien. */
    fun scanProblem(state: ChargeState): String? = state.lines.firstOrNull { it.hasIncompleteScan }?.let {
        "En «${it.name}» escaneaste ${it.inventoryUnitCodes.size} etiqueta(s) para ${it.quantity} pieza(s). " +
            "Escanea una por pieza o quita los códigos."
    }

    /** Pesos sin decimales cuando son exactos ("1200"), con dos decimales si no ("1200.50"). */
    fun formatInput(value: Double): String {
        val rounded = round2(value)
        return if (rounded == floor(rounded)) rounded.toLong().toString() else "%.2f".format(java.util.Locale.US, rounded)
    }

    /** Cambia el precio de un renglón. Descarta cualquier total tecleado antes. */
    fun withUnitPrice(state: ChargeState, key: String, unitPrice: Double): ChargeState = state.copy(
        lines = state.lines.map { if (it.key == key) it.copy(unitPrice = round2(max(0.0, unitPrice))) else it },
        totalOverride = null
    )

    fun withQuantity(state: ChargeState, key: String, quantity: Int): ChargeState {
        if (quantity < 1) return state
        return state.copy(
            lines = state.lines.map { if (it.key == key) it.copy(quantity = quantity) else it },
            totalOverride = null
        )
    }

    fun withoutLine(state: ChargeState, key: String): ChargeState =
        state.copy(lines = state.lines.filterNot { it.key == key }, totalOverride = null)

    /** Agrega un renglón; si ya hay uno del mismo producto y variante, solo suma una pieza. */
    fun withAddedLine(state: ChargeState, line: ChargeLine): ChargeState {
        val existing = state.lines.firstOrNull { it.productId == line.productId && it.variantId == line.variantId }
        val lines = if (existing != null) {
            state.lines.map { if (it === existing) it.copy(quantity = it.quantity + line.quantity) else it }
        } else {
            state.lines + line
        }
        return state.copy(lines = lines, totalOverride = null)
    }

    fun withVisitFeeCharged(state: ChargeState, amount: Double): ChargeState =
        state.copy(visitFeeCharged = round2(max(0.0, amount)), totalOverride = null)

    fun withVisitFeeCredit(state: ChargeState, amount: Double): ChargeState =
        state.copy(visitFeeCredit = round2(max(0.0, amount)), totalOverride = null)

    /**
     * El usuario fijó el total ("el par en 1,800"): los precios de los renglones se reparten
     * proporcionalmente para que sumen eso, descontando la revisión cobrada/bonificada.
     */
    fun withTotal(state: ChargeState, target: Double): ChargeState {
        val total = round2(max(0.0, target))

        // Revisión sola (sin productos): lo que se teclea es lo que se cobra por la revisión.
        if (state.lines.isEmpty() && state.visitFeeCharged > 0.0) {
            return state.copy(visitFeeCharged = round2(total + state.visitFeeCredit), totalOverride = total)
        }

        val linesTarget = max(0.0, round2(total - state.visitFeeCharged + state.visitFeeCredit))
        return state.copy(lines = prorate(state.lines, linesTarget), totalOverride = total)
    }

    /**
     * Asocia un código QR escaneado con un renglón:
     *  1. al renglón de ese producto/variante que aún tenga piezas sin código;
     *  2. si todas ya tienen código, se suma una pieza (el cliente pidió otra);
     *  3. si no hay renglón de ese producto, se crea con `newLine`.
     * Un código ya escaneado se ignora.
     */
    fun withScannedUnit(state: ChargeState, code: String, newLine: () -> ChargeLine): ChargeState {
        if (state.lines.any { code in it.inventoryUnitCodes }) return state

        val productId = newLine().productId
        val variantId = newLine().variantId

        val withRoom = state.lines.firstOrNull {
            it.productId == productId && it.variantId == variantId && it.inventoryUnitCodes.size < it.quantity
        }
        if (withRoom != null) {
            return state.copy(
                lines = state.lines.map { if (it === withRoom) it.copy(inventoryUnitCodes = it.inventoryUnitCodes + code) else it },
                totalOverride = null
            )
        }

        val sameProduct = state.lines.lastOrNull { it.productId == productId && it.variantId == variantId }
        if (sameProduct != null) {
            return state.copy(
                lines = state.lines.map {
                    if (it === sameProduct) it.copy(quantity = it.quantity + 1, inventoryUnitCodes = it.inventoryUnitCodes + code) else it
                },
                totalOverride = null
            )
        }

        return state.copy(
            lines = state.lines + newLine().copy(quantity = 1, inventoryUnitCodes = listOf(code)),
            totalOverride = null
        )
    }

    private fun prorate(lines: List<ChargeLine>, target: Double): List<ChargeLine> {
        if (lines.isEmpty()) return lines

        val currentTotal = lines.sumOf { it.unitPrice * it.quantity }
        val totalUnits = lines.sumOf { it.quantity }

        val scaled = lines.map { line ->
            val newUnit = if (currentTotal > 0.0) {
                round2(line.unitPrice * (target / currentTotal))
            } else {
                round2(target / totalUnits)
            }
            line.copy(unitPrice = newUnit)
        }.toMutableList()

        // Los redondeos a centavos pueden dejar unos centavos de diferencia: se absorben en el último renglón.
        val residual = round2(target - scaled.sumOf { it.unitPrice * it.quantity })
        if (residual != 0.0) {
            val last = scaled.last()
            scaled[scaled.lastIndex] = last.copy(unitPrice = round2(max(0.0, last.unitPrice + residual / last.quantity)))
        }
        return scaled
    }
}
