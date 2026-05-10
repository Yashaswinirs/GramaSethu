package com.example.gramasethu

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseRepository {

    private val db = Firebase.firestore
    private val bridgesCollection = db.collection("bridges")

    // ── Seed bridges into Firestore if collection is empty ────────────────────
    suspend fun seedIfEmpty(bridges: List<BridgeEntity>) {
        val snapshot = bridgesCollection.get().await()
        if (snapshot.isEmpty) {
            val batch = db.batch()
            bridges.forEach { bridge ->
                val doc = bridgesCollection.document(bridge.id.toString())
                batch.set(doc, mapOf(
                    "id"          to bridge.id,
                    "name"        to bridge.name,
                    "village"     to bridge.village,
                    "latitude"    to bridge.latitude,
                    "longitude"   to bridge.longitude,
                    "status"      to bridge.status,
                    "lastUpdated" to bridge.lastUpdated
                ))
            }
            batch.commit().await()
        }
    }

    // ── Listen to all bridges in real time ────────────────────────────────────
    // This Flow emits a new list every time ANY bridge changes in Firestore
    // meaning all users see updates within ~1-2 seconds
    fun listenToBridges(): Flow<List<BridgeEntity>> = callbackFlow {
        val listener = bridgesCollection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val bridges = snapshot.documents.mapNotNull { doc ->
                try {
                    BridgeEntity(
                        id          = (doc.getLong("id") ?: 0).toInt(),
                        name        = doc.getString("name") ?: "",
                        village     = doc.getString("village") ?: "",
                        latitude    = doc.getDouble("latitude") ?: 0.0,
                        longitude   = doc.getDouble("longitude") ?: 0.0,
                        status      = doc.getString("status") ?: "Open",
                        lastUpdated = doc.getLong("lastUpdated") ?: 0L
                    )
                } catch (e: Exception) { null }
            }.sortedBy { it.id }
            trySend(bridges)
        }
        awaitClose { listener.remove() }
    }

    // ── Update a single bridge status ─────────────────────────────────────────
    // This write goes to Firestore → all listening devices get it within 3 seconds
    suspend fun updateStatus(id: Int, status: String, lastUpdated: Long) {
        bridgesCollection.document(id.toString())
            .update(mapOf(
                "status"      to status,
                "lastUpdated" to lastUpdated
            )).await()
    }
}