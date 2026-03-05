package com.u1.slicer.bambu

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Sanitizes Bambu Studio 3MF files for compatibility with PrusaSlicer.
 *
 * Ported from u1-slicer-bridge: profile_embedder.py
 *
 * Operations:
 * 1. Strip incompatible Bambu metadata files
 * 2. Clamp Bambu "-1" auto values to safe minimums
 * 3. Replace 'nil' strings with first non-nil value in arrays
 * 4. Convert PrusaSlicer mmu_segmentation → paint_color
 * 5. Recenter packed multi-plate transforms
 * 6. Clear stale plater_name values
 */
object BambuSanitizer {
    private const val TAG = "BambuSanitizer"

    // Files to drop entirely
    private val DROP_FILES = setOf(
        "Metadata/slice_info.config",
        "Metadata/cut_information.xml",
        "Metadata/filament_sequence.json",
        "Metadata/Slic3r_PE.config",
        "Metadata/Slic3r_PE_model.config"
    )

    // Preview image prefixes to drop
    private val DROP_PREFIXES = listOf(
        "Metadata/plate",
        "Metadata/top",
        "Metadata/pick"
    )

    // Parameters to clamp to minimum value
    private val CLAMP_RULES = mapOf(
        "raft_first_layer_expansion" to 0f,
        "tree_support_wall_count" to 0f,
        "prime_volume" to 0f,
        "prime_tower_brim_width" to 0f,
        "prime_tower_brim_chamfer" to 0f,
        "prime_tower_brim_chamfer_max_width" to 0f,
        "solid_infill_filament" to 1f,
        "sparse_infill_filament" to 1f,
        "wall_filament" to 1f
    )

    // Keys that are NOT per-filament arrays (excluded from normalization)
    private val NON_FILAMENT_KEYS = setOf(
        "compatible_printers",
        "compatible_prints"
    )

    // Keys with special list semantics (excluded from normalize, but padded)
    private val SPECIAL_LIST_KEYS = setOf(
        "flush_volumes_matrix",
        "flush_volumes_vector",
        "different_settings_to_system",
        "inherits_group",
        "upward_compatible_machine",
        "printable_area",
        "bed_exclude_area",
        "thumbnails",
        "head_wrap_detect_zone",
        "extruder_offset",
        "wipe_tower_x",
        "wipe_tower_y"
    )

    /**
     * Process a 3MF file: detect if Bambu, sanitize if needed, return the
     * (possibly modified) file path. If not Bambu, returns the original path unchanged.
     */
    fun process(inputFile: File, outputDir: File): File {
        val info = ThreeMfParser.parse(inputFile)
        if (!info.isBambu) {
            Log.i(TAG, "Not a Bambu file, no sanitization needed")
            return inputFile
        }

        Log.i(TAG, "Bambu file detected, sanitizing...")
        val outputFile = File(outputDir, "sanitized_${inputFile.name}")

        try {
            ZipFile(inputFile).use { srcZip ->
                ZipOutputStream(FileOutputStream(outputFile)).use { destZip ->
                    var projectSettingsWritten = false

                    for (entry in srcZip.entries()) {
                        val name = entry.name

                        // Drop incompatible files
                        if (name in DROP_FILES) {
                            Log.d(TAG, "Dropping: $name")
                            continue
                        }

                        // Drop preview images
                        if (DROP_PREFIXES.any { name.startsWith(it) }) {
                            continue
                        }

                        // Read the entry content
                        val content = srcZip.getInputStream(entry).readBytes()

                        when {
                            // Sanitize project_settings.config
                            name == "Metadata/project_settings.config" -> {
                                val sanitized = sanitizeProjectSettings(String(content))
                                destZip.putNextEntry(ZipEntry(name))
                                destZip.write(sanitized.toByteArray())
                                destZip.closeEntry()
                                projectSettingsWritten = true
                            }

                            // Sanitize model_settings.config
                            name == "Metadata/model_settings.config" -> {
                                val sanitized = sanitizeModelSettings(content)
                                destZip.putNextEntry(ZipEntry(name))
                                destZip.write(sanitized)
                                destZip.closeEntry()
                            }

                            // Convert mmu_segmentation in .model files
                            name.endsWith(".model") -> {
                                val converted = convertMmuSegmentation(content)
                                destZip.putNextEntry(ZipEntry(name))
                                destZip.write(converted)
                                destZip.closeEntry()
                            }

                            // Pass through everything else
                            else -> {
                                destZip.putNextEntry(ZipEntry(name))
                                destZip.write(content)
                                destZip.closeEntry()
                            }
                        }
                    }

                    // If no project_settings.config existed, that's fine
                    if (!projectSettingsWritten) {
                        Log.d(TAG, "No project_settings.config found in source")
                    }
                }
            }

            Log.i(TAG, "Sanitization complete: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Sanitization failed: ${e.message}")
            // Return original file as fallback
            outputFile.delete()
            return inputFile
        }
    }

    /**
     * Sanitize project_settings.config (INI-like key=value format).
     * - Clamp Bambu "-1" auto values
     * - Replace 'nil' strings
     * - Normalize per-filament arrays
     * - Strip Bambu-specific G-code macros
     */
    private fun sanitizeProjectSettings(content: String): String {
        val config = parseIniConfig(content)

        // 1. Clamp parameters
        for ((key, minVal) in CLAMP_RULES) {
            clampParameter(config, key, minVal)
        }

        // 2. Replace 'nil' values
        sanitizeNilValues(config)

        // 3. Normalize per-filament arrays
        val extruderCount = detectExtruderCount(config)
        if (extruderCount > 1) {
            normalizePerFilamentArrays(config, extruderCount)
        }

        // 4. Strip Bambu-specific G-code
        stripBambuGcode(config)

        // 5. Clamp wipe tower position
        sanitizeWipeTowerPosition(config)

        return serializeIniConfig(config)
    }

    /**
     * Sanitize model_settings.config XML:
     * - Clear stale plater_name values
     * - Clamp packed transforms (>370mm) to bed center
     */
    private fun sanitizeModelSettings(content: ByteArray): ByteArray {
        var text = String(content)

        // Clear plater_name to prevent OrcaSlicer/PrusaSlicer segfaults
        text = text.replace(
            Regex("""(key="plater_name"\s+value=")([^"]*)""""),
            "$1\""
        )

        // Clamp assemble_item transforms that are packed (Bambu multi-plate)
        val bedCenter = 135f // 270mm / 2
        val packedThreshold = bedCenter * 2 + 100 // 370mm
        text = text.replace(
            Regex("""(<assemble_item[^>]*\btransform=")([\d\s.eE+-]+)(")""")
        ) { match ->
            val prefix = match.groupValues[1]
            val transformStr = match.groupValues[2]
            val suffix = match.groupValues[3]
            val vals = transformStr.trim().split("\\s+".toRegex())
                .mapNotNull { it.toFloatOrNull() }
            if (vals.size >= 12) {
                val tx = vals[9]
                val ty = vals[10]
                if (kotlin.math.abs(tx) > packedThreshold || kotlin.math.abs(ty) > packedThreshold) {
                    val newVals = vals.toMutableList()
                    newVals[9] = bedCenter
                    newVals[10] = bedCenter
                    newVals[11] = 0f
                    prefix + newVals.joinToString(" ") { "%.6f".format(it) } + suffix
                } else {
                    match.value
                }
            } else {
                match.value
            }
        }

        return text.toByteArray()
    }

    /**
     * Convert PrusaSlicer mmu_segmentation attribute to paint_color
     * for OrcaSlicer/PrusaSlicer multi-color compatibility.
     */
    private fun convertMmuSegmentation(content: ByteArray): ByteArray {
        var text = String(content)
        if (!text.contains("mmu_segmentation")) return content

        text = text.replace("slic3rpe:mmu_segmentation=", "paint_color=")
        text = text.replace(Regex("""\s+xmlns:slic3rpe="[^"]*""""), "")

        return text.toByteArray()
    }

    // ---- INI Config Helpers ----

    /**
     * Parse a Bambu/OrcaSlicer project_settings.config into a map.
     * Values that look like semicolon-separated lists become List<String>.
     * Everything else is a String.
     */
    private fun parseIniConfig(content: String): MutableMap<String, Any> {
        val config = mutableMapOf<String, Any>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) continue
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx < 0) continue
            val key = trimmed.substring(0, eqIdx).trim()
            val value = trimmed.substring(eqIdx + 1).trim()

            // Detect list values (semicolon-separated in Bambu config format)
            if (value.contains(";")) {
                config[key] = value.split(";").map { it.trim() }.toMutableList()
            } else {
                config[key] = value
            }
        }
        return config
    }

    private fun serializeIniConfig(config: Map<String, Any>): String {
        val sb = StringBuilder()
        for ((key, value) in config) {
            when (value) {
                is List<*> -> sb.appendLine("$key = ${value.joinToString(";")}")
                else -> sb.appendLine("$key = $value")
            }
        }
        return sb.toString()
    }

    private fun clampParameter(config: MutableMap<String, Any>, key: String, minimum: Float) {
        val raw = config[key] ?: return
        val str = when (raw) {
            is List<*> -> (raw.firstOrNull() ?: return).toString()
            else -> raw.toString()
        }
        val numeric = str.trim().toFloatOrNull() ?: return
        if (numeric < minimum) {
            config[key] = if (minimum == minimum.toInt().toFloat()) {
                minimum.toInt().toString()
            } else {
                minimum.toString()
            }
        }
    }

    /**
     * Replace 'nil' strings in list values with the first non-nil element.
     * If all elements are 'nil', remove the key entirely.
     */
    private fun sanitizeNilValues(config: MutableMap<String, Any>) {
        val toRemove = mutableListOf<String>()
        for ((key, value) in config) {
            if (value !is MutableList<*>) continue
            @Suppress("UNCHECKED_CAST")
            val list = value as MutableList<String>
            if (!list.any { it == "nil" }) continue

            val default = list.firstOrNull { it != "nil" }
            if (default == null) {
                toRemove.add(key)
                continue
            }

            for (i in list.indices) {
                if (list[i] == "nil") list[i] = default
            }
        }
        toRemove.forEach { config.remove(it) }
    }

    /**
     * Normalize per-filament arrays to match target extruder count.
     * Pads short arrays by repeating last element. Truncates long arrays.
     */
    private fun normalizePerFilamentArrays(config: MutableMap<String, Any>, targetCount: Int) {
        for ((key, value) in config.entries.toList()) {
            if (key in NON_FILAMENT_KEYS || key in SPECIAL_LIST_KEYS) continue
            if (value !is MutableList<*>) continue
            @Suppress("UNCHECKED_CAST")
            val list = value as MutableList<Any>
            if (list.isEmpty()) continue

            when {
                list.size < targetCount -> {
                    val last = list.last()
                    while (list.size < targetCount) list.add(last)
                }
                list.size > targetCount -> {
                    while (list.size > targetCount) list.removeAt(list.lastIndex)
                }
            }
        }

        // Special: flush_volumes_matrix must be NxN
        normalizeFlushMatrix(config, targetCount)
    }

    private fun normalizeFlushMatrix(config: MutableMap<String, Any>, targetCount: Int) {
        val fvm = config["flush_volumes_matrix"]
        if (fvm is MutableList<*>) {
            @Suppress("UNCHECKED_CAST")
            val list = fvm as MutableList<Any>
            val needed = targetCount * targetCount
            val last = list.lastOrNull() ?: "0"
            while (list.size < needed) list.add(last)
            while (list.size > needed) list.removeAt(list.lastIndex)
        }
    }

    private fun detectExtruderCount(config: Map<String, Any>): Int {
        // Check filament_colour array size
        val colors = config["filament_colour"]
        if (colors is List<*> && colors.size > 1) return colors.size

        // Check extruder_colour
        val extColors = config["extruder_colour"]
        if (extColors is List<*> && extColors.size > 1) return extColors.size

        return 1
    }

    private fun stripBambuGcode(config: MutableMap<String, Any>) {
        // Remove Bambu-specific G-code keys
        config.remove("time_lapse_gcode")
        config.remove("machine_pause_gcode")

        // Strip filament_start_gcode if it contains Bambu macros
        val fsg = config["filament_start_gcode"]
        if (fsg is List<*>) {
            if (fsg.any { it.toString().contains("M142") || it.toString().contains("air_filtration") }) {
                config.remove("filament_start_gcode")
            }
        }
    }

    /**
     * Clamp wipe tower position to stay within 270mm bed.
     */
    private fun sanitizeWipeTowerPosition(
        config: MutableMap<String, Any>,
        bedSize: Float = 270f,
        extraMargin: Float = 6f
    ) {
        val towerWidth = getNumeric(config, "prime_tower_width", 35f)
        val towerBrim = maxOf(0f, getNumeric(config, "prime_tower_brim_width", 3f))
        val halfSpan = maxOf(12f, towerWidth / 2f + towerBrim + extraMargin)
        val minPos = halfSpan
        val maxPos = maxOf(minPos, bedSize - halfSpan)

        clampScalarPosition(config, "wipe_tower_x", minPos, maxPos)
        clampScalarPosition(config, "wipe_tower_y", minPos, maxPos)
    }

    private fun clampScalarPosition(config: MutableMap<String, Any>, key: String, min: Float, max: Float) {
        val raw = config[key] ?: return
        // Only clamp scalar values, not per-plate arrays
        if (raw is List<*>) return
        val value = raw.toString().toFloatOrNull() ?: return
        if (value < min || value > max) {
            config[key] = "%.3f".format(value.coerceIn(min, max))
        }
    }

    private fun getNumeric(config: Map<String, Any>, key: String, default: Float): Float {
        val raw = config[key] ?: return default
        return when (raw) {
            is List<*> -> raw.firstOrNull()?.toString()?.toFloatOrNull() ?: default
            else -> raw.toString().toFloatOrNull() ?: default
        }
    }

    /**
     * Extract a single plate from a multi-plate 3MF by marking other plates as non-printable.
     * This preserves all Bambu metadata links.
     */
    fun extractPlate(inputFile: File, targetPlateId: Int, outputDir: File): File {
        val outputFile = File(outputDir, "plate${targetPlateId}_${inputFile.name}")

        ZipFile(inputFile).use { srcZip ->
            ZipOutputStream(FileOutputStream(outputFile)).use { destZip ->
                for (entry in srcZip.entries()) {
                    val content = srcZip.getInputStream(entry).readBytes()

                    if (entry.name.endsWith("3dmodel.model")) {
                        // Modify build items: set printable flag
                        var text = String(content)
                        var itemIndex = 0
                        text = text.replace(Regex("""<item\b([^>]*)""")) { match ->
                            itemIndex++
                            val attrs = match.groupValues[1]
                            val newPrintable = if (itemIndex == targetPlateId) "1" else "0"
                            if (attrs.contains("printable=")) {
                                "<item" + attrs.replace(
                                    Regex("""printable="[^"]*""""),
                                    """printable="$newPrintable""""
                                )
                            } else {
                                """<item printable="$newPrintable"${attrs}"""
                            }
                        }
                        destZip.putNextEntry(ZipEntry(entry.name))
                        destZip.write(text.toByteArray())
                        destZip.closeEntry()
                    } else {
                        destZip.putNextEntry(ZipEntry(entry.name))
                        destZip.write(content)
                        destZip.closeEntry()
                    }
                }
            }
        }
        return outputFile
    }
}
