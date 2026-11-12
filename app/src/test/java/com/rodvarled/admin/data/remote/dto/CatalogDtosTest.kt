package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogDtosTest {

    @Test
    fun productUpdateKeepsAnExplicitEmptyVariantList() {
        val request = ProductSaveRequest(
            categoryId = 1,
            name = "Producto de prueba",
            slug = "producto-de-prueba",
            price = 100.0,
            warrantyMonths = 12,
            variants = emptyList()
        )

        val payload = Json.encodeToString(request)

        assertTrue(payload.contains("\"variants\":[]"))
    }
}
