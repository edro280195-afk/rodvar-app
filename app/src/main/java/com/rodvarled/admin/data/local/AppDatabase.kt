package com.rodvarled.admin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedAppointmentEntity::class, CachedProductEntity::class, CachedCustomerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appointmentCacheDao(): AppointmentCacheDao
    abstract fun productCacheDao(): ProductCacheDao
    abstract fun customerCacheDao(): CustomerCacheDao
}
