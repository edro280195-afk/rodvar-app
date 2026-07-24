package com.rodvarled.admin.di

import javax.inject.Qualifier

/** Cliente sin interceptor de auth ni authenticator: solo para login/refresh (evita ciclo de DI). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainClient

/** Cliente con Bearer token automático + renovación transparente en 401: para el resto de la API. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient
