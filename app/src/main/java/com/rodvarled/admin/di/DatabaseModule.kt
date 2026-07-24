package com.rodvarled.admin.di

import android.content.Context
import androidx.room.Room
import com.rodvarled.admin.data.local.AppDatabase
import com.rodvarled.admin.data.local.AppointmentCacheDao
import com.rodvarled.admin.data.local.CustomerCacheDao
import com.rodvarled.admin.data.local.ProductCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "rodvar_cache.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideAppointmentCacheDao(db: AppDatabase): AppointmentCacheDao = db.appointmentCacheDao()

    @Provides
    fun provideProductCacheDao(db: AppDatabase): ProductCacheDao = db.productCacheDao()

    @Provides
    fun provideCustomerCacheDao(db: AppDatabase): CustomerCacheDao = db.customerCacheDao()
}
