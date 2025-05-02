package com.example.notbroke.models

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class NetWorthEntryTest {
    @Test
    fun `test NetWorthEntry creation with valid data`() {
        val date = Date()
        val entry = NetWorthEntry(
            id = "entry1",
            userId = "user1",
            amount = 1234.56,
            date = date
        )
        assertEquals("entry1", entry.id)
        assertEquals("user1", entry.userId)
        assertEquals(1234.56, entry.amount, 0.001)
        assertEquals(date, entry.date)
    }

    @Test
    fun `test NetWorthEntry default values`() {
        val entry = NetWorthEntry()
        assertEquals("", entry.id)
        assertEquals("", entry.userId)
        assertEquals(0.0, entry.amount, 0.001)
        assertNotNull(entry.date)
    }

    @Test
    fun `test NetWorthEntry equality`() {
        val date = Date()
        val entry1 = NetWorthEntry("id1", "user1", "Entry1",100.0, date)
        val entry2 = NetWorthEntry("id1", "user1", "Entry1",100.0, date)
        val entry3 = NetWorthEntry("id2", "user2", "Entry2", 200.0, date)
        assertEquals(entry1, entry2)
        assertNotEquals(entry1, entry3)
    }

    @Test
    fun `test NetWorthEntry toString`() {
        val entry = NetWorthEntry("id1", "user1", "NetworthEntry", 100.0, Date(0))
        val str = entry.toString()
        assertTrue(str.contains("id1"))
        assertTrue(str.contains("user1"))
        assertTrue(str.contains("100.0"))
    }
} 