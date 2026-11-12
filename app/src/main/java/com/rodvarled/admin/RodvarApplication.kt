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

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            "Rodvar · General",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Novedades y alertas del sistema"
        }

        val appointmentsChannel = NotificationChannel(
            CHANNEL_APPOINTMENTS,
            "Rodvar · Recordatorio de Citas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones y recordatorios de citas próximas"
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(generalChannel, appointmentsChannel))
    }

    companion object {
        const val CHANNEL_GENERAL = "rodvar_general"
        const val CHANNEL_APPOINTMENTS = "rodvar_appointments"
    }
}
