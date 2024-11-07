package com.example.progettoruggerilam.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.progettoruggerilam.util.GeofenceEntity
import com.example.progettoruggerilam.data.dao.GeofenceDao
import com.example.progettoruggerilam.data.dao.*
import com.example.progettoruggerilam.data.model.*
import com.example.progettoruggerilam.util.User

@Database(
    entities = [User::class, ActivityRecord::class, GeofenceEntity::class],
    version = 7, // Aggiornata la versione
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun geofenceDao(): GeofenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migrazione dalla versione 2 alla versione 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users RENAME COLUMN uid TO userId")
            }
        }

        // Migrazione dalla versione 3 alla versione 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN age INTEGER DEFAULT 0 NOT NULL")
            }
        }

        // Migrazione dalla versione 4 alla versione 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS area_presence_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        transitionType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        locationName TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migrazione dalla versione 5 alla versione 6 per creare la tabella geofences
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS geofences (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        radius REAL NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        userId INTEGER NOT NULL DEFAULT -1
                    )
                """.trimIndent())
            }
        }

        // Migrazione per aggiungere la colonna userId alla tabella esistente
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE geofences ADD COLUMN userId INTEGER NOT NULL DEFAULT -1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            Log.d("AppDatabase", "Creazione o accesso al database.")
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dataBaseWaveWatcher.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                Log.d("AppDatabase", "Database creato con successo.")
                instance
            }
        }
    }
}
