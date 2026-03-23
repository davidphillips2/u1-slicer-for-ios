package com.u1.slicer.shared.native

/**
 * Platform-agnostic wrapper for SAPIL (Slicer API Layer)
 * Native C++ slicing engine wrapper - expect/actual for platform-specific implementations
 */
expect class SapilWrapper {
    /**
     * Constructor for Android: requires Context
     * Constructor for iOS: no parameters
     */
    constructor()

    /**
     * Get the version of the slicing engine core
     */
    fun getCoreVersion(): String

    /**
     * Load a 3D model from file path
     * @param path Absolute path to model file (STL, 3MF, OBJ, STEP)
     * @return true if successful
     */
    fun loadModel(path: String): Boolean

    /**
     * Get information about the loaded model
     */
    fun getModelInfo(): ModelInfo?

    /**
     * Get the prepare preview mesh for 3D visualization
     * Contains triangle positions and per-triangle extruder indices
     */
    fun getPreparePreviewMesh(): PreparePreviewMesh?

    /**
     * Clear the currently loaded model
     */
    fun clearModel()

    /**
     * Execute slicing with the given configuration
     * @param config Slicing parameters
     * @param progress Optional callback for progress updates (0-100)
     * @return Slice result with G-code path and statistics
     */
    suspend fun slice(config: SliceConfig, progress: ((Int, String) -> Unit)? = null): SliceResult?

    /**
     * Load slicing configuration from INI file
     */
    fun loadProfile(iniPath: String): Boolean

    /**
     * Get default configuration from loaded profile
     */
    fun getConfigFromProfile(): SliceConfig?

    /**
     * Get G-code preview (first N lines)
     */
    fun getGcodePreview(maxLines: Int = 100): String

    /**
     * Set model instance positions for multi-part printing
     * @param positions List of X,Y coordinates in mm (bed space)
     */
    fun setModelInstances(positions: List<Pair<Float, Float>>): Boolean

    /**
     * Set model scale factors
     */
    fun setModelScale(x: Float, y: Float, z: Float): Boolean

    /**
     * Clean up native resources
     */
    fun dispose()
}

/**
 * Slicing configuration
 */
data class SliceConfig(
    // Print settings
    val layerHeight: Float = 0.2f,
    val firstLayerHeight: Float = 0.3f,
    val perimeters: Int = 2,
    val topSolidLayers: Int = 5,
    val bottomSolidLayers: Int = 4,
    val fillDensity: Float = 0.15f,  // 0.0 - 1.0
    val fillPattern: String = "gyroid",

    // Speed settings (mm/s)
    val printSpeed: Float = 60.0f,
    val travelSpeed: Float = 150.0f,
    val firstLayerSpeed: Float = 20.0f,

    // Temperature
    val nozzleTemp: Int = 210,
    val bedTemp: Int = 60,

    // Retraction
    val retractLength: Float = 0.8f,
    val retractSpeed: Float = 45.0f,

    // Support
    val supportEnabled: Boolean = false,
    val supportType: String = "normal",  // "normal", "tree"
    val supportAngle: Float = 45.0f,

    // Skirt/Brim
    val skirtLoops: Int = 0,
    val skirtDistance: Float = 6.0f,
    val brimWidth: Float = 0.0f,

    // Printer bed (Snapmaker U1: 270x270x270mm)
    val bedSizeX: Float = 270.0f,
    val bedSizeY: Float = 270.0f,
    val maxPrintHeight: Float = 270.0f,

    // Nozzle
    val nozzleDiameter: Float = 0.4f,

    // Filament
    val filamentDiameter: Float = 1.75f,
    val filamentType: String = "PLA",

    // Multi-extruder (up to 4 for Snapmaker U1)
    val extruderCount: Int = 1,
    val extruderTemps: List<Int> = emptyList(),
    val extruderRetractLength: List<Float> = emptyList(),
    val extruderRetractSpeed: List<Float> = emptyList(),

    // Wipe tower (for multi-extruder)
    val wipeTowerEnabled: Boolean = false,
    val wipeTowerX: Float = 170.0f,
    val wipeTowerY: Float = 140.0f,
    val wipeTowerWidth: Float = 60.0f
)

/**
 * Model information
 */
data class ModelInfo(
    val filename: String,
    val format: String,  // "stl", "3mf", "step", "obj"
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float,  // bounding box mm
    val triangleCount: Int,
    val volumeCount: Int,
    val isManifold: Boolean
)

/**
 * Prepare preview mesh for 3D visualization
 * Contains triangle data in world-space coordinates
 */
data class PreparePreviewMesh(
    /**
     * Triangle positions as flat float array
     * 9 floats per triangle (3 vertices × 3 coordinates)
     */
    val trianglePositions: FloatArray,

    /**
     * Per-triangle extruder indices
     * One byte per triangle, values 0-3 for Snapmaker U1
     */
    val extruderIndices: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PreparePreviewMesh) return false

        if (!trianglePositions.contentEquals(other.trianglePositions)) return false
        if (!extruderIndices.contentEquals(other.extruderIndices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trianglePositions.contentHashCode()
        result = 31 * result + extruderIndices.contentHashCode()
        return result
    }
}

/**
 * Slicing result
 */
data class SliceResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val gcodePath: String? = null,
    val totalLayers: Int = 0,
    val estimatedTimeSeconds: Float = 0f,
    val estimatedFilamentMm: Float = 0f,
    val estimatedFilamentGrams: Float = 0f
)
