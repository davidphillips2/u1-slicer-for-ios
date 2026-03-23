package com.u1.slicer.shared.data

import kotlinx.serialization.Serializable

/**
 * Filament profile data.
 * Platform-agnostic version - Room-specific annotations added in Android implementation.
 */
@Serializable
data class FilamentProfile(
    val id: Long = 0,
    val name: String,
    val material: String,        // PLA, PETG, ABS, TPU, ASA, PA, PVA
    val nozzleTemp: Int,
    val bedTemp: Int,
    val printSpeed: Float,
    val retractLength: Float,
    val retractSpeed: Float,
    val color: String = "#808080", // Hex color for UI display
    val density: Float = 1.24f,    // g/cm3 for weight estimation
    val isDefault: Boolean = false
)
