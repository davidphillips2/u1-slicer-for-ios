package com.u1.slicer.network

import org.junit.Assert.*
import org.junit.Test

class MakerWorldClientTest {

    @Test
    fun `parseDesignId from full URL with language prefix`() {
        val id = MakerWorldClient.parseDesignId("https://makerworld.com/en/models/12345")
        assertEquals("12345", id)
    }

    @Test
    fun `parseDesignId from URL without language`() {
        val id = MakerWorldClient.parseDesignId("https://makerworld.com/models/67890")
        assertEquals("67890", id)
    }

    @Test
    fun `parseDesignId from URL with www`() {
        val id = MakerWorldClient.parseDesignId("https://www.makerworld.com/en/models/11111")
        assertEquals("11111", id)
    }

    @Test
    fun `parseDesignId from URL without https`() {
        val id = MakerWorldClient.parseDesignId("makerworld.com/en/models/99999")
        assertEquals("99999", id)
    }

    @Test
    fun `parseDesignId from URL with http`() {
        val id = MakerWorldClient.parseDesignId("http://makerworld.com/en/models/55555")
        assertEquals("55555", id)
    }

    @Test
    fun `parseDesignId from raw numeric ID`() {
        val id = MakerWorldClient.parseDesignId("12345")
        assertEquals("12345", id)
    }

    @Test
    fun `parseDesignId from numeric ID with whitespace`() {
        val id = MakerWorldClient.parseDesignId("  12345  ")
        assertEquals("12345", id)
    }

    @Test
    fun `parseDesignId returns null for invalid input`() {
        assertNull(MakerWorldClient.parseDesignId("not a url"))
        assertNull(MakerWorldClient.parseDesignId(""))
        assertNull(MakerWorldClient.parseDesignId("https://thingiverse.com/thing:12345"))
        assertNull(MakerWorldClient.parseDesignId("abc123"))
    }

    @Test
    fun `parseDesignId from different language prefixes`() {
        assertEquals("100", MakerWorldClient.parseDesignId("https://makerworld.com/de/models/100"))
        assertEquals("200", MakerWorldClient.parseDesignId("https://makerworld.com/ja/models/200"))
        assertEquals("300", MakerWorldClient.parseDesignId("https://makerworld.com/zh/models/300"))
    }

    @Test
    fun `isValidInput returns true for valid URLs`() {
        assertTrue(MakerWorldClient.isValidInput("https://makerworld.com/en/models/12345"))
        assertTrue(MakerWorldClient.isValidInput("12345"))
    }

    @Test
    fun `isValidInput returns false for invalid input`() {
        assertFalse(MakerWorldClient.isValidInput("not valid"))
        assertFalse(MakerWorldClient.isValidInput(""))
        assertFalse(MakerWorldClient.isValidInput("abc"))
    }

    @Test
    fun `parseDesignId handles large IDs`() {
        val id = MakerWorldClient.parseDesignId("https://makerworld.com/en/models/9999999999")
        assertEquals("9999999999", id)
    }
}
