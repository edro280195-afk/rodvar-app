package com.rodvarled.admin.data.repository

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AddressSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

data class SelectedAddress(
    val placeId: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

@Singleton
class GooglePlacesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apiKey: String by lazy {
        context.packageManager
            .getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            .metaData
            ?.getString("com.google.android.geo.API_KEY")
            ?.trim()
            .orEmpty()
    }

    private val placesClient: PlacesClient by lazy {
        check(apiKey.isNotBlank()) {
            "Configura MAPS_API_KEY en local.properties para buscar direcciones."
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context, apiKey)
        }
        Places.createClient(context)
    }

    suspend fun search(query: String): List<AddressSuggestion> {
        if (query.trim().length < 3) return emptyList()

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query.trim())
            .setCountries(listOf("MX"))
            .build()

        return placesClient.findAutocompletePredictions(request)
            .await()
            .autocompletePredictions
            .map { prediction ->
                AddressSuggestion(
                    placeId = prediction.placeId,
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString(),
                    fullText = prediction.getFullText(null).toString()
                )
            }
    }

    suspend fun getAddress(placeId: String): SelectedAddress {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        val request = FetchPlaceRequest.newInstance(placeId, fields)
        val place = placesClient.fetchPlace(request).await().place
        val coordinates: LatLng = checkNotNull(place.latLng) {
            "Google no devolvió las coordenadas de esta dirección."
        }

        return SelectedAddress(
            placeId = place.id ?: placeId,
            address = place.address.orEmpty(),
            latitude = coordinates.latitude,
            longitude = coordinates.longitude
        )
    }
}
