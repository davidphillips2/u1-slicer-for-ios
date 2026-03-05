package com.u1.slicer.ui

import org.junit.Assert.*
import org.junit.Test

class ExtruderAssignmentTest {

    @Test
    fun `default temperature is 210`() {
        val a = ExtruderAssignment(extruderIndex = 0, color = "#FF0000")
        assertEquals(210, a.temperature)
    }

    @Test
    fun `default filament name is empty`() {
        val a = ExtruderAssignment(extruderIndex = 0, color = "#FF0000")
        assertEquals("", a.filamentName)
    }

    @Test
    fun `can set custom temperature`() {
        val a = ExtruderAssignment(extruderIndex = 1, color = "#00FF00", temperature = 240)
        assertEquals(240, a.temperature)
    }

    @Test
    fun `equality works on same values`() {
        val a = ExtruderAssignment(0, "#FF0000", 210, "PLA White")
        val b = ExtruderAssignment(0, "#FF0000", 210, "PLA White")
        assertEquals(a, b)
    }

    @Test
    fun `copy with different temperature`() {
        val a = ExtruderAssignment(0, "#FF0000", 210)
        val b = a.copy(temperature = 230)
        assertEquals(230, b.temperature)
        assertEquals(a.color, b.color)
        assertEquals(a.extruderIndex, b.extruderIndex)
    }

    @Test
    fun `four extruder list with different colors`() {
        val colors = listOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00")
        val assignments = colors.mapIndexed { i, c ->
            ExtruderAssignment(extruderIndex = i, color = c, temperature = 200 + i * 10)
        }
        assertEquals(4, assignments.size)
        assertEquals(210, assignments[1].temperature)
        assertEquals("#0000FF", assignments[2].color)
    }

    // parseHexColor uses android.graphics.Color (Android API) so it's tested on-device only;
    // covered by the Bambu/ThreeMfInfo color string format tests here at JVM level.
}
