package com.example.notbroke

<<<<<<< HEAD
import android.content.Intent
=======
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.notbroke.fragments.DashboardFragment
import com.example.notbroke.fragments.ProgressionFragment
import com.example.notbroke.fragments.HabitsFragment
import com.example.notbroke.fragments.DebtFragment

class HomeActivity : AppCompatActivity() {
    private val TAG = "HomeActivity"
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var dashboardMenuItem: LinearLayout
    private lateinit var profileMenuItem: LinearLayout
    private lateinit var settingsMenuItem: LinearLayout
    private lateinit var progressionMenuItem: LinearLayout
    private lateinit var habitsMenuItem: LinearLayout
    private lateinit var debtsMenuItem: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(TAG, "Starting onCreate")
            
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_home)
            
            // Initialize drawer components
            drawerLayout = findViewById(R.id.drawerLayout)
            menuButton = findViewById(R.id.menuButton)
            
            // Initialize drawer menu items
            dashboardMenuItem = findViewById(R.id.nav_dashboard_item)
            profileMenuItem = findViewById(R.id.nav_profile_item)
            settingsMenuItem = findViewById(R.id.nav_settings_item)
            progressionMenuItem = findViewById(R.id.nav_progression_item)
            habitsMenuItem = findViewById(R.id.nav_habits_item)
            debtsMenuItem = findViewById(R.id.nav_debts_item)
            
            // Setup drawer menu
            setupDrawerMenu()
            
            // Default fragment is Dashboard
            if (savedInstanceState == null) {
                loadFragment(DashboardFragment.newInstance())
            }
            
            Log.d(TAG, "onCreate completed")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate", e)
            e.printStackTrace()
            showToast("Critical error: ${e.message}")
        }
    }

    private fun setupDrawerMenu() {
        try {
            Log.d(TAG, "Setting up drawer menu")
            
            // Set up the menu button click listener
            menuButton.setOnClickListener {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }
            
            // Set up click listeners for drawer menu items
            dashboardMenuItem.setOnClickListener {
                showToast("Dashboard selected")
                loadFragment(DashboardFragment.newInstance())
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            
            profileMenuItem.setOnClickListener {
                showToast("Profile selected")
<<<<<<< HEAD
                // Navigate to ProfileActivity
                startActivity(Intent(this, ProfileActivity::class.java))
=======
                // TODO: Load profile fragment when created
>>>>>>> dfb36664f5bd8ecf892bd63e6b9a33f2eefe4dac
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            
            settingsMenuItem.setOnClickListener {
                showToast("Settings selected")
                // TODO: Load settings fragment when created
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            
            progressionMenuItem.setOnClickListener {
                showToast("Progression selected")
                loadFragment(ProgressionFragment.newInstance())
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            
            habitsMenuItem.setOnClickListener {
                showToast("Habits selected")
                loadFragment(HabitsFragment.newInstance())
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            
            debtsMenuItem.setOnClickListener {
                showToast("Debts selected")
                loadFragment(DebtFragment.newInstance())
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            
            Log.d(TAG, "Drawer menu setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up drawer menu", e)
            e.printStackTrace()
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        try {
            Log.d(TAG, "Loading fragment: ${fragment.javaClass.simpleName}")
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment", e)
            showToast("Error: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    // Handle back button press to close drawer if open
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
} 