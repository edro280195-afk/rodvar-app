package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

/** Tarifas de servicio que se publican al cliente (WhatsApp, portal y sitio web). */
@Serializable
data class ServiceFees(
    val homeVisitFee: Double
)

/** Tarifa de revisión a domicilio que se usa si todavía no se pudo consultar la del servidor. */
const val DEFAULT_HOME_VISIT_FEE = 200.0
