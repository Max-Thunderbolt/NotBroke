package com.example.notbroke

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * LoginActivity is a redirect activity that sends users to MainActivity
 * which already contains the login functionality.
 */
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to MainActivity which handles login
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 