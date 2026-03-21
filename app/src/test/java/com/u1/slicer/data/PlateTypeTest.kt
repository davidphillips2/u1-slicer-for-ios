package com.u1.slicer.data

import org.junit.Assert.*
import org.junit.Test

class PlateTypeTest {

    // ── bedTempFor ─────────────────────────────────────────────────────────────

    @Test
    fun `Textured PEI PLA is 65`() = assertEquals(65, PlateType.TEXTURED_PEI.bedTempFor("PLA"))

    @Test
    fun `Textured PEI PETG is 70`() = assertEquals(70, PlateType.TEXTURED_PEI.bedTempFor("PETG"))

    @Test
    fun `Textured PEI ABS is 100`() = assertEquals(100, PlateType.TEXTURED_PEI.bedTempFor("ABS"))

    @Test
    fun `Textured PEI ASA is 100`() = assertEquals(100, PlateType.TEXTURED_PEI.bedTempFor("ASA"))

    @Test
    fun `Textured PEI TPU is 40`() = assertEquals(40, PlateType.TEXTURED_PEI.bedTempFor("TPU"))

    @Test
    fun `Smooth PEI PLA is 55`() = assertEquals(55, PlateType.SMOOTH_PEI.bedTempFor("PLA"))

    @Test
    fun `Smooth PEI PETG is 70`() = assertEquals(70, PlateType.SMOOTH_PEI.bedTempFor("PETG"))

    @Test
    fun `Smooth PEI ABS is 100`() = assertEquals(100, PlateType.SMOOTH_PEI.bedTempFor("ABS"))

    @Test
    fun `Cool Plate PLA is 35`() = assertEquals(35, PlateType.COOL_PLATE.bedTempFor("PLA"))

    @Test
    fun `Cool Plate TPU is 35`() = assertEquals(35, PlateType.COOL_PLATE.bedTempFor("TPU"))

    @Test
    fun `Cool Plate PETG uses fallback temp`() = assertEquals(65, PlateType.COOL_PLATE.bedTempFor("PETG"))

    @Test
    fun `Engineering Plate PLA is 45`() = assertEquals(45, PlateType.ENGINEERING_PLATE.bedTempFor("PLA"))

    @Test
    fun `Engineering Plate ABS is 110`() = assertEquals(110, PlateType.ENGINEERING_PLATE.bedTempFor("ABS"))

    @Test
    fun `Engineering Plate PC is 120`() = assertEquals(120, PlateType.ENGINEERING_PLATE.bedTempFor("PC"))

    @Test
    fun `Engineering Plate PETG is 75`() = assertEquals(75, PlateType.ENGINEERING_PLATE.bedTempFor("PETG"))

    @Test
    fun `bedTempFor is case-insensitive`() {
        assertEquals(PlateType.TEXTURED_PEI.bedTempFor("PLA"), PlateType.TEXTURED_PEI.bedTempFor("pla"))
        assertEquals(PlateType.TEXTURED_PEI.bedTempFor("PETG"), PlateType.TEXTURED_PEI.bedTempFor("petg"))
        assertEquals(PlateType.TEXTURED_PEI.bedTempFor("ABS"), PlateType.TEXTURED_PEI.bedTempFor("abs"))
    }

    // ── fromName ───────────────────────────────────────────────────────────────

    @Test
    fun `fromName returns correct type for valid name`() {
        assertEquals(PlateType.TEXTURED_PEI, PlateType.fromName("TEXTURED_PEI"))
        assertEquals(PlateType.SMOOTH_PEI, PlateType.fromName("SMOOTH_PEI"))
        assertEquals(PlateType.COOL_PLATE, PlateType.fromName("COOL_PLATE"))
        assertEquals(PlateType.ENGINEERING_PLATE, PlateType.fromName("ENGINEERING_PLATE"))
    }

    @Test
    fun `fromName returns DEFAULT for null`() = assertEquals(PlateType.DEFAULT, PlateType.fromName(null))

    @Test
    fun `fromName returns DEFAULT for unknown name`() = assertEquals(PlateType.DEFAULT, PlateType.fromName("UNKNOWN"))

    @Test
    fun `DEFAULT is TEXTURED_PEI`() = assertEquals(PlateType.TEXTURED_PEI, PlateType.DEFAULT)

    // ── all plates have sensible temps for PLA ─────────────────────────────────

    @Test
    fun `all plate types return positive bed temp for PLA`() {
        PlateType.entries.forEach { plate ->
            val temp = plate.bedTempFor("PLA")
            assertTrue("${plate.name} PLA temp $temp should be > 0", temp > 0)
        }
    }
}
