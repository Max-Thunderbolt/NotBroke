package com.example.notbroke.DAO

/**
 * Enum class representing the synchronization status of entities
 */
enum class SyncStatus {
    SYNCED,           // Entity is synchronized with Firestore
    PENDING_CREATE,   // Entity is pending creation in Firestore
    PENDING_UPLOAD,   // Entity is pending upload to Firestore
    PENDING_UPDATE,   // Entity is pending update in Firestore
    PENDING_DELETE    // Entity is pending deletion in Firestore
} 