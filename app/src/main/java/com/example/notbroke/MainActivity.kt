package com.example.notbroke

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MainActivity handles user authentication including:
 * 1. Email/Password Sign In
 * 2. Google Sign In
 * 3. Navigation to Registration
 * 4. Auto-login for previously authenticated users
 */
class MainActivity : AppCompatActivity() {
    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth
    // Firestore database instance for user data
    private lateinit var db: FirebaseFirestore
    // Request code for Google Sign In
    private val RC_SIGN_IN = 9001
    // Google Sign In Client
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase services
        auth = Firebase.auth
        db = Firebase.firestore

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("253659646538-fldqee1ap0svm2qrsojd4j1uhb8b4k99.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize UI elements
        val signUpButton = findViewById<MaterialButton>(R.id.registerLinkButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val googleSignInButton = findViewById<MaterialButton>(R.id.googleSignInButton)

        // Set up navigation to registration screen
        signUpButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Handle email/password login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate input fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable login button to prevent multiple submissions
            loginButton.isEnabled = false

            // Attempt to sign in with Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login successful - navigate to home screen
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        // Login failed - show error and re-enable button
                        Toast.makeText(this, "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                    }
                }
        }

        // Set up Google Sign In button
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    /**
     * Initiates the Google Sign In process using the Google Sign-In API.
     */
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /**
     * Handles the result of the Google Sign-In process.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Authenticates with Firebase using the Google ID token and handles user data.
     * @param idToken The Google ID token to use for authentication
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        // Create a Firebase credential from the Google ID token
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        
        // Sign in to Firebase with the credential
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Get the current user
                    val user = auth.currentUser
                    
                    // Check if this is a new user
                    if (task.result?.additionalUserInfo?.isNewUser == true) {
                        // Create a new user document in Firestore for first-time users
                        user?.let { firebaseUser ->
                            val userData = hashMapOf(
                                "email" to (firebaseUser.email ?: ""),
                                "username" to (firebaseUser.displayName ?: firebaseUser.email?.split("@")?.get(0) ?: "User"),
                                "createdAt" to System.currentTimeMillis()
                            )
                            
                            // Save user data to Firestore
                            db.collection("users").document(firebaseUser.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Existing user - just navigate to home screen
                        Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    // Authentication failed - show error message
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Checks if a user is already signed in when the activity starts.
     * If a user is signed in, they are automatically redirected to the home screen.
     */
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