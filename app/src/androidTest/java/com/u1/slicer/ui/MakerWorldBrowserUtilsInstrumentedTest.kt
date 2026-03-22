package com.u1.slicer.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [resolveDownloadFilename] which depends on
 * [android.webkit.URLUtil.guessFileName] (Android framework API).
 */
@RunWith(AndroidJUnit4::class)
class MakerWorldBrowserUtilsInstrumentedTest {

    @Test
    fun resolveDownloadFilename_simpleContentDisposition() {
        val result = resolveDownloadFilename(
            url = "https://cdn.example.com/abc123.3mf",
            contentDisposition = "attachment;filename=model.3mf",
            mimeType = "binary/octet-stream"
        )
        assertEquals("model.3mf", result)
    }

    @Test
    fun resolveDownloadFilename_quotedFilename() {
        val result = resolveDownloadFilename(
            url = "https://cdn.example.com/abc123",
            contentDisposition = "attachment;filename=\"my model.3mf\"",
            mimeType = "binary/octet-stream"
        )
        assertEquals("my model.3mf", result)
    }

    @Test
    fun resolveDownloadFilename_filenameStar_RFC5987() {
        val result = resolveDownloadFilename(
            url = "https://cdn.example.com/abc123",
            contentDisposition = "attachment;filename=old.3mf;filename*=utf-8''super+clean.3mf",
            mimeType = "binary/octet-stream"
        )
        // Should extract "old.3mf" from the filename= part, not the raw disposition
        assertEquals("old.3mf", result)
    }

    @Test
    fun resolveDownloadFilename_noContentDisposition_fallsBackToUrl() {
        val result = resolveDownloadFilename(
            url = "https://cdn.example.com/model.3mf?token=abc",
            contentDisposition = null,
            mimeType = "binary/octet-stream"
        )
        assertEquals("model.3mf", result)
    }

    @Test
    fun resolveDownloadFilename_makerWorldRealUrl() {
        // Real URL pattern from MakerWorld CDN downloads
        val result = resolveDownloadFilename(
            url = "https://makerworld.bblmw.com/makerworld/model/USabc/123/instance/uuid.3mf?at=1&exp=2&key=abc",
            contentDisposition = "attachment;filename=old.3mf;filename*=utf-8''super+clean.3mf",
            mimeType = "binary/octet-stream"
        )
        assertEquals("old.3mf", result)
    }

    @Test
    fun resolveDownloadFilename_resultIsSafeAfterSanitize() {
        val raw = resolveDownloadFilename(
            url = "https://cdn.example.com/abc",
            contentDisposition = "attachment;filename=\"../../etc/passwd\"",
            mimeType = "binary/octet-stream"
        )
        val safe = sanitizeFilename(raw)
        assertTrue("Sanitized filename should not contain path separators", !safe.contains("/") && !safe.contains("\\"))
    }
}
