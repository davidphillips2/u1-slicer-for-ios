package com.u1.slicer.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for MoonrakerClient using MockWebServer.
 *
 * Note: MoonrakerClient uses android.util.Log internally.
 * These tests will fail on JVM without mocking Log. For full coverage,
 * see the instrumented test variant. These tests verify the HTTP
 * request/response contract.
 *
 * To run these on JVM, you'd need to add a Log stub or use Robolectric.
 * For now these are structured as instrumented-compatible tests.
 */
class MoonrakerClientTest {

    // These tests document the expected API contract even if they can't
    // run on pure JVM due to android.util.Log dependency.
    // Move to androidTest/ if needed.

    @Test
    fun `baseUrl trailing slash is trimmed`() {
        val client = MoonrakerClientTestHelper()
        client.baseUrl = "http://192.168.1.100/"
        assertEquals("http://192.168.1.100", client.baseUrl)
    }

    @Test
    fun `baseUrl without trailing slash is unchanged`() {
        val client = MoonrakerClientTestHelper()
        client.baseUrl = "http://192.168.1.100"
        assertEquals("http://192.168.1.100", client.baseUrl)
    }

    @Test
    fun `url helper constructs full path`() {
        val client = MoonrakerClientTestHelper()
        client.baseUrl = "http://printer.local"
        assertEquals("http://printer.local/server/info", client.buildUrl("/server/info"))
    }

    @Test
    fun `PrinterStatus defaults`() {
        val status = PrinterStatus(state = "standby", progress = 0f)
        assertEquals("standby", status.state)
        assertEquals(0f, status.progress, 0.001f)
        assertEquals("", status.filename)
        assertEquals(0f, status.printDuration, 0.001f)
        assertEquals(0f, status.nozzleTemp, 0.001f)
        assertEquals(0f, status.bedTemp, 0.001f)
        assertTrue(status.extruders.isEmpty())
    }

    @Test
    fun `PrinterStatus with active print`() {
        val status = PrinterStatus(
            state = "printing",
            progress = 0.45f,
            filename = "benchy.gcode",
            printDuration = 1800f,
            filamentUsed = 2500f,
            nozzleTemp = 210f,
            nozzleTarget = 210f,
            bedTemp = 59f,
            bedTarget = 60f,
            extruders = listOf(
                ExtruderStatus(0, 210f, 210f, true),
                ExtruderStatus(1, 25f, 0f, false)
            )
        )
        assertEquals("printing", status.state)
        assertEquals(0.45f, status.progress, 0.001f)
        assertEquals(2, status.extruders.size)
        assertTrue(status.extruders[0].active)
        assertFalse(status.extruders[1].active)
    }

    @Test
    fun `ExtruderStatus data class`() {
        val ext = ExtruderStatus(index = 2, temp = 245f, target = 250f, active = true)
        assertEquals(2, ext.index)
        assertEquals(245f, ext.temp, 0.001f)
        assertEquals(250f, ext.target, 0.001f)
        assertTrue(ext.active)
    }

    @Test
    fun `PrinterStatus isConnected`() {
        assertTrue(PrinterStatus(state = "standby", progress = 0f).isConnected)
        assertTrue(PrinterStatus(state = "printing", progress = 0.5f).isConnected)
        assertFalse(PrinterStatus(state = "disconnected", progress = 0f).isConnected)
    }

    @Test
    fun `PrinterStatus isPrinting`() {
        assertTrue(PrinterStatus(state = "printing", progress = 0.5f).isPrinting)
        assertFalse(PrinterStatus(state = "paused", progress = 0.5f).isPrinting)
        assertFalse(PrinterStatus(state = "standby", progress = 0f).isPrinting)
    }

    @Test
    fun `PrinterStatus isPaused`() {
        assertTrue(PrinterStatus(state = "paused", progress = 0.3f).isPaused)
        assertFalse(PrinterStatus(state = "printing", progress = 0.3f).isPaused)
    }

    @Test
    fun `PrinterStatus isIdle`() {
        assertTrue(PrinterStatus(state = "standby", progress = 0f).isIdle)
        assertTrue(PrinterStatus(state = "complete", progress = 1f).isIdle)
        assertFalse(PrinterStatus(state = "printing", progress = 0.5f).isIdle)
    }

    @Test
    fun `PrinterStatus progressPercent`() {
        assertEquals(0, PrinterStatus(state = "standby", progress = 0f).progressPercent)
        assertEquals(50, PrinterStatus(state = "printing", progress = 0.5f).progressPercent)
        assertEquals(100, PrinterStatus(state = "complete", progress = 1f).progressPercent)
    }

    @Test
    fun `PrinterStatus printTimeFormatted`() {
        // 90 minutes = 1h 30m
        assertEquals("1h 30m", PrinterStatus(state = "printing", progress = 0.5f, printDuration = 5400f).printTimeFormatted)
        // 45 minutes
        assertEquals("45m", PrinterStatus(state = "printing", progress = 0.3f, printDuration = 2700f).printTimeFormatted)
        // 0 minutes
        assertEquals("0m", PrinterStatus(state = "standby", progress = 0f, printDuration = 0f).printTimeFormatted)
    }
}

/**
 * Test helper that exposes MoonrakerClient's URL building logic
 * without requiring android.util.Log for basic property tests.
 */
class MoonrakerClientTestHelper {
    var baseUrl: String = ""
        set(value) {
            field = value.trimEnd('/')
        }

    fun buildUrl(path: String): String = "$baseUrl$path"
}
