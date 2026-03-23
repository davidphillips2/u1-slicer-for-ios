package com.u1.slicer.shared.data

import com.u1.slicer.shared.platform.PlatformUtils
import kotlinx.serialization.Serializable

/**
 * Mirrors sapil::SliceResult in C++.
 * Contains slicing outcome data.
 */
@Serializable
data class SliceResult(
    val success: Boolean,
    val errorMessage: String,
    val gcodePath: String,
    val totalLayers: Int,
    val estimatedTimeSeconds: Float,
    val estimatedFilamentMm: Float,
    val estimatedFilamentGrams: Float
) {
    val estimatedTimeFormatted: String
        get() {
            val minutes = (estimatedTimeSeconds / 60).toInt()
            val hours = minutes / 60
            val mins = minutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }

    val estimatedFilamentFormatted: String
        get() = "${PlatformUtils.formatFloat("%.1f", estimatedFilamentGrams)} g (${PlatformUtils.formatFloat("%.0f", estimatedFilamentMm)} mm)"
}
