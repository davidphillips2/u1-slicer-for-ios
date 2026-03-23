package com.u1.slicer.shared.data

import com.u1.slicer.shared.platform.PlatformUtils
import kotlinx.serialization.Serializable

/**
 * Slice job record.
 * Platform-agnostic version - Room-specific annotations added in Android implementation.
 */
@Serializable
data class SliceJob(
    val id: Long = 0,
    val modelName: String,
    val gcodePath: String,
    val totalLayers: Int,
    val estimatedTimeSeconds: Float,
    val estimatedFilamentMm: Float,
    val layerHeight: Float,
    val fillDensity: Float,
    val nozzleTemp: Int,
    val bedTemp: Int,
    val supportEnabled: Boolean,
    val filamentType: String,
    val timestamp: Long = PlatformUtils.currentTimeMillis()
)
