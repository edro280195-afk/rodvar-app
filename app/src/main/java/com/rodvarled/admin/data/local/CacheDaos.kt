package com.rodvarled.admin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentCacheDao {
    @Query("SELECT * FROM cached_appointments ORDER BY requestedDate DESC")
    fun observeAll(): Flow<List<CachedAppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedAppointmentEntity>)

    @Query("DELETE FROM cached_appointments")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<CachedAppointmentEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface ProductCacheDao {
    @Query("SELECT * FROM cached_products ORDER BY name ASC")
    fun observeAll(): Flow<List<CachedProductEntity>>

    @Query("SELECT * FROM cached_products WHERE isLowStock = 1 ORDER BY name ASC")
    fun observeLowStock(): Flow<List<CachedProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedProductEntity>)

    @Query("DELETE FROM cached_products")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<CachedProductEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface CustomerCacheDao {
    @Query("SELECT * FROM cached_customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CachedCustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCustomerEntity>)

    @Query("DELETE FROM cached_customers")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<CachedCustomerEntity>) {
        clear()
        insertAll(items)
    }
}
