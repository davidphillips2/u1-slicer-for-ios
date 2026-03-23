package com.u1.slicer.shared.data

import kotlinx.serialization.Serializable

/**
 * Mirrors sapil::ModelInfo in C++.
 * Contains metadata about a loaded 3D model.
 */
@Serializable
data class ModelInfo(
    val filename: String,
    val format: String,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float,
    val triangleCount: Int,
    val volumeCount: Int,
    val isManifold: Boolean
) {
    val dimensionString: String
        get() = "%.1f × %.1f × %.1f mm".format(sizeX, sizeY, sizeZ)
}
