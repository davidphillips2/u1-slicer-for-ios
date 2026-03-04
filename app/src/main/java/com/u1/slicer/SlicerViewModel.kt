package com.u1.slicer

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.u1.slicer.data.ModelInfo
import com.u1.slicer.data.SliceConfig
import com.u1.slicer.data.SliceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the main slicing screen.
 * Manages the slicing pipeline: load model → configure → slice → view G-code.
 */
class SlicerViewModel(application: Application) : AndroidViewModel(application) {

    private val native = NativeLibrary()

    // ---- UI State ----
    sealed class SlicerState {
        object Idle : SlicerState()
        data class ModelLoaded(val info: ModelInfo) : SlicerState()
        data class Slicing(val progress: Int, val stage: String) : SlicerState()
        data class SliceComplete(val result: SliceResult) : SlicerState()
        data class Error(val message: String) : SlicerState()
    }

    private val _state = MutableStateFlow<SlicerState>(SlicerState.Idle)
    val state: StateFlow<SlicerState> = _state.asStateFlow()

    private val _config = MutableStateFlow(SliceConfig())
    val config: StateFlow<SliceConfig> = _config.asStateFlow()

    private val _coreVersion = MutableStateFlow("")
    val coreVersion: StateFlow<String> = _coreVersion.asStateFlow()

    private val _gcodePreview = MutableStateFlow("")
    val gcodePreview: StateFlow<String> = _gcodePreview.asStateFlow()

    init {
        _coreVersion.value = if (NativeLibrary.isLoaded) {
            native.getCoreVersion()
        } else {
            "Native library not available"
        }
    }

    fun loadModel(uri: Uri) {
        if (!NativeLibrary.isLoaded) {
            _state.value = SlicerState.Error("Native slicer library not available on this device (arm64 required)")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Copy URI content to internal storage
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                    _state.value = SlicerState.Error("Could not open file")
                    return@launch
                }

                val filename = getDisplayName(context, uri) ?: "model.stl"
                val file = File(context.filesDir, filename)
                file.outputStream().use { inputStream.copyTo(it) }

                val success = native.loadModel(file.absolutePath)
                if (success) {
                    val info = native.getModelInfo()
                    if (info != null) {
                        _state.value = SlicerState.ModelLoaded(info)
                    } else {
                        _state.value = SlicerState.Error("Failed to read model info")
                    }
                } else {
                    _state.value = SlicerState.Error("Failed to load model")
                }
            } catch (e: Throwable) {
                _state.value = SlicerState.Error("Error: ${e.message}")
            }
        }
    }

    fun updateConfig(updater: (SliceConfig) -> SliceConfig) {
        _config.value = updater(_config.value)
    }

    fun startSlicing() {
        viewModelScope.launch(Dispatchers.IO) {
            native.progressListener = { pct, stage ->
                _state.value = SlicerState.Slicing(pct, stage)
            }

            _state.value = SlicerState.Slicing(0, "Preparing...")
            val result = native.slice(_config.value)

            native.progressListener = null

            if (result != null && result.success) {
                _state.value = SlicerState.SliceComplete(result)
                _gcodePreview.value = native.getGcodePreview(50)
            } else {
                _state.value = SlicerState.Error(result?.errorMessage ?: "Slicing failed")
            }
        }
    }

    fun clearModel() {
        native.clearModel()
        _state.value = SlicerState.Idle
        _gcodePreview.value = ""
    }

    fun loadProfile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            native.loadProfile(path)
        }
    }

    private fun getDisplayName(context: android.content.Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        // Fallback: extract from path segment
        return uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
    }
}
