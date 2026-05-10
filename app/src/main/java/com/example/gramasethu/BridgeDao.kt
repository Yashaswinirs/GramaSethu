package com.example.gramasethu


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BridgeDao {

    // Observe all bridges as a live stream — UI auto-updates when data changes
    @Query("SELECT * FROM bridges ORDER BY id ASC")
    fun getAllBridges(): Flow<List<BridgeEntity>>

    // Insert or replace (used for seeding initial data + updates)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bridges: List<BridgeEntity>)

    // Update just status + timestamp for a single bridge
    @Query("UPDATE bridges SET status = :status, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, lastUpdated: Long)

    // Check if DB is empty (used to decide whether to seed)
    @Query("SELECT COUNT(*) FROM bridges")
    suspend fun count(): Int
}