package com.u1.slicer.shared.graphics

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.u1.slicer.shared.platform.getLogger
import com.u1.slicer.shared.viewer.MeshData

/**
 * Android implementation using OpenGL ES 3.0
 *
 * This is a wrapper that provides the cross-platform MeshRenderer interface.
 * The actual rendering is handled by the existing ModelRenderer in the Android app.
 *
 * Note: This is a minimal wrapper for API compatibility.
 * The Android app should use the existing ModelRenderer directly for GLSurfaceView integration.
 */
actual class MeshRenderer actual constructor() {

    private var context: Context? = null
    private var oglDelegate: OpenGLMeshRenderer? = null

    /**
     * Initialize with Android context
     * Must be called before other methods
     */
    fun initialize(context: Context) {
        this.context = context
        this.oglDelegate = OpenGLMeshRenderer()
        getLogger().d("MeshRenderer", "OpenGL ES renderer initialized")
    }

    private fun requireDelegate(): OpenGLMeshRenderer {
        return oglDelegate ?: throw IllegalStateException("MeshRenderer not initialized. Call initialize(context) first.")
    }

    actual fun initialize() {
        // No-op for Android - initialization requires Context
    }

    actual fun setMesh(mesh: MeshData) {
        requireDelegate().setMesh(mesh)
    }

    actual fun clearMesh() {
        requireDelegate().clearMesh()
    }

    actual fun setInstanceColors(colors: List<FloatArray>) {
        requireDelegate().setInstanceColors(colors)
    }

    actual fun setInstancePositions(positions: FloatArray) {
        requireDelegate().setInstancePositions(positions)
    }

    actual fun setModelScale(x: Float, y: Float, z: Float) {
        requireDelegate().setModelScale(x, y, z)
    }

    actual fun setHighlightIndex(index: Int) {
        requireDelegate().setHighlightIndex(index)
    }

    actual fun setWipeTower(x: Float, y: Float, width: Float, depth: Float) {
        requireDelegate().setWipeTower(x, y, width, depth)
    }

    actual fun clearWipeTower() {
        requireDelegate().clearWipeTower()
    }

    actual fun setCameraDistance(distance: Float) {
        requireDelegate().setCameraDistance(distance)
    }

    actual fun setCameraRotation(yaw: Float, pitch: Float) {
        requireDelegate().setCameraRotation(yaw, pitch)
    }

    actual fun resetCamera() {
        requireDelegate().resetCamera()
    }

    actual fun render(width: Int, height: Int) {
        // Rendering is handled by GLSurfaceView.Renderer interface
        // This is a no-op in the wrapper
    }

    actual fun handleDrag(dx: Float, dy: Float) {
        requireDelegate().handleDrag(dx, dy)
    }

    actual fun handleZoom(scale: Float) {
        requireDelegate().handleZoom(scale)
    }

    actual fun hitTest(x: Float, y: Float, width: Int, height: Int): Int {
        return requireDelegate().hitTest(x, y, width, height)
    }

    actual fun dispose() {
        oglDelegate = null
        context = null
    }

    /**
     * Internal OpenGL ES renderer delegate
     * Stores state and provides camera controls
     */
    private class OpenGLMeshRenderer {
        // Camera state
        var distance: Float = 400f
        var yaw: Float = 45f
        var pitch: Float = 30f

        // Mesh state
        private var mesh: MeshData? = null
        private var instanceColors: List<FloatArray>? = null
        private var instancePositions: FloatArray? = null
        private var modelScale: FloatArray = floatArrayOf(1f, 1f, 1f)
        private var highlightIndex: Int = -1
        private var wipeTower: WipeTowerInfo? = null

        fun setMesh(mesh: MeshData) {
            this.mesh = mesh
        }

        fun clearMesh() {
            this.mesh = null
        }

        fun setInstanceColors(colors: List<FloatArray>) {
            this.instanceColors = colors
        }

        fun setInstancePositions(positions: FloatArray) {
            this.instancePositions = positions
        }

        fun setModelScale(x: Float, y: Float, z: Float) {
            this.modelScale = floatArrayOf(x, y, z)
        }

        fun setHighlightIndex(index: Int) {
            this.highlightIndex = index
        }

        fun setWipeTower(x: Float, y: Float, width: Float, depth: Float) {
            this.wipeTower = WipeTowerInfo(x, y, width, depth)
        }

        fun clearWipeTower() {
            this.wipeTower = null
        }

        fun setCameraDistance(distance: Float) {
            this.distance = distance
        }

        fun setCameraRotation(yaw: Float, pitch: Float) {
            this.yaw = yaw
            this.pitch = pitch
        }

        fun resetCamera() {
            this.distance = 400f
            this.yaw = 45f
            this.pitch = 30f
        }

        fun handleDrag(dx: Float, dy: Float) {
            val sensitivity = 0.5f
            this.yaw += dx * sensitivity
            this.pitch += dy * sensitivity
        }

        fun handleZoom(scale: Float) {
            this.distance *= scale
            this.distance = this.distance.coerceIn(50f, 1000f)
        }

        fun hitTest(x: Float, y: Float, width: Int, height: Int): Int {
            // TODO: Implement hit testing
            return -1
        }

        data class WipeTowerInfo(val x: Float, val y: Float, val width: Float, val depth: Float)
    }
}
