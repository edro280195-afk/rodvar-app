package com.rodvarled.admin.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openGoogleMapsNavigation(
    context: Context,
    address: String?,
    latitude: Double?,
    longitude: Double?
) {
    val destination = if (latitude != null && longitude != null) {
        "${latitude},${longitude}"
    } else {
        address?.trim().orEmpty()
    }
    if (destination.isBlank()) return

    val uri = if (latitude != null && longitude != null) {
        Uri.parse("google.navigation:q=$destination")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
    }

    val googleMapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    runCatching { context.startActivity(googleMapsIntent) }
        .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}
