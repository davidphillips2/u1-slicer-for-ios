package com.u1.slicer.printer

import com.u1.slicer.network.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintProgressNotifierTest {

    @Test
    fun `shouldShow only for active print states`() {
        assertTrue(PrintProgressNotifier.shouldShow(PrinterStatus(state = "printing", progress = 0.42f)))
        assertTrue(PrintProgressNotifier.shouldShow(PrinterStatus(state = "paused", progress = 0.42f)))
        assertFalse(PrintProgressNotifier.shouldShow(PrinterStatus(state = "standby", progress = 0.42f)))
        assertFalse(PrintProgressNotifier.shouldShow(PrinterStatus(state = "disconnected", progress = 0f)))
    }

    @Test
    fun `titleFor shows progress for active print`() {
        val title = PrintProgressNotifier.titleFor(
            PrinterStatus(state = "printing", progress = 0.42f, filename = "cube.gcode")
        )

        assertEquals("Printing 42%", title)
    }

    @Test
    fun `textFor includes paused progress and filename`() {
        val text = PrintProgressNotifier.textFor(
            PrinterStatus(state = "paused", progress = 0.58f, filename = "benchy.gcode")
        )

        assertEquals("benchy.gcode is paused at 58%", text)
    }
}
