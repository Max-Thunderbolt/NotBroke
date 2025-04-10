package com.example.notbroke

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // UI Elements
    private lateinit var backButton: ImageButton
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var signOutButton: MaterialButton
    private lateinit var deleteAccountButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Initialize UI elements
        backButton = findViewById(R.id.backButton)
        profileName = findViewById(R.id.profileName)
        profileEmail = findViewById(R.id.profileEmail)
        signOutButton = findViewById(R.id.signOutButton)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)

        // Set up click listeners
        backButton.setOnClickListener {
            finish()
        }

        signOutButton.setOnClickListener {
            signOut()
        }

        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // Load user profile data
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set email from Firebase Auth
            profileEmail.text = currentUser.email

            // Load additional user data from Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Set username if available
                        val username = document.getString("username")
                        if (username != null) {
                            profileName.text = username
                        } else {
                            // Use email username part as default
                            val emailUsername = currentUser.email?.split("@")?.get(0) ?: "User"
                            profileName.text = emailUsername
                        }
                    } else {
                        // No user document exists yet, use email username as default
                        val emailUsername = currentUser.email?.split("@")?.get(0) ?: "User"
                        profileName.text = emailUsername
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // User not logged in, redirect to login
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // First delete user data from Firestore
            db.collection("users").document(currentUser.uid)
                .delete()
                .addOnSuccessListener {
                    // Then delete the user account
                    currentUser.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error deleting account: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error deleting user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun signOut() {
        auth.signOut()
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 