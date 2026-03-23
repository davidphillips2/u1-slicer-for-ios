package com.u1.slicer.shared.data

import kotlinx.serialization.Serializable

/**
 * Mirrors sapil::SliceConfig in C++.
 * Maps to PrusaSlicer's DynamicPrintConfig.
 */
@Serializable
data class SliceConfig(
    // Print settings
    var layerHeight: Float = 0.2f,
    var firstLayerHeight: Float = 0.3f,
    var perimeters: Int = 2,
    var topSolidLayers: Int = 5,
    var bottomSolidLayers: Int = 4,
    var fillDensity: Float = 0.15f,
    var fillPattern: String = "gyroid",

    // Speed (mm/s) — Snapmaker U1 defaults matching standard_0.20mm.json process profile.
    // printSpeed sets outer_wall_speed (inner wall, infill, travel use profile values).
    var printSpeed: Float = 200f,
    var travelSpeed: Float = 500f,
    var firstLayerSpeed: Float = 50f,

    // Temperature
    var nozzleTemp: Int = 210,
    var bedTemp: Int = 60,

    // Retraction
    var retractLength: Float = 0.8f,
    var retractSpeed: Float = 45f,

    // Support
    var supportEnabled: Boolean = false,
    var supportType: String = "normal",
    var supportAngle: Float = 45f,

    // Skirt/Brim
    var skirtLoops: Int = 0,
    var skirtDistance: Float = 6f,
    var brimWidth: Float = 0f,

    // Printer bed (Snapmaker U1: 270x270x270mm)
    var bedSizeX: Float = 270f,
    var bedSizeY: Float = 270f,
    var maxPrintHeight: Float = 270f,

    // Nozzle
    var nozzleDiameter: Float = 0.4f,

    // Filament
    var filamentDiameter: Float = 1.75f,
    var filamentType: String = "PLA",

    // Multi-extruder (up to 4 for Snapmaker U1)
    var extruderCount: Int = 1,
    var extruderTemps: List<Int> = emptyList(),
    var extruderRetractLength: List<Float> = emptyList(),
    var extruderRetractSpeed: List<Float> = emptyList(),

    // Wipe tower (for multi-extruder)
    var wipeTowerEnabled: Boolean = false,
    var wipeTowerX: Float = 170f,
    var wipeTowerY: Float = 140f,
    var wipeTowerWidth: Float = 60f
)
