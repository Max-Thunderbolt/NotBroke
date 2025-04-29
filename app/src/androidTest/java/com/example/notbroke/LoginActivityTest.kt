package com.example.notbroke

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Test
    fun testLoginActivityRedirectsToMainActivity() {
        // Launch the LoginActivity
        ActivityScenario.launch(LoginActivity::class.java)

        // Verify that we are redirected to MainActivity
        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }
} 