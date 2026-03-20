package com.u1.slicer.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterRepositoryTest {

    @Test
    fun buildPrinterUploadFilename_appendsUniqueSuffix() {
        val name = PrinterRepository.buildPrinterUploadFilename("output.gcode", nowMillis = 1234567890L)

        assertEquals("output_1234567890.gcode", name)
    }

    @Test
    fun buildPrinterUploadFilename_sanitizesAndFallsBack() {
        val name = PrinterRepository.buildPrinterUploadFilename("My Slip/Slide #1.gcode", nowMillis = 99L)
        val fallback = PrinterRepository.buildPrinterUploadFilename("   ", nowMillis = 99L)

        assertTrue(name.startsWith("My_Slip_Slide_1_"))
        assertTrue(name.endsWith("_99.gcode"))
        assertEquals("print_99.gcode", fallback)
    }
}
