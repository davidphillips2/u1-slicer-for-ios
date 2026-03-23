package com.u1.slicer.shared.platform

/**
 * iOS-specific IOException for Kotlin/Native
 * Since kotlin.io.IOException is not available in Kotlin/Native
 */
class IOException(message: String) : Exception(message)
