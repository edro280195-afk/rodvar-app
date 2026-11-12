package com.rodvarled.admin.core.util

import android.net.Uri

data class CountryCode(
    val code: String,
    val countryName: String,
    val flagEmoji: String
)

object PhoneUtils {
    val DEFAULT_COUNTRY = CountryCode("+52", "México", "🇲🇽")

    val POPULAR_COUNTRIES: List<CountryCode> = listOf(
        CountryCode("+52", "México", "🇲🇽"),
        CountryCode("+1", "Estados Unidos", "🇺🇸"),
        CountryCode("+1", "Canadá", "🇨🇦"),
        CountryCode("+502", "Guatemala", "🇬🇹"),
        CountryCode("+503", "El Salvador", "🇸🇻"),
        CountryCode("+504", "Honduras", "🇭🇳"),
        CountryCode("+506", "Costa Rica", "🇨🇷"),
        CountryCode("+507", "Panamá", "🇵🇦"),
        CountryCode("+57", "Colombia", "🇨🇴"),
        CountryCode("+34", "España", "🇪🇸"),
        CountryCode("+54", "Argentina", "🇦🇷"),
        CountryCode("+56", "Chile", "🇨🇱"),
        CountryCode("+51", "Perú", "🇵🇪"),
        CountryCode("+58", "Venezuela", "🇻🇪"),
        CountryCode("+55", "Brasil", "🇧🇷")
    )

    /**
     * Devuelve la bandera asociada a una lada/código, o 🌐 si es personalizada.
     */
    fun getFlagForCode(code: String): String {
        val cleanCode = if (code.startsWith("+")) code else "+$code"
        return POPULAR_COUNTRIES.firstOrNull { it.code == cleanCode }?.flagEmoji ?: "🌐"
    }

    /**
     * Separa un teléfono en (Lada/Código de país, Número local).
     * Si no tiene lada explícita (+...), asigna la lada por defecto (+52 México).
     */
    fun parsePhoneNumber(raw: String, defaultCode: String = "+52"): Pair<String, String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return Pair(defaultCode, "")
        }

        // Caso 1: Comienza con + (ej. "+52 8671234567", "+1 9561234567", "+528671234567")
        if (trimmed.startsWith("+")) {
            // Si tiene espacio después del código
            val spaceIndex = trimmed.indexOf(' ')
            if (spaceIndex in 2..6) {
                val code = trimmed.substring(0, spaceIndex)
                val number = trimmed.substring(spaceIndex + 1).trim()
                return Pair(code, number)
            }

            // Si no tiene espacio, probar matching con códigos conocidos (los más largos primero)
            val sortedCodes = POPULAR_COUNTRIES.map { it.code }.distinct().sortedByDescending { it.length }
            for (knownCode in sortedCodes) {
                if (trimmed.startsWith(knownCode)) {
                    val number = trimmed.removePrefix(knownCode).trim()
                    return Pair(knownCode, number)
                }
            }

            // Código personalizado sin espacio: tomar el '+' y los primeros 1-4 dígitos
            val digits = trimmed.substring(1)
            val codeLen = digits.takeWhile { it.isDigit() }.length.coerceIn(1, 4)
            val customCode = "+" + digits.take(codeLen)
            val number = digits.drop(codeLen).trim()
            return Pair(customCode, number)
        }

        // Caso 2: Sin '+'
        val onlyDigits = trimmed.filter { it.isDigit() }
        return when {
            // 12 dígitos comenzando con 52 (México)
            onlyDigits.length == 12 && onlyDigits.startsWith("52") -> {
                Pair("+52", onlyDigits.substring(2))
            }
            // 11 dígitos comenzando con 1 (USA/Canadá)
            onlyDigits.length == 11 && onlyDigits.startsWith("1") -> {
                Pair("+1", onlyDigits.substring(1))
            }
            // 10 dígitos locales estándar
            onlyDigits.length == 10 -> {
                Pair(defaultCode, onlyDigits)
            }
            // Cualquier otro formato
            else -> {
                Pair(defaultCode, trimmed)
            }
        }
    }

    /**
     * Combina lada y número nacional de manera normalizada.
     * Ej: "+52 8671234567" o "+1 9561234567".
     */
    fun formatFullPhone(countryCode: String, nationalNumber: String): String {
        val cleanNumber = nationalNumber.trim()
        if (cleanNumber.isBlank()) return ""
        val cleanCode = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
        return "$cleanCode $cleanNumber"
    }

    /**
     * Construye un enlace oficial a WhatsApp sin hardcodear nunca +52.
     * Respeta la lada del número (ya sea México +52, USA +1 u otra).
     */
    fun buildWhatsAppUrl(phone: String, message: String? = null): String {
        val clean = phone.replace(Regex("[^0-9+]"), "")
        if (clean.isBlank()) {
            return if (!message.isNullOrBlank()) {
                "https://wa.me/?text=${Uri.encode(message)}"
            } else {
                "https://wa.me/"
            }
        }

        val digitsForWa = if (clean.startsWith("+")) {
            clean.removePrefix("+")
        } else {
            val onlyDigits = clean.filter { it.isDigit() }
            when {
                onlyDigits.length == 12 && onlyDigits.startsWith("52") -> onlyDigits
                onlyDigits.length == 11 && onlyDigits.startsWith("1") -> onlyDigits
                onlyDigits.length == 10 -> "52$onlyDigits"
                else -> onlyDigits
            }
        }

        return if (!message.isNullOrBlank()) {
            "https://wa.me/$digitsForWa?text=${Uri.encode(message)}"
        } else {
            "https://wa.me/$digitsForWa"
        }
    }
}
