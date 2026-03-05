package com.u1.slicer.viewer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Holds interleaved vertex data for OpenGL rendering.
 * Format per vertex: x, y, z, nx, ny, nz (6 floats = 24 bytes)
 */
data class MeshData(
    val vertices: FloatBuffer,  // Interleaved position + normal
    val vertexCount: Int,
    val minX: Float, val minY: Float, val minZ: Float,
    val maxX: Float, val maxY: Float, val maxZ: Float
) {
    val centerX get() = (minX + maxX) / 2
    val centerY get() = (minY + maxY) / 2
    val centerZ get() = (minZ + maxZ) / 2
    val sizeX get() = maxX - minX
    val sizeY get() = maxY - minY
    val sizeZ get() = maxZ - minZ
    val maxDimension get() = maxOf(sizeX, sizeY, sizeZ)

    companion object {
        const val FLOATS_PER_VERTEX = 6 // x,y,z, nx,ny,nz
        const val BYTES_PER_VERTEX = FLOATS_PER_VERTEX * 4

        fun allocateBuffer(triangleCount: Int): FloatBuffer {
            val floatCount = triangleCount * 3 * FLOATS_PER_VERTEX
            return ByteBuffer.allocateDirect(floatCount * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }
    }
}
