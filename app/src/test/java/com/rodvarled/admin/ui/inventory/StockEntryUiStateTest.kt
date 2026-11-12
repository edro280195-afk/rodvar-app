package com.rodvarled.admin.ui.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockEntryUiStateTest {

    @Test
    fun costInPesosIsUsedAsIs() {
        val state = StockEntryUiState(costMxn = 85.5)

        assertEquals(85.5, state.effectiveCostMxn, 0.0)
    }

    @Test
    fun costInDollarsIsConvertedWithTheExchangeRate() {
        val state = StockEntryUiState(costMxn = 10.0, showUsd = true, usd = 4.0, exchangeRate = 17.25)

        assertEquals(69.0, state.effectiveCostMxn, 0.0)
    }

    @Test
    fun incompleteDollarsFallBackToPesos() {
        val state = StockEntryUiState(costMxn = 50.0, showUsd = true, usd = 4.0, exchangeRate = 0.0)

        assertEquals(50.0, state.effectiveCostMxn, 0.0)
    }

    @Test
    fun receivingOnlyPrintsWhenTheSwitchIsOn() {
        assertTrue(StockEntryUiState(mode = StockEntryMode.RECEIVE, printLabels = true).willPrint)
        assertFalse(StockEntryUiState(mode = StockEntryMode.RECEIVE, printLabels = false).willPrint)
    }

    @Test
    fun labelsModeAlwaysPrints() {
        assertTrue(StockEntryUiState(mode = StockEntryMode.LABELS, printLabels = false).willPrint)
    }
}
