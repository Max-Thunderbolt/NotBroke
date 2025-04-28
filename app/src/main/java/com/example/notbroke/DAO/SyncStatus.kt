package com.example.notbroke.DAO

/**
 * Enum class representing the synchronization status of a user profile
 */
enum class SyncStatus {
    SYNCED,         // Profile is synchronized with Firestore
    PENDING_CREATE, // Profile is pending creation in Firestore
    PENDING_UPDATE, // Profile is pending update in Firestore
    PENDING_DELETE  // Profile is pending deletion in Firestore
} 