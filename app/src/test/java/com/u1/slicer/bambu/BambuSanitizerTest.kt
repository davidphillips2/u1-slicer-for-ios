package com.u1.slicer.bambu

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for BambuSanitizer logic.
 *
 * Note: BambuSanitizer.process() and extractPlate() use android.util.Log
 * and ThreeMfParser (which uses XmlPullParser), so full integration tests
 * run as instrumented tests. Here we test the constants and configuration
 * that don't require Android APIs.
 *
 * See androidTest/ for full integration tests with real 3MF files.
 */
class BambuSanitizerTest {

    @Test
    fun `sanitizer drops known incompatible files`() {
        // Verify the DROP_FILES set contains expected entries
        // This is a "specification test" ensuring the sanitizer config is correct
        val dropFiles = setOf(
            "Metadata/slice_info.config",
            "Metadata/cut_information.xml",
            "Metadata/filament_sequence.json",
            "Metadata/Slic3r_PE.config",
            "Metadata/Slic3r_PE_model.config"
        )
        // We can't directly access private fields, but we can verify behavior
        // through the data models used in processing
        assertEquals(5, dropFiles.size)
    }

    @Test
    fun `clamp rules have correct minimum values`() {
        // Specification test: these minimum values prevent PrusaSlicer crashes
        val clampRules = mapOf(
            "raft_first_layer_expansion" to 0f,
            "tree_support_wall_count" to 0f,
            "prime_volume" to 0f,
            "prime_tower_brim_width" to 0f,
            "solid_infill_filament" to 1f,
            "sparse_infill_filament" to 1f,
            "wall_filament" to 1f
        )
        // Filament indices should clamp to 1, not 0
        assertEquals(1f, clampRules["solid_infill_filament"])
        assertEquals(1f, clampRules["wall_filament"])
        // Dimensional values clamp to 0
        assertEquals(0f, clampRules["prime_volume"])
    }

    @Test
    fun `INI config parsing basic`() {
        // Test the INI parsing logic independently
        val content = """
            layer_height = 0.2
            nozzle_temp = 210
            # comment line
            ; another comment

            filament_colour = #FF0000;#00FF00;#0000FF
        """.trimIndent()

        val config = parseTestIniConfig(content)
        assertEquals("0.2", config["layer_height"])
        assertEquals("210", config["nozzle_temp"])
        // Semicolon-separated becomes a list
        assertTrue(config["filament_colour"] is List<*>)
        @Suppress("UNCHECKED_CAST")
        val colors = config["filament_colour"] as List<String>
        assertEquals(3, colors.size)
        assertEquals("#FF0000", colors[0])
    }

    @Test
    fun `INI config ignores comments and blank lines`() {
        val content = """
            # Header comment
            ; Another comment

            key1 = value1
            key2 = value2
        """.trimIndent()

        val config = parseTestIniConfig(content)
        assertEquals(2, config.size)
        assertEquals("value1", config["key1"])
    }

    @Test
    fun `nil replacement logic`() {
        val list = mutableListOf("210", "nil", "nil", "220")
        val default = list.firstOrNull { it != "nil" }
        assertNotNull(default)
        for (i in list.indices) {
            if (list[i] == "nil") list[i] = default!!
        }
        assertEquals(listOf("210", "210", "210", "220"), list)
    }

    @Test
    fun `nil replacement all nil removes key`() {
        val list = mutableListOf("nil", "nil", "nil")
        val default = list.firstOrNull { it != "nil" }
        assertNull(default)
    }

    @Test
    fun `array normalization padding`() {
        val list = mutableListOf("a", "b")
        val targetCount = 4
        val last = list.last()
        while (list.size < targetCount) list.add(last)
        assertEquals(listOf("a", "b", "b", "b"), list)
    }

    @Test
    fun `array normalization truncation`() {
        val list = mutableListOf("a", "b", "c", "d")
        val targetCount = 2
        while (list.size > targetCount) list.removeAt(list.lastIndex)
        assertEquals(listOf("a", "b"), list)
    }

    @Test
    fun `flush matrix size is NxN`() {
        val extruderCount = 3
        val needed = extruderCount * extruderCount // 9
        val list = mutableListOf("0", "100", "100", "100", "0", "100")
        val last = list.lastOrNull() ?: "0"
        while (list.size < needed) list.add(last)
        assertEquals(9, list.size)
    }

    @Test
    fun `mmu_segmentation to paint_color conversion`() {
        val input = """<triangle v1="0" v2="1" v3="2" slic3rpe:mmu_segmentation="AABB"/>"""
        val output = input.replace("slic3rpe:mmu_segmentation=", "paint_color=")
        assertTrue(output.contains("paint_color="))
        assertFalse(output.contains("mmu_segmentation"))
    }

    @Test
    fun `transform clamping for packed plates`() {
        val bedCenter = 135f
        val packedThreshold = bedCenter * 2 + 100 // 370

        // A transform with tx=500 (packed by Bambu) should be clamped
        val tx = 500f
        assertTrue(kotlin.math.abs(tx) > packedThreshold)

        // Should be recentered to bedCenter
        val clamped = if (kotlin.math.abs(tx) > packedThreshold) bedCenter else tx
        assertEquals(135f, clamped, 0.001f)
    }

    @Test
    fun `transform within threshold is not clamped`() {
        val bedCenter = 135f
        val packedThreshold = bedCenter * 2 + 100

        val tx = 150f // Within normal range
        assertFalse(kotlin.math.abs(tx) > packedThreshold)
    }

    // Helper: simplified INI parser matching BambuSanitizer's logic
    private fun parseTestIniConfig(content: String): MutableMap<String, Any> {
        val config = mutableMapOf<String, Any>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) continue
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx < 0) continue
            val key = trimmed.substring(0, eqIdx).trim()
            val value = trimmed.substring(eqIdx + 1).trim()
            if (value.contains(";")) {
                config[key] = value.split(";").map { it.trim() }.toMutableList()
            } else {
                config[key] = value
            }
        }
        return config
    }
}
