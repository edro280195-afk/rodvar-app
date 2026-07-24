package com.rodvarled.admin.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.rodvarled.admin.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val manrope = GoogleFont("Manrope")

// Fuente de marca (geométrica, técnica) descargada vía Google Play Services;
// si el proveedor no está disponible, Compose cae de vuelta a la fuente del
// sistema sin bloquear el render.
val ManropeFontFamily = FontFamily(
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
)
