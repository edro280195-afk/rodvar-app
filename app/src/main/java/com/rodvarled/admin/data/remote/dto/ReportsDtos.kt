package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PricingReport(
    val from: String,
    val to: String,
    val installationCount: Int,
    val grossAmount: Double,
    val discountAmount: Double,
    val netAmount: Double,
    val costAmount: Double = 0.0,
    val profitAmount: Double = 0.0,
    val warrantyClaimCostAmount: Double = 0.0,
    val netProfitAmount: Double = 0.0,
    val warrantyClaimCount: Int = 0,
    val installationsWithCost: Int = 0,
    val byProduct: List<PricingReportLine> = emptyList(),
    val byRule: List<PricingReportLine> = emptyList()
)

@Serializable
data class PricingReportLine(
    val name: String,
    val quantity: Int,
    val grossAmount: Double,
    val discountAmount: Double,
    val netAmount: Double,
    val costAmount: Double = 0.0,
    val profitAmount: Double = 0.0
)
