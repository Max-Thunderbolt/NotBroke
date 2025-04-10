package com.example.notbroke

import android.app.Application
import com.google.firebase.FirebaseApp

class NotBrokeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 