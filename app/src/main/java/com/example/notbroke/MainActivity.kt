package com.example.notbroke

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
<<<<<<< HEAD
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

=======

class MainActivity : AppCompatActivity() {
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

<<<<<<< HEAD
        // Initialize Firebase Auth
        auth = Firebase.auth

=======
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
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
<<<<<<< HEAD
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
=======
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            // TODO: Add login validation
            if (email.isNotEmpty() && password.isNotEmpty()) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                //Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                // Temporarily navigate to home
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
        }
    }
}