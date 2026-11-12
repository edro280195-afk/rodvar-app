package com.rodvarled.admin.core.pricing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeMathTest {

    private fun line(
        key: String,
        productId: Int = 1,
        variantId: Int? = null,
        quantity: Int = 1,
        listUnitPrice: Double = 1000.0,
        unitPrice: Double = listUnitPrice,
        codes: List<String> = emptyList()
    ) = ChargeLine(key, productId, variantId, "Producto $productId", null, quantity, listUnitPrice, unitPrice, codes)

    @Test
    fun totalIsTheSumOfLines() {
        val state = ChargeState(lines = listOf(line("a", unitPrice = 1000.0), line("b", productId = 2, quantity = 2, unitPrice = 500.0)))

        assertEquals(2000.0, state.total, 0.0)
        assertNull(state.totalOverride)
    }

    @Test
    fun typingTheTotalSplitsItAcrossLinesProportionally() {
        // "Un par en 1,000 y otro par en 1,000... déjame los dos en 1,800"
        val state = ChargeState(lines = listOf(line("a"), line("b", productId = 2)))

        val result = ChargeMath.withTotal(state, 1800.0)

        assertEquals(1800.0, result.total, 0.0)
        assertEquals(1800.0, result.linesTotal, 0.0)
        assertEquals(900.0, result.lines[0].unitPrice, 0.0)
        assertEquals(900.0, result.lines[1].unitPrice, 0.0)
        assertTrue(result.lines.all { it.isDiscounted })
        assertEquals(200.0, result.discount, 0.0)
    }

    @Test
    fun splitKeepsProportionBetweenDifferentPrices() {
        val state = ChargeState(lines = listOf(line("a", unitPrice = 1000.0), line("b", productId = 2, unitPrice = 500.0)))

        val result = ChargeMath.withTotal(state, 1200.0)

        assertEquals(800.0, result.lines[0].unitPrice, 0.0)
        assertEquals(400.0, result.lines[1].unitPrice, 0.0)
    }

    @Test
    fun splitAbsorbsCentsOfRoundingInTheLastLine() {
        val state = ChargeState(lines = listOf(line("a", listUnitPrice = 100.0), line("b", productId = 2, listUnitPrice = 100.0), line("c", productId = 3, listUnitPrice = 100.0)))

        val result = ChargeMath.withTotal(state, 100.0)

        assertEquals(100.0, result.linesTotal, 0.0)
        assertEquals(33.33, result.lines[0].unitPrice, 0.0)
        assertEquals(33.34, result.lines[2].unitPrice, 0.0)
    }

    @Test
    fun splitWorksEvenWhenAllLinesAreFree() {
        val state = ChargeState(lines = listOf(line("a", unitPrice = 0.0), line("b", productId = 2, quantity = 2, unitPrice = 0.0)))

        val result = ChargeMath.withTotal(state, 300.0)

        assertEquals(100.0, result.lines[0].unitPrice, 0.0)
        assertEquals(100.0, result.lines[1].unitPrice, 0.0)
        assertEquals(300.0, result.linesTotal, 0.0)
    }

    @Test
    fun creditedVisitIsSubtractedFromTheTotal() {
        val state = ChargeState(lines = listOf(line("a")), visitFeeCredit = 200.0)

        assertEquals(800.0, state.total, 0.0)
    }

    @Test
    fun typingTheTotalWithCreditKeepsLinesAtTheRightAmount() {
        val state = ChargeState(lines = listOf(line("a")), visitFeeCredit = 200.0)

        // El cliente paga 700 en total: con la revisión bonificada, los productos quedan en 900.
        val result = ChargeMath.withTotal(state, 700.0)

        assertEquals(700.0, result.total, 0.0)
        assertEquals(900.0, result.lines[0].unitPrice, 0.0)
    }

    @Test
    fun visitOnlyTotalIsTheVisitFee() {
        val state = ChargeState(visitFeeCharged = 200.0)

        assertEquals(200.0, state.total, 0.0)

        val renegotiated = ChargeMath.withTotal(state, 150.0)
        assertEquals(150.0, renegotiated.total, 0.0)
        assertEquals(150.0, renegotiated.visitFeeCharged, 0.0)
    }

    @Test
    fun editingALinePriceDropsTheTypedTotal() {
        val typed = ChargeMath.withTotal(ChargeState(lines = listOf(line("a"), line("b", productId = 2))), 1800.0)

        val edited = ChargeMath.withUnitPrice(typed, "a", 950.0)

        assertNull(edited.totalOverride)
        assertEquals(1850.0, edited.total, 0.0)
    }

    @Test
    fun addingTheSameProductAgainOnlyAddsAPiece() {
        val state = ChargeMath.withAddedLine(ChargeState(lines = listOf(line("a"))), line("zzz"))

        assertEquals(1, state.lines.size)
        assertEquals(2, state.lines[0].quantity)
        assertEquals(2000.0, state.total, 0.0)
    }

    @Test
    fun quantityBelowOneIsIgnored() {
        val state = ChargeState(lines = listOf(line("a")))

        assertEquals(state, ChargeMath.withQuantity(state, "a", 0))
    }

    @Test
    fun scannedCodeFillsAPieceWithoutCodeFirst() {
        val state = ChargeState(lines = listOf(line("a", quantity = 2)))

        val result = ChargeMath.withScannedUnit(state, "RVU-1") { line("new") }

        assertEquals(2, result.lines[0].quantity)
        assertEquals(listOf("RVU-1"), result.lines[0].inventoryUnitCodes)
    }

    @Test
    fun scannedCodeAddsAPieceWhenAllAlreadyHaveCodes() {
        val state = ChargeState(lines = listOf(line("a", quantity = 1, codes = listOf("RVU-1"))))

        val result = ChargeMath.withScannedUnit(state, "RVU-2") { line("new") }

        assertEquals(2, result.lines[0].quantity)
        assertEquals(listOf("RVU-1", "RVU-2"), result.lines[0].inventoryUnitCodes)
    }

    @Test
    fun scannedCodeOfANewProductCreatesALine() {
        val state = ChargeState(lines = listOf(line("a")))

        val result = ChargeMath.withScannedUnit(state, "RVU-9") { line("n", productId = 7, variantId = 3) }

        assertEquals(2, result.lines.size)
        assertEquals(7, result.lines[1].productId)
        assertEquals(listOf("RVU-9"), result.lines[1].inventoryUnitCodes)
    }

    @Test
    fun scannedCodeTwiceIsIgnored() {
        val state = ChargeState(lines = listOf(line("a", codes = listOf("RVU-1"))))

        assertEquals(state, ChargeMath.withScannedUnit(state, "RVU-1") { line("new") })
    }

    @Test
    fun scanProblemFlagsLinesWithFewerCodesThanPieces() {
        val ok = ChargeState(lines = listOf(line("a", quantity = 2, codes = listOf("RVU-1", "RVU-2")), line("b", productId = 2)))
        val partial = ChargeState(lines = listOf(line("a", quantity = 2, codes = listOf("RVU-1"))))

        assertNull(ChargeMath.scanProblem(ok))
        assertTrue(partial.lines[0].hasIncompleteScan)
        assertTrue(ChargeMath.scanProblem(partial)!!.contains("1 etiqueta(s) para 2 pieza(s)"))
    }

    @Test
    fun parseMoneyAcceptsCommasAndRejectsGarbage() {
        assertEquals(1200.5, ChargeMath.parseMoney("1,200.50")!!, 0.0)
        assertEquals(1200.0, ChargeMath.parseMoney("$1200")!!, 0.0)
        assertNull(ChargeMath.parseMoney(""))
        assertNull(ChargeMath.parseMoney("abc"))
        assertNull(ChargeMath.parseMoney("-5"))
    }

    @Test
    fun formatInputDropsDecimalsOnlyWhenExact() {
        assertEquals("1200", ChargeMath.formatInput(1200.0))
        assertEquals("1200.50", ChargeMath.formatInput(1200.5))
        assertEquals("0", ChargeMath.formatInput(0.0))
    }
}
