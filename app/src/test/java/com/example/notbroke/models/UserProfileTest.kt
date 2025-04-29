package com.example.notbroke.models

import org.junit.Test
import org.junit.Assert.*

class UserProfileTest {
    
    @Test
    fun `test UserProfile creation with valid data`() {
        val userId = "123"
        val username = "testUser"
        val email = "test@example.com"
        
        val userProfile = UserProfile(userId, username, email)

        assertEquals(userId, userProfile.userId)
        assertEquals(username, userProfile.username)
        assertEquals(email, userProfile.email)
    }
    
    @Test
    fun `test UserProfile equality`() {
        val userProfile1 = UserProfile("123", "testUser", "test@example.com")
        val userProfile2 = UserProfile("123", "testUser", "test@example.com")
        val userProfile3 = UserProfile("456", "otherUser", "other@example.com")
        
        assertEquals(userProfile1, userProfile2)
        assertNotEquals(userProfile1, userProfile3)
    }
    
    @Test
    fun `test UserProfile toString`() {
        val userProfile = UserProfile("123", "testUser", "test@example.com")
        
        val toString = userProfile.toString()
        
        assertTrue(toString.contains("123"))
        assertTrue(toString.contains("testUser"))
        assertTrue(toString.contains("test@example.com"))
    }
} 