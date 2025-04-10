package com.example.notbroke

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.widget.EditText
<<<<<<< HEAD
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

=======

class RegisterActivity : AppCompatActivity() {
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

<<<<<<< HEAD
        // Initialize Firebase Auth
        auth = Firebase.auth

=======
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
        // Initialize views
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val signUpButton = findViewById<MaterialButton>(R.id.signUpButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        // Set up click listeners
        backButton.setOnClickListener {
            finish()
        }

        signUpButton.setOnClickListener {
<<<<<<< HEAD
            val email = emailInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state
            signUpButton.isEnabled = false

            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Registration successful
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        // Registration failed
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                        signUpButton.isEnabled = true
                    }
                }
=======
            val email = emailInput.text.toString()
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            // TODO: Add registration validation
            if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                // TODO: Implement actual registration
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
        }
    }
} 