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
    fun `copy preserves all fields`() {
        val config = SliceConfig(
            layerHeight = 0.1f,
            nozzleTemp = 250,
            bedTemp = 100,
            supportEnabled = true,
            fillDensity = 0.5f,
            fillPattern = "cubic"
        )
        val copy = config.copy()
        assertEquals(config, copy)
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
    fun `data class equality works`() {
        val a = SliceConfig(layerHeight = 0.1f)
        val b = SliceConfig(layerHeight = 0.1f)
        assertEquals(a, b)
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
}
