package com.u1.slicer.shared.native

import android.content.Context
import com.u1.slicer.shared.platform.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation using JNI to call into libprusaslicer-jni.so
 */
actual class SapilWrapper actual constructor() {

    private var nativeHandle: Long = 0
    private var context: Context? = null

    /**
     * Initialize with Android Context
     * Must be called before using the wrapper
     */
    fun initialize(context: Context) {
        this.context = context
        System.loadLibrary("prusaslicer-jni")
        nativeHandle = nativeCreate()
        getLogger().d("SapilWrapper", "Native SAPIL engine created")
    }

    private fun requireContext(): Context {
        return context ?: throw IllegalStateException("SapilWrapper not initialized. Call initialize(context) first.")
    }

    init {
        // Note: Context must be set via initialize() before use
    }

    actual fun getCoreVersion(): String {
        return nativeGetCoreVersion(nativeHandle)
    }

    actual fun loadModel(path: String): Boolean {
        return nativeLoadModel(nativeHandle, path)
    }

    actual fun getModelInfo(): ModelInfo? {
        return nativeGetModelInfo(nativeHandle)
    }

    actual fun getPreparePreviewMesh(): PreparePreviewMesh? {
        return nativeGetPreparePreviewMesh(nativeHandle)
    }

    actual fun clearModel() {
        nativeClearModel(nativeHandle)
    }

    actual suspend fun slice(config: SliceConfig, progress: ((Int, String) -> Unit)?): SliceResult? =
        withContext(Dispatchers.IO) {
            nativeSlice(
                nativeHandle,
                config.layerHeight,
                config.firstLayerHeight,
                config.perimeters,
                config.topSolidLayers,
                config.bottomSolidLayers,
                config.fillDensity,
                config.fillPattern,
                config.printSpeed,
                config.travelSpeed,
                config.firstLayerSpeed,
                config.nozzleTemp,
                config.bedTemp,
                config.retractLength,
                config.retractSpeed,
                config.supportEnabled,
                config.supportType,
                config.supportAngle,
                config.skirtLoops,
                config.skirtDistance,
                config.brimWidth,
                config.bedSizeX,
                config.bedSizeY,
                config.maxPrintHeight,
                config.nozzleDiameter,
                config.filamentDiameter,
                config.filamentType,
                config.extruderCount,
                config.extruderTemps.toIntArray(),
                config.extruderRetractLength.toFloatArray(),
                config.extruderRetractSpeed.toFloatArray(),
                config.wipeTowerEnabled,
                config.wipeTowerX,
                config.wipeTowerY,
                config.wipeTowerWidth,
                null, // Progress callback (TODO: implement)
                requireContext().cacheDir?.absolutePath ?: "/data/local/tmp"
            )
        }

    actual fun loadProfile(iniPath: String): Boolean {
        return nativeLoadProfile(nativeHandle, iniPath)
    }

    actual fun getConfigFromProfile(): SliceConfig? {
        return nativeGetConfigFromProfile(nativeHandle)
    }

    actual fun getGcodePreview(maxLines: Int): String {
        return nativeGetGcodePreview(nativeHandle, maxLines)
    }

    actual fun setModelInstances(positions: List<Pair<Float, Float>>): Boolean {
        val flatPositions = positions.flatMap { listOf(it.first, it.second) }.toFloatArray()
        return nativeSetModelInstances(nativeHandle, flatPositions)
    }

    actual fun setModelScale(x: Float, y: Float, z: Float): Boolean {
        return nativeSetModelScale(nativeHandle, x, y, z)
    }

    actual fun dispose() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }

    // =========================================================================
    // Native methods (JNI) - implemented in C++
    // =========================================================================

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeGetCoreVersion(handle: Long): String
    private external fun nativeLoadModel(handle: Long, path: String): Boolean
    private external fun nativeGetModelInfo(handle: Long): ModelInfo?
    private external fun nativeGetPreparePreviewMesh(handle: Long): PreparePreviewMesh?
    private external fun nativeClearModel(handle: Long)
    private external fun nativeSlice(
        handle: Long,
        layerHeight: Float,
        firstLayerHeight: Float,
        perimeters: Int,
        topSolidLayers: Int,
        bottomSolidLayers: Int,
        fillDensity: Float,
        fillPattern: String,
        printSpeed: Float,
        travelSpeed: Float,
        firstLayerSpeed: Float,
        nozzleTemp: Int,
        bedTemp: Int,
        retractLength: Float,
        retractSpeed: Float,
        supportEnabled: Boolean,
        supportType: String,
        supportAngle: Float,
        skirtLoops: Int,
        skirtDistance: Float,
        brimWidth: Float,
        bedSizeX: Float,
        bedSizeY: Float,
        maxPrintHeight: Float,
        nozzleDiameter: Float,
        filamentDiameter: Float,
        filamentType: String,
        extruderCount: Int,
        extruderTemps: IntArray,
        extruderRetractLength: FloatArray,
        extruderRetractSpeed: FloatArray,
        wipeTowerEnabled: Boolean,
        wipeTowerX: Float,
        wipeTowerY: Float,
        wipeTowerWidth: Float,
        progressCallback: Any?,
        cacheDir: String
    ): SliceResult?

    private external fun nativeLoadProfile(handle: Long, iniPath: String): Boolean
    private external fun nativeGetConfigFromProfile(handle: Long): SliceConfig?
    private external fun nativeGetGcodePreview(handle: Long, maxLines: Int): String
    private external fun nativeSetModelInstances(handle: Long, positions: FloatArray): Boolean
    private external fun nativeSetModelScale(handle: Long, x: Float, y: Float, z: Float): Boolean

    protected fun finalize() {
        dispose()
    }
}
