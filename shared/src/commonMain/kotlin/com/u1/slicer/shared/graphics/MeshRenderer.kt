package com.u1.slicer.shared.graphics

import com.u1.slicer.shared.viewer.MeshData

/**
 * Platform-agnostic interface for 3D mesh rendering
 * Provides abstraction over OpenGL ES (Android) and Metal (iOS)
 */
expect class MeshRenderer {
    /**
     * Constructor
     */
    constructor()

    /**
     * Initialize the renderer
     * Must be called before any other methods
     */
    fun initialize()

    /**
     * Set the mesh data to render
     * @param mesh Interleaved vertex data (position + normal + color)
     */
    fun setMesh(mesh: MeshData)

    /**
     * Clear the current mesh
     */
    fun clearMesh()

    /**
     * Set instance colors for multi-extruder rendering
     * @param colors List of RGBA colors (4 floats each) per instance
     */
    fun setInstanceColors(colors: List<FloatArray>)

    /**
     * Set instance positions for multi-part layout
     * @param positions Flat array of X,Y coordinates in bed space
     */
    fun setInstancePositions(positions: FloatArray)

    /**
     * Set model scale
     */
    fun setModelScale(x: Float, y: Float, z: Float)

    /**
     * Highlight a specific instance
     * @param index Instance index, or -1 to clear highlight
     */
    fun setHighlightIndex(index: Int)

    /**
     * Set wipe tower visualization
     * @param x X position in bed space
     * @param y Y position in bed space
     * @param width Width in mm
     * @param depth Depth in mm
     */
    fun setWipeTower(x: Float, y: Float, width: Float, depth: Float)

    /**
     * Clear wipe tower visualization
     */
    fun clearWipeTower()

    /**
     * Set camera distance
     */
    fun setCameraDistance(distance: Float)

    /**
     * Set camera rotation angles
     */
    fun setCameraRotation(yaw: Float, pitch: Float)

    /**
     * Reset camera to default position
     */
    fun resetCamera()

    /**
     * Render the scene
     * @param width Viewport width in pixels
     * @param height Viewport height in pixels
     */
    fun render(width: Int, height: Int)

    /**
     * Handle touch drag for camera rotation
     * @param dx Delta X in pixels
     * @param dy Delta Y in pixels
     */
    fun handleDrag(dx: Float, dy: Float)

    /**
     * Handle pinch/zoom
     * @param scale Scale factor
     */
    fun handleZoom(scale: Float)

    /**
     * Hit test for picking objects at screen coordinates
     * @param x X coordinate in pixels
     * @param y Y coordinate in pixels
     * @param width Viewport width
     * @param height Viewport height
     * @return Instance index that was hit, or -1 if none
     */
    fun hitTest(x: Float, y: Float, width: Int, height: Int): Int

    /**
     * Release resources
     */
    fun dispose()
}

/**
 * Camera configuration
 */
data class CameraConfig(
    val distance: Float = 400f,
    val yaw: Float = 45f,
    val pitch: Float = 30f
)

/**
 * Lighting configuration
 */
data class LightingConfig(
    val ambientIntensity: Float = 0.3f,
    val directionalIntensity: Float = 0.7f,
    val lightDirection: FloatArray = floatArrayOf(-0.5f, -1.0f, -0.5f)
)
