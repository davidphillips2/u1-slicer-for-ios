package com.u1.slicer.shared.graphics

import com.u1.slicer.shared.platform.getLogger
import com.u1.slicer.shared.viewer.MeshData
import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.NSMakeRange
import platform.Foundation.create
import platform.Metal.*
import platform.MetalKit.MTKView
import platform.QuartzCore.CAMetalLayer
import kotlin.experimental.ExperimentalNativeApi

/**
 * iOS implementation using Metal
 *
 * This is a stub implementation that provides the interface structure.
 * The actual Metal rendering will be implemented in Swift with Kotlin/Native interop.
 *
 * Architecture:
 * 1. Swift class: MetalMeshRenderer (implements MTKViewDelegate)
 * 2. Kotlin/Native cinterop: Bridges Swift/Kotlin
 * 3. This class: Wraps the Metal renderer for cross-platform API
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual class MeshRenderer actual constructor() {

    private var metalRenderer: MetalMeshRendererWrapper? = null
    private val logger = getLogger()

    actual fun initialize() {
        metalRenderer = MetalMeshRendererWrapper()
        logger.d("MeshRenderer", "Metal renderer initialized")
    }

    actual fun setMesh(mesh: MeshData) {
        metalRenderer?.setMesh(
            vertices = mesh.vertices,
            vertexCount = mesh.vertexCount,
            extruderIndices = mesh.extruderIndices
        )
    }

    actual fun clearMesh() {
        metalRenderer?.clearMesh()
    }

    actual fun setInstanceColors(colors: List<FloatArray>) {
        val flatColors = colors.flatMap { it.asList() }.toFloatArray()
        metalRenderer?.setInstanceColors(flatColors, colors.size)
    }

    actual fun setInstancePositions(positions: FloatArray) {
        metalRenderer?.setInstancePositions(positions, positions.size / 2)
    }

    actual fun setModelScale(x: Float, y: Float, z: Float) {
        metalRenderer?.setModelScale(x, y, z)
    }

    actual fun setHighlightIndex(index: Int) {
        metalRenderer?.setHighlightIndex(index)
    }

    actual fun setWipeTower(x: Float, y: Float, width: Float, depth: Float) {
        metalRenderer?.setWipeTower(x, y, width, depth)
    }

    actual fun clearWipeTower() {
        metalRenderer?.clearWipeTower()
    }

    actual fun setCameraDistance(distance: Float) {
        metalRenderer?.setCameraDistance(distance)
    }

    actual fun setCameraRotation(yaw: Float, pitch: Float) {
        metalRenderer?.setCameraRotation(yaw, pitch)
    }

    actual fun resetCamera() {
        metalRenderer?.resetCamera()
    }

    actual fun render(width: Int, height: Int) {
        metalRenderer?.render(width, height)
    }

    actual fun handleDrag(dx: Float, dy: Float) {
        val sensitivity = 0.5f
        metalRenderer?.handleDrag(dx * sensitivity, dy * sensitivity)
    }

    actual fun handleZoom(scale: Float) {
        metalRenderer?.handleZoom(scale)
    }

    actual fun hitTest(x: Float, y: Float, width: Int, height: Int): Int {
        return metalRenderer?.hitTest(x, y, width, height) ?: -1
    }

    actual fun dispose() {
        metalRenderer?.dispose()
        metalRenderer = null
    }
}

/**
 * C wrapper for Swift MetalMeshRenderer
 *
 * This provides the C interface that Kotlin/Native can call into.
 * The actual implementation is in Swift (see ios/U1Slicer/Platform/MetalMeshRenderer.swift)
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private class MetalMeshRendererWrapper {
    private var rendererPtr: Long = 0

    init {
        rendererPtr = metal_renderer_create()
    }

    fun setMesh(vertices: FloatArray, vertexCount: Int, extruderIndices: ByteArray?) {
        memScoped {
            val verticesPtr = allocArray<FloatVar>(vertices.size)
            for (i in vertices.indices) {
                verticesPtr[i] = vertices[i]
            }

            val indicesPtr: CPointer<ByteVar>? = if (extruderIndices != null) {
                val ptr = allocArray<ByteVar>(extruderIndices.size)
                for (i in extruderIndices.indices) {
                    ptr[i] = extruderIndices[i]
                }
                ptr
            } else {
                null
            }

            metal_renderer_set_mesh(
                rendererPtr,
                verticesPtr,
                vertexCount,
                indicesPtr,
                extruderIndices?.size ?: 0
            )
        }
    }

    fun clearMesh() {
        metal_renderer_clear_mesh(rendererPtr)
    }

    fun setInstanceColors(colors: FloatArray, count: Int) {
        metal_renderer_set_instance_colors(
            rendererPtr,
            colors,
            count
        )
    }

    fun setInstancePositions(positions: FloatArray, count: Int) {
        metal_renderer_set_instance_positions(
            rendererPtr,
            positions,
            count
        )
    }

    fun setModelScale(x: Float, y: Float, z: Float) {
        metal_renderer_set_model_scale(rendererPtr, x, y, z)
    }

    fun setHighlightIndex(index: Int) {
        metal_renderer_set_highlight_index(rendererPtr, index)
    }

    fun setWipeTower(x: Float, y: Float, width: Float, depth: Float) {
        metal_renderer_set_wipe_tower(rendererPtr, x, y, width, depth)
    }

    fun clearWipeTower() {
        metal_renderer_clear_wipe_tower(rendererPtr)
    }

    fun setCameraDistance(distance: Float) {
        metal_renderer_set_camera_distance(rendererPtr, distance)
    }

    fun setCameraRotation(yaw: Float, pitch: Float) {
        metal_renderer_set_camera_rotation(rendererPtr, yaw, pitch)
    }

    fun resetCamera() {
        metal_renderer_reset_camera(rendererPtr)
    }

    fun render(width: Int, height: Int) {
        metal_renderer_render(rendererPtr, width, height)
    }

    fun handleDrag(dx: Float, dy: Float) {
        metal_renderer_handle_drag(rendererPtr, dx, dy)
    }

    fun handleZoom(scale: Float) {
        metal_renderer_handle_zoom(rendererPtr, scale)
    }

    fun hitTest(x: Float, y: Float, width: Int, height: Int): Int {
        return metal_renderer_hit_test(rendererPtr, x, y, width, height)
    }

    fun dispose() {
        metal_renderer_destroy(rendererPtr)
        rendererPtr = 0
    }
}

// ============================================================================
// C Function Declarations (to be implemented in Swift)
// ============================================================================

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_create")
private external fun metal_renderer_create(): Long

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_destroy")
private external fun metal_renderer_destroy(renderer: Long)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("metal_renderer_set_mesh")
private external fun metal_renderer_set_mesh(
    renderer: Long,
    vertices: CPointer<FloatVar>,
    vertexCount: Int,
    extruderIndices: CPointer<ByteVar>?,
    extruderCount: Int
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_clear_mesh")
private external fun metal_renderer_clear_mesh(renderer: Long)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_set_instance_colors")
private external fun metal_renderer_set_instance_colors(
    renderer: Long,
    colors: FloatArray,
    count: Int
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_set_instance_positions")
private external fun metal_renderer_set_instance_positions(
    renderer: Long,
    positions: FloatArray,
    count: Int
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_set_model_scale")
private external fun metal_renderer_set_model_scale(
    renderer: Long,
    x: Float,
    y: Float,
    z: Float
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_set_highlight_index")
private external fun metal_renderer_set_highlight_index(
    renderer: Long,
    index: Int
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_set_wipe_tower")
private external fun metal_renderer_set_wipe_tower(
    renderer: Long,
    x: Float,
    y: Float,
    width: Float,
    depth: Float
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_clear_wipe_tower")
private external fun metal_renderer_clear_wipe_tower(renderer: Long)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_set_camera_distance")
private external fun metal_renderer_set_camera_distance(
    renderer: Long,
    distance: Float
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_set_camera_rotation")
private external fun metal_renderer_set_camera_rotation(
    renderer: Long,
    yaw: Float,
    pitch: Float
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_reset_camera")
private external fun metal_renderer_reset_camera(renderer: Long)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_render")
private external fun metal_renderer_render(
    renderer: Long,
    width: Int,
    height: Int
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_handle_drag")
private external fun metal_renderer_handle_drag(
    renderer: Long,
    dx: Float,
    dy: Float
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_handle_zoom")
private external fun metal_renderer_handle_zoom(
    renderer: Long,
    scale: Float
)

@OptIn(ExperimentalNativeApi::class)
@CName("metal_renderer_hit_test")
private external fun metal_renderer_hit_test(
    renderer: Long,
    x: Float,
    y: Float,
    width: Int,
    height: Int
): Int

