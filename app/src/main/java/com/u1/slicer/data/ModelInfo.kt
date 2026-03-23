package com.u1.slicer.data

/**
 * Mirrors sapil::ModelInfo in C++.
 * Contains metadata about a loaded 3D model.
 */
data class ModelInfo(
    @JvmField val filename: String,
    @JvmField val format: String,
    @JvmField val sizeX: Float,
    @JvmField val sizeY: Float,
    @JvmField val sizeZ: Float,
    @JvmField val triangleCount: Int,
    @JvmField val volumeCount: Int,
    @JvmField val isManifold: Boolean
) {
    val dimensionString: String
        get() = String.format("%.1f × %.1f × %.1f mm", sizeX, sizeY, sizeZ)
}
