package com.u1.slicer.data

import org.junit.Assert.*
import org.junit.Test

class DataClassesTest {

    // --- FilamentProfile ---

    @Test
    fun `FilamentProfile default values`() {
        val profile = FilamentProfile(
            name = "Test PLA",
            material = "PLA",
            nozzleTemp = 210,
            bedTemp = 60,
            printSpeed = 60f,
            retractLength = 0.8f,
            retractSpeed = 45f
        )
        assertEquals(0L, profile.id) // autoGenerate default
        assertEquals("#808080", profile.color)
        assertEquals(1.24f, profile.density, 0.001f)
        assertFalse(profile.isDefault)
    }

    @Test
    fun `FilamentProfile copy with custom color`() {
        val profile = FilamentProfile(
            name = "Red PLA",
            material = "PLA",
            nozzleTemp = 210,
            bedTemp = 60,
            printSpeed = 60f,
            retractLength = 0.8f,
            retractSpeed = 45f,
            color = "#FF0000"
        )
        assertEquals("#FF0000", profile.color)
    }

    @Test
    fun `FilamentProfile equality`() {
        val a = FilamentProfile(id = 1, name = "PLA", material = "PLA", nozzleTemp = 210, bedTemp = 60, printSpeed = 60f, retractLength = 0.8f, retractSpeed = 45f)
        val b = FilamentProfile(id = 1, name = "PLA", material = "PLA", nozzleTemp = 210, bedTemp = 60, printSpeed = 60f, retractLength = 0.8f, retractSpeed = 45f)
        assertEquals(a, b)
    }

    // --- SliceJob ---

    @Test
    fun `SliceJob creation`() {
        val job = SliceJob(
            modelName = "benchy.stl",
            gcodePath = "/data/output.gcode",
            totalLayers = 150,
            estimatedTimeSeconds = 3600f,
            estimatedFilamentMm = 5000f,
            layerHeight = 0.2f,
            fillDensity = 0.15f,
            nozzleTemp = 210,
            bedTemp = 60,
            supportEnabled = false,
            filamentType = "PLA"
        )
        assertEquals("benchy.stl", job.modelName)
        assertEquals(150, job.totalLayers)
        assertTrue(job.timestamp > 0)
    }

    @Test
    fun `SliceJob timestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val job = SliceJob(
            modelName = "test.stl",
            gcodePath = "/out.gcode",
            totalLayers = 10,
            estimatedTimeSeconds = 100f,
            estimatedFilamentMm = 500f,
            layerHeight = 0.2f,
            fillDensity = 0.2f,
            nozzleTemp = 200,
            bedTemp = 55,
            supportEnabled = true,
            filamentType = "PETG"
        )
        val after = System.currentTimeMillis()
        assertTrue(job.timestamp in before..after)
    }

    // --- GcodeLayer data classes ---

    @Test
    fun `GcodeMove data class`() {
        val move = com.u1.slicer.gcode.GcodeMove(
            type = com.u1.slicer.gcode.MoveType.EXTRUDE,
            x0 = 0f, y0 = 0f,
            x1 = 10f, y1 = 20f,
            extruder = 1
        )
        assertEquals(10f, move.x1, 0.001f)
        assertEquals(1, move.extruder)
    }

    @Test
    fun `GcodeMove default extruder is 0`() {
        val move = com.u1.slicer.gcode.GcodeMove(
            type = com.u1.slicer.gcode.MoveType.TRAVEL,
            x0 = 0f, y0 = 0f, x1 = 5f, y1 = 5f
        )
        assertEquals(0, move.extruder)
    }

    @Test
    fun `ParsedGcode default bed dimensions`() {
        val gcode = com.u1.slicer.gcode.ParsedGcode(layers = emptyList())
        assertEquals(270f, gcode.bedWidth, 0.001f)
        assertEquals(270f, gcode.bedHeight, 0.001f)
    }

    // --- ModelInfo ---

    @Test
    fun `ModelInfo creation`() {
        val info = ModelInfo(
            filename = "cube.stl",
            format = "STL",
            sizeX = 20f, sizeY = 20f, sizeZ = 20f,
            triangleCount = 12,
            volumeCount = 1,
            isManifold = true
        )
        assertEquals("cube.stl", info.filename)
        assertEquals(12, info.triangleCount)
        assertTrue(info.isManifold)
    }
}
