package com.u1.slicer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [resolveStatusBadge] — the badge text shown on the Printer screen.
 *
 * Key scenario: after a print is sent ("Print started!" banner visible),
 * Moonraker may still report the previous print's state (e.g. "cancelled")
 * until the PRINT_START macro finishes.  The badge should show "STARTING…"
 * instead of the stale state.
 */
class PrinterStatusBadgeTest {

    // ── Normal (no pending send) ───────────────────────────────────────

    @Test
    fun `printing state shows PRINTING`() {
        assertEquals("PRINTING", resolveStatusBadge("printing", justSent = false))
    }

    @Test
    fun `paused state shows PAUSED`() {
        assertEquals("PAUSED", resolveStatusBadge("paused", justSent = false))
    }

    @Test
    fun `complete state shows COMPLETE`() {
        assertEquals("COMPLETE", resolveStatusBadge("complete", justSent = false))
    }

    @Test
    fun `error state shows ERROR`() {
        assertEquals("ERROR", resolveStatusBadge("error", justSent = false))
    }

    @Test
    fun `standby state shows STANDBY`() {
        assertEquals("STANDBY", resolveStatusBadge("standby", justSent = false))
    }

    @Test
    fun `cancelled state shows CANCELLED`() {
        assertEquals("CANCELLED", resolveStatusBadge("cancelled", justSent = false))
    }

    @Test
    fun `disconnected state shows DISCONNECTED`() {
        assertEquals("DISCONNECTED", resolveStatusBadge("disconnected", justSent = false))
    }

    // ── Just-sent: stale states overridden to STARTING ─────────────────

    @Test
    fun `justSent with cancelled shows STARTING`() {
        assertEquals("STARTING\u2026", resolveStatusBadge("cancelled", justSent = true))
    }

    @Test
    fun `justSent with standby shows STARTING`() {
        assertEquals("STARTING\u2026", resolveStatusBadge("standby", justSent = true))
    }

    @Test
    fun `justSent with complete shows STARTING`() {
        assertEquals("STARTING\u2026", resolveStatusBadge("complete", justSent = true))
    }

    @Test
    fun `justSent with error shows STARTING`() {
        assertEquals("STARTING\u2026", resolveStatusBadge("error", justSent = true))
    }

    // ── Just-sent: non-stale states are NOT overridden ─────────────────

    @Test
    fun `justSent with printing still shows PRINTING`() {
        assertEquals("PRINTING", resolveStatusBadge("printing", justSent = true))
    }

    @Test
    fun `justSent with paused still shows PAUSED`() {
        assertEquals("PAUSED", resolveStatusBadge("paused", justSent = true))
    }

    @Test
    fun `justSent with disconnected shows DISCONNECTED not STARTING`() {
        // "disconnected" is not in the stale-state list — if the printer
        // genuinely lost connection, we should show that, not mask it.
        assertEquals("DISCONNECTED", resolveStatusBadge("disconnected", justSent = true))
    }
}
