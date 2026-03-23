package com.u1.slicer.shared.data

import kotlinx.serialization.Serializable

/**
 * Build plate type selector for Snapmaker U1.
 * Each plate type has a recommended bed temperature that depends on the filament material.
 * Temperatures are sourced from Bambu/OrcaSlicer recommendations and Snapmaker U1 documentation.
 */
@Serializable
enum class PlateType(val label: String) {
    TEXTURED_PEI("Textured PEI"),
    SMOOTH_PEI("Smooth PEI"),
    COOL_PLATE("Cool Plate"),
    ENGINEERING_PLATE("Engineering Plate");

    /**
     * Returns the recommended bed temperature (°C) for this plate and material type.
     * [materialType] is the filament material string (e.g. "PLA", "PETG", "ABS", "TPU").
     */
    fun bedTempFor(materialType: String): Int {
        val mat = materialType.uppercase()
        return when (this) {
            TEXTURED_PEI -> when {
                mat.startsWith("ABS") || mat.startsWith("ASA") -> 100
                mat.startsWith("PETG") || mat.startsWith("PC") -> 70
                mat.startsWith("TPU") -> 40
                else -> 65  // PLA and similar
            }
            SMOOTH_PEI -> when {
                mat.startsWith("ABS") || mat.startsWith("ASA") -> 100
                mat.startsWith("PETG") || mat.startsWith("PC") -> 70
                mat.startsWith("TPU") -> 40
                else -> 55
            }
            COOL_PLATE -> when {
                mat.startsWith("PETG") || mat.startsWith("ABS") || mat.startsWith("ASA") || mat.startsWith("PC") -> 65
                mat.startsWith("TPU") -> 35
                else -> 35  // PLA
            }
            ENGINEERING_PLATE -> when {
                mat.startsWith("ABS") || mat.startsWith("ASA") -> 110
                mat.startsWith("PC") -> 120
                mat.startsWith("PETG") -> 75
                mat.startsWith("TPU") -> 40
                else -> 45
            }
        }
    }

    companion object {
        val DEFAULT = TEXTURED_PEI

        fun fromName(name: String?): PlateType =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
