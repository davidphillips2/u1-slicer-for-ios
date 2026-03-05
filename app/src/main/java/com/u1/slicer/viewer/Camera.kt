package com.u1.slicer.viewer

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

/**
 * Orbit camera for 3D model viewing.
 * Orbits around a target point with azimuth/elevation/distance.
 */
class Camera {
    var azimuth = -45f       // horizontal rotation (degrees)
    var elevation = 30f      // vertical rotation (degrees)
    var distance = 300f      // distance from target
    var panX = 0f            // pan offset X
    var panY = 0f            // pan offset Y
    var targetX = 0f
    var targetY = 0f
    var targetZ = 0f

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val mvpMatrix = FloatArray(16)
    val normalMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    fun setTarget(x: Float, y: Float, z: Float) {
        targetX = x; targetY = y; targetZ = z
    }

    fun rotate(dAzimuth: Float, dElevation: Float) {
        azimuth += dAzimuth
        elevation = (elevation + dElevation).coerceIn(-89f, 89f)
    }

    fun zoom(factor: Float) {
        distance = (distance * factor).coerceIn(10f, 2000f)
    }

    fun pan(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    fun updateViewMatrix() {
        val radAz = Math.toRadians(azimuth.toDouble())
        val radEl = Math.toRadians(elevation.toDouble())

        val eyeX = targetX + panX + (distance * cos(radEl) * sin(radAz)).toFloat()
        val eyeY = targetY + panY + (distance * sin(radEl)).toFloat()
        val eyeZ = targetZ + (distance * cos(radEl) * cos(radAz)).toFloat()

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            targetX + panX, targetY + panY, targetZ,
            0f, 1f, 0f
        )
    }

    fun updateProjectionMatrix(width: Int, height: Int) {
        val aspect = width.toFloat() / height.toFloat()
        val near = distance * 0.01f
        val far = distance * 10f
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, near, far)
    }

    fun computeMVP(modelMatrix: FloatArray = IDENTITY) {
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        // Normal matrix = transpose(inverse(modelView))
        Matrix.invertM(normalMatrix, 0, tempMatrix, 0)
        transposeInPlace(normalMatrix)
    }

    private fun transposeInPlace(m: FloatArray) {
        fun swap(i: Int, j: Int) { val t = m[i]; m[i] = m[j]; m[j] = t }
        swap(1, 4); swap(2, 8); swap(3, 12)
        swap(6, 9); swap(7, 13); swap(11, 14)
    }

    companion object {
        val IDENTITY = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    }
}
