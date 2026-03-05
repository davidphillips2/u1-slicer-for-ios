package com.u1.slicer.printer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.u1.slicer.U1SlicerApplication
import com.u1.slicer.network.PrinterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class PrinterViewModel(application: Application) : AndroidViewModel(application) {

    private val printerRepo = (application as U1SlicerApplication).container.printerRepository

    val status: StateFlow<PrinterStatus> = printerRepo.status
    val printerUrl: StateFlow<String> = printerRepo.printerUrl

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sendingState = MutableStateFlow<SendingState>(SendingState.Idle)
    val sendingState: StateFlow<SendingState> = _sendingState.asStateFlow()

    sealed class ConnectionState {
        object Unknown : ConnectionState()
        object Testing : ConnectionState()
        object Connected : ConnectionState()
        object Failed : ConnectionState()
    }

    sealed class SendingState {
        object Idle : SendingState()
        object Uploading : SendingState()
        object Success : SendingState()
        data class Error(val message: String) : SendingState()
    }

    init {
        printerRepo.startPolling(viewModelScope)
    }

    fun updateUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            printerRepo.updateUrl(url)
            _connectionState.value = ConnectionState.Unknown
        }
    }

    fun testConnection() {
        _connectionState.value = ConnectionState.Testing
        viewModelScope.launch(Dispatchers.IO) {
            val ok = printerRepo.testConnection()
            _connectionState.value = if (ok) ConnectionState.Connected else ConnectionState.Failed
        }
    }

    fun sendAndPrint(gcodePath: String) {
        _sendingState.value = SendingState.Uploading
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(gcodePath)
            if (!file.exists()) {
                _sendingState.value = SendingState.Error("G-code file not found")
                return@launch
            }
            val filename = file.name
            val ok = printerRepo.uploadAndPrint(file, filename)
            _sendingState.value = if (ok) {
                SendingState.Success
            } else {
                SendingState.Error("Failed to upload or start print")
            }
        }
    }

    fun pausePrint() {
        viewModelScope.launch(Dispatchers.IO) { printerRepo.pausePrint() }
    }

    fun resumePrint() {
        viewModelScope.launch(Dispatchers.IO) { printerRepo.resumePrint() }
    }

    fun cancelPrint() {
        viewModelScope.launch(Dispatchers.IO) { printerRepo.cancelPrint() }
    }

    fun clearSendingState() {
        _sendingState.value = SendingState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        printerRepo.stopPolling()
    }
}
