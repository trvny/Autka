package com.autka.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autka.data.local.AutkaDatabase
import com.autka.data.local.CarOfferDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE car_offers ADD COLUMN listingCount INTEGER")
            db.execSQL("ALTER TABLE car_offers ADD COLUMN latitude REAL")
            db.execSQL("ALTER TABLE car_offers ADD COLUMN longitude REAL")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AutkaDatabase =
        Room.databaseBuilder(context, AutkaDatabase::class.java, "autka.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCarOfferDao(db: AutkaDatabase): CarOfferDao = db.carOfferDao()
}
