package com.example.gramasethu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bridges")
data class BridgeEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val village: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val lastUpdated: Long
)