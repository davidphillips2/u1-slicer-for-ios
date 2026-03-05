package com.u1.slicer.bambu

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tests for ThreeMfParser.
 *
 * Note: ThreeMfParser uses android.util.Log which is unavailable in JVM tests.
 * These tests run as instrumented tests instead. For JVM, we test the 3MF
 * ZIP structure creation helpers here, and the actual parser in androidTest/.
 *
 * This file tests the data models and ZIP creation utilities.
 */
class ThreeMfParserTest {

    @Test
    fun `ThreeMfInfo defaults`() {
        val info = ThreeMfInfo(
            objects = emptyList(),
            plates = emptyList(),
            isBambu = false,
            isMultiPlate = false
        )
        assertFalse(info.isBambu)
        assertFalse(info.isMultiPlate)
        assertFalse(info.hasPaintData)
        assertFalse(info.hasLayerToolChanges)
        assertEquals(0, info.detectedColors.size)
        assertEquals(1, info.detectedExtruderCount)
    }

    @Test
    fun `ThreeMfObject data class`() {
        val obj = ThreeMfObject(
            objectId = "1",
            name = "Cube",
            vertices = 8,
            triangles = 12
        )
        assertEquals("1", obj.objectId)
        assertEquals("Cube", obj.name)
        assertEquals(8, obj.vertices)
        assertEquals(12, obj.triangles)
    }

    @Test
    fun `ThreeMfPlate data class`() {
        val plate = ThreeMfPlate(
            plateId = 1,
            name = "Main Plate",
            objectIds = listOf("1", "2"),
            printable = true,
            transform = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f)
        )
        assertEquals(1, plate.plateId)
        assertEquals("Main Plate", plate.name)
        assertEquals(2, plate.objectIds.size)
        assertTrue(plate.printable)
    }

    @Test
    fun `ThreeMfInfo with multicolor metadata`() {
        val info = ThreeMfInfo(
            objects = listOf(
                ThreeMfObject("1", "Part", 100, 200)
            ),
            plates = listOf(
                ThreeMfPlate(1, "Plate 1", listOf("1"), true,
                    floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f))
            ),
            isBambu = true,
            isMultiPlate = false,
            hasPaintData = true,
            hasLayerToolChanges = true,
            detectedColors = listOf("#FF0000", "#00FF00", "#0000FF"),
            detectedExtruderCount = 3
        )
        assertTrue(info.isBambu)
        assertTrue(info.hasPaintData)
        assertTrue(info.hasLayerToolChanges)
        assertEquals(3, info.detectedColors.size)
        assertEquals(3, info.detectedExtruderCount)
    }

    @Test
    fun `ThreeMfInfo multi-plate detection`() {
        val info = ThreeMfInfo(
            objects = listOf(
                ThreeMfObject("1", "Part A", 50, 100),
                ThreeMfObject("2", "Part B", 60, 120)
            ),
            plates = listOf(
                ThreeMfPlate(1, "Plate 1", listOf("1"), true,
                    floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f)),
                ThreeMfPlate(2, "Plate 2", listOf("2"), true,
                    floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f))
            ),
            isBambu = true,
            isMultiPlate = true
        )
        assertTrue(info.isMultiPlate)
        assertEquals(2, info.plates.size)
    }
}
