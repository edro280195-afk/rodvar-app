package com.rodvarled.admin.di

import com.rodvarled.admin.BuildConfig
import com.rodvarled.admin.data.auth.AuthInterceptor
import com.rodvarled.admin.data.auth.TokenAuthenticator
import com.rodvarled.admin.data.remote.api.AppointmentsApi
import com.rodvarled.admin.data.remote.api.AuthApi
import com.rodvarled.admin.data.remote.api.CatalogApi
import com.rodvarled.admin.data.remote.api.CompatibilityApi
import com.rodvarled.admin.data.remote.api.CustomersApi
import com.rodvarled.admin.data.remote.api.GalleryApi
import com.rodvarled.admin.data.remote.api.InstallationsApi
import com.rodvarled.admin.data.remote.api.NotificationsApi
import com.rodvarled.admin.data.remote.api.QuotesApi
import com.rodvarled.admin.data.remote.api.ReviewsApi
import com.rodvarled.admin.data.remote.api.VehiclesApi
import com.rodvarled.admin.data.remote.api.WarrantiesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    @PlainClient
    fun providePlainOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedOkHttpClient(
        logging: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(authenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @PlainClient
    fun providePlainRetrofit(@PlainClient client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedRetrofit(@AuthenticatedClient client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    // Dagger expone automáticamente un Provider<AuthApi> con este mismo calificador a quien lo pida
    // (como TokenAuthenticator) — no se declara un @Provides propio para Provider<T>, Dagger lo prohíbe.
    @Provides
    @Singleton
    @PlainClient
    fun providePlainAuthApi(@PlainClient retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAppointmentsApi(@AuthenticatedClient retrofit: Retrofit): AppointmentsApi = retrofit.create(AppointmentsApi::class.java)

    @Provides
    @Singleton
    fun provideCustomersApi(@AuthenticatedClient retrofit: Retrofit): CustomersApi = retrofit.create(CustomersApi::class.java)

    @Provides
    @Singleton
    fun provideCatalogApi(@AuthenticatedClient retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)

    @Provides
    @Singleton
    fun provideInstallationsApi(@AuthenticatedClient retrofit: Retrofit): InstallationsApi = retrofit.create(InstallationsApi::class.java)

    @Provides
    @Singleton
    fun provideWarrantiesApi(@AuthenticatedClient retrofit: Retrofit): WarrantiesApi = retrofit.create(WarrantiesApi::class.java)

    @Provides
    @Singleton
    fun provideQuotesApi(@AuthenticatedClient retrofit: Retrofit): QuotesApi = retrofit.create(QuotesApi::class.java)

    @Provides
    @Singleton
    fun provideVehiclesApi(@AuthenticatedClient retrofit: Retrofit): VehiclesApi = retrofit.create(VehiclesApi::class.java)

    @Provides
    @Singleton
    fun provideCompatibilityApi(@AuthenticatedClient retrofit: Retrofit): CompatibilityApi = retrofit.create(CompatibilityApi::class.java)

    @Provides
    @Singleton
    fun provideGalleryApi(@AuthenticatedClient retrofit: Retrofit): GalleryApi = retrofit.create(GalleryApi::class.java)

    @Provides
    @Singleton
    fun provideReviewsApi(@AuthenticatedClient retrofit: Retrofit): ReviewsApi = retrofit.create(ReviewsApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationsApi(@AuthenticatedClient retrofit: Retrofit): NotificationsApi = retrofit.create(NotificationsApi::class.java)
}
