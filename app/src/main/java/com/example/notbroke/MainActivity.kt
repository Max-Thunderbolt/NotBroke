package com.example.notbroke

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize views
        val signUpButton = findViewById<MaterialButton>(R.id.registerLinkButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)

        // Set up click listeners
        signUpButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state
            loginButton.isEnabled = false

            // Sign in with email and password
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login successful
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        // Login failed
                        Toast.makeText(this, "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in and update UI accordingly
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already signed in, go to home screen
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}