package com.rodvarled.admin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché de solo-lectura: cada fila guarda el DTO completo serializado en [json] más unas pocas
 * columnas indexadas para poder filtrar/ordenar sin tener que deserializar todo. Se reemplaza por
 * completo en cada refresh exitoso a la red (no hay escritura offline en esta versión).
 */
@Entity(tableName = "cached_appointments")
data class CachedAppointmentEntity(
    @PrimaryKey val id: Int,
    val status: String,
    val requestedDate: String,
    val customerName: String,
    val customerPhone: String,
    val createdAt: String?,
    val json: String
)

@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val categoryId: Int,
    val stock: Int,
    val isLowStock: Boolean,
    val isActive: Boolean,
    val json: String
)

@Entity(tableName = "cached_customers")
data class CachedCustomerEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val phone: String,
    val json: String
)
