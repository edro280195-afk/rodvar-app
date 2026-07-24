package com.rodvarled.admin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RodvarApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                "Rodvar · Notificaciones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Citas nuevas, cambios de estado y alertas de inventario"
            }
        )
    }

    companion object {
        const val CHANNEL_GENERAL = "rodvar_general"
    }
}
