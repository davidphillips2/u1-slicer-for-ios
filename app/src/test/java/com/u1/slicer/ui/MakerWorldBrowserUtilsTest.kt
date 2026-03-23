package com.u1.slicer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MakerWorldBrowserUtilsTest {

    // ---- sanitizeFilename ----

    @Test
    fun `sanitizeFilename passes through clean names`() {
        assertEquals("model.3mf", sanitizeFilename("model.3mf"))
        assertEquals("my model.3mf", sanitizeFilename("my model.3mf"))
    }

    @Test
    fun `sanitizeFilename strips path separators`() {
        assertEquals("_.._secret.3mf", sanitizeFilename("/../secret.3mf"))
        assertEquals("_.._.._etc_passwd", sanitizeFilename("/../../etc/passwd"))
    }

    @Test
    fun `sanitizeFilename strips special characters`() {
        assertEquals("model_v2_.3mf", sanitizeFilename("model_v2>.3mf"))
        assertEquals("test_model_.3mf", sanitizeFilename("test|model:.3mf"))
    }

    @Test
    fun `sanitizeFilename handles backslashes`() {
        assertEquals("_.._windows_path.3mf", sanitizeFilename("\\..\\windows\\path.3mf"))
    }

    // ---- hasAuthCookies ----

    @Test
    fun `hasAuthCookies returns false for short cloudflare-only cookies`() {
        assertFalse(hasAuthCookies("__cf_bm=abc123"))
        assertFalse(hasAuthCookies(""))
    }

    @Test
    fun `hasAuthCookies returns true when token is present`() {
        assertTrue(hasAuthCookies("__cf_bm=abc123; token=jwt.eyJ..."))
    }

    @Test
    fun `hasAuthCookies returns true when sessionid is present`() {
        assertTrue(hasAuthCookies("__cf_bm=abc123; sessionid=abc456"))
    }

    @Test
    fun `hasAuthCookies returns true for long cookie strings`() {
        assertTrue(hasAuthCookies("a".repeat(501)))
    }

    @Test
    fun `hasAuthCookies returns false at boundary length`() {
        assertFalse(hasAuthCookies("a".repeat(500)))
    }

    @Test
    fun `hasAuthCookies returns false for session substring without auth`() {
        // "session" alone (not "sessionid") should not trigger — test current behavior
        // Current implementation uses contains("sessionid"), not contains("session")
        assertFalse(hasAuthCookies("__cf_bm=abc; _ga_session=123"))
    }
}
