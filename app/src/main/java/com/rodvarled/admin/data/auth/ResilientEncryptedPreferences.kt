package com.rodvarled.admin.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Crea preferencias cifradas y recupera el archivo si Android conserva un keyset
 * incompatible después de reinstalar o restaurar la aplicación.
 */
internal fun createResilientEncryptedPreferences(
    context: Context,
    name: String
): SharedPreferences {
    fun create(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    return runCatching { create() }.getOrElse { firstError ->
        Log.w(TAG, "Preferencias cifradas incompatibles; se limpiará únicamente $name", firstError)
        context.deleteSharedPreferences(name)

        runCatching { create() }.getOrElse { secondError ->
            Log.e(TAG, "No se pudieron recrear las preferencias cifradas $name", secondError)
            // El archivo se reinició arriba; este respaldo evita que la app quede inutilizable
            // si el proveedor Keystore del dispositivo sigue fallando durante el arranque.
            context.getSharedPreferences("${name}_recovery", Context.MODE_PRIVATE)
        }
    }
}

private const val TAG = "RodvarSecurePrefs"
