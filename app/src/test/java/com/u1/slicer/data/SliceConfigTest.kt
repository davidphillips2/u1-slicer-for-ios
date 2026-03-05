package com.u1.slicer.data

import org.junit.Assert.*
import org.junit.Test

class SliceConfigTest {

    @Test
    fun `default values match Snapmaker U1 specs`() {
        val config = SliceConfig()
        assertEquals(270f, config.bedSizeX, 0.001f)
        assertEquals(270f, config.bedSizeY, 0.001f)
        assertEquals(270f, config.maxPrintHeight, 0.001f)
    }

    @Test
    fun `default nozzle diameter is 0_4mm`() {
        val config = SliceConfig()
        assertEquals(0.4f, config.nozzleDiameter, 0.001f)
    }

    @Test
    fun `default filament diameter is 1_75mm`() {
        val config = SliceConfig()
        assertEquals(1.75f, config.filamentDiameter, 0.001f)
    }

    @Test
    fun `default layer height is 0_2mm`() {
        val config = SliceConfig()
        assertEquals(0.2f, config.layerHeight, 0.001f)
        assertEquals(0.3f, config.firstLayerHeight, 0.001f)
    }

    @Test
    fun `default temperatures are PLA-appropriate`() {
        val config = SliceConfig()
        assertEquals(210, config.nozzleTemp)
        assertEquals(60, config.bedTemp)
        assertEquals("PLA", config.filamentType)
    }

    @Test
    fun `default support is disabled`() {
        val config = SliceConfig()
        assertFalse(config.supportEnabled)
    }

    @Test
    fun `copy preserves scalar fields`() {
        val config = SliceConfig(
            layerHeight = 0.1f,
            nozzleTemp = 250,
            bedTemp = 100,
            supportEnabled = true,
            fillDensity = 0.5f,
            fillPattern = "cubic"
        )
        val copy = config.copy()
        // Arrays use reference equality in data class — compare scalars explicitly
        assertEquals(config.layerHeight, copy.layerHeight, 0.001f)
        assertEquals(config.nozzleTemp, copy.nozzleTemp)
        assertEquals(config.bedTemp, copy.bedTemp)
        assertEquals(config.supportEnabled, copy.supportEnabled)
        assertEquals(config.fillDensity, copy.fillDensity, 0.001f)
        assertEquals(config.fillPattern, copy.fillPattern)
    }

    @Test
    fun `copy with modification changes only specified field`() {
        val config = SliceConfig()
        val modified = config.copy(nozzleTemp = 250)
        assertEquals(250, modified.nozzleTemp)
        assertEquals(config.bedTemp, modified.bedTemp)
        assertEquals(config.layerHeight, modified.layerHeight, 0.001f)
    }

    @Test
    fun `data class copy preserves values`() {
        val a = SliceConfig(layerHeight = 0.1f)
        val b = a.copy()
        assertEquals(a.layerHeight, b.layerHeight)
        assertEquals(a.fillDensity, b.fillDensity)
        assertEquals(a.extruderCount, b.extruderCount)
    }

    @Test
    fun `data class inequality works`() {
        val a = SliceConfig(layerHeight = 0.1f)
        val b = SliceConfig(layerHeight = 0.2f)
        assertNotEquals(a, b)
    }

    @Test
    fun `default fill pattern is gyroid`() {
        val config = SliceConfig()
        assertEquals("gyroid", config.fillPattern)
    }

    @Test
    fun `default fill density is 15 percent`() {
        val config = SliceConfig()
        assertEquals(0.15f, config.fillDensity, 0.001f)
    }

    // --- Multi-extruder (Phase 3) ---

    @Test
    fun `default extruder count is 1`() {
        val config = SliceConfig()
        assertEquals(1, config.extruderCount)
    }

    @Test
    fun `default wipe tower is disabled`() {
        val config = SliceConfig()
        assertFalse(config.wipeTowerEnabled)
    }

    @Test
    fun `default wipe tower position is within 270x270 bed`() {
        val config = SliceConfig()
        assertTrue(config.wipeTowerX < config.bedSizeX)
        assertTrue(config.wipeTowerY < config.bedSizeY)
        assertTrue(config.wipeTowerWidth > 0f)
    }

    @Test
    fun `default extruder arrays are empty`() {
        val config = SliceConfig()
        assertEquals(0, config.extruderTemps.size)
        assertEquals(0, config.extruderRetractLength.size)
        assertEquals(0, config.extruderRetractSpeed.size)
    }

    @Test
    fun `can set extruder count to 4`() {
        val config = SliceConfig(extruderCount = 4)
        assertEquals(4, config.extruderCount)
    }

    @Test
    fun `can set per-extruder temperatures`() {
        val temps = intArrayOf(210, 220, 230, 240)
        val config = SliceConfig(extruderCount = 4, extruderTemps = temps)
        assertArrayEquals(temps, config.extruderTemps)
    }

    @Test
    fun `can set per-extruder retraction`() {
        val retract = floatArrayOf(0.8f, 0.6f, 0.8f, 0.8f)
        val config = SliceConfig(extruderCount = 4, extruderRetractLength = retract)
        assertEquals(0.6f, config.extruderRetractLength[1], 0.001f)
    }

    @Test
    fun `wipe tower can be enabled`() {
        val config = SliceConfig(wipeTowerEnabled = true, wipeTowerWidth = 80f)
        assertTrue(config.wipeTowerEnabled)
        assertEquals(80f, config.wipeTowerWidth, 0.001f)
    }

    @Test
    fun `copy preserves extruder count`() {
        val config = SliceConfig(extruderCount = 3, wipeTowerEnabled = true)
        val copy = config.copy()
        assertEquals(3, copy.extruderCount)
        assertTrue(copy.wipeTowerEnabled)
    }
}
