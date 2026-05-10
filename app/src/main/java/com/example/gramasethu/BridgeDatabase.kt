package com.example.gramasethu



import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BridgeEntity::class], version = 1, exportSchema = false)
abstract class BridgeDatabase : RoomDatabase() {

    abstract fun bridgeDao(): BridgeDao

    companion object {
        // Volatile ensures all threads see the same instance immediately
        @Volatile
        private var INSTANCE: BridgeDatabase? = null

        fun getInstance(context: Context): BridgeDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BridgeDatabase::class.java,
                    "gramasethu_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}