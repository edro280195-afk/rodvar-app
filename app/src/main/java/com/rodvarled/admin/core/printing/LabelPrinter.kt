package com.rodvarled.admin.core.printing

import android.content.Context
import com.rodvarled.admin.data.remote.dto.InventoryUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imprime etiquetas QR en la NIIMBOT B1 sin que el usuario tenga que buscar la impresora cada vez:
 * recuerda la última que funcionó y se conecta directo a ella; solo si falla vuelve a buscarla.
 */
@Singleton
class LabelPrinter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Nombre de la última impresora que imprimió bien, o null si todavía no hay ninguna. */
    val lastPrinterName: String? get() = prefs.getString(KEY_NAME, null)

    fun hasPermission(): Boolean = NiimbotB1Printer.hasPermission(context)

    /**
     * Imprime una etiqueta por unidad (`copies` veces cada una) y devuelve el nombre de la impresora usada.
     * Lanza una excepción con un mensaje para el usuario si no se pudo imprimir.
     */
    suspend fun print(
        units: List<InventoryUnit>,
        copies: Int = 1,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): String {
        require(units.isNotEmpty()) { "No hay etiquetas para imprimir." }

        val labels = units.map { InventoryLabelRenderer.render(it.productName, it.variantName, it.code) }

        val savedAddress = prefs.getString(KEY_ADDRESS, null)
        if (savedAddress != null) {
            try {
                NiimbotB1Printer.print(context, savedAddress, labels, copies, onProgress)
                return lastPrinterName ?: "NIIMBOT B1"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // La impresora guardada no respondió (apagada, otra cerca, cambió de dirección): se busca de nuevo.
            }
        }

        val printer = NiimbotB1Printer.scan(context).firstOrNull()
            ?: error("No encontramos la NIIMBOT. Enciéndela y acércala al teléfono.")

        NiimbotB1Printer.print(context, printer.address, labels, copies, onProgress)
        prefs.edit().putString(KEY_ADDRESS, printer.address).putString(KEY_NAME, printer.name).apply()
        return printer.name
    }

    private companion object {
        const val PREFS_NAME = "label_printer"
        const val KEY_ADDRESS = "address"
        const val KEY_NAME = "name"
    }
}
