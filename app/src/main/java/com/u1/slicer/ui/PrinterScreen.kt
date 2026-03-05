package com.u1.slicer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.u1.slicer.network.PrinterStatus
import com.u1.slicer.printer.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(
    viewModel: PrinterViewModel,
    onBack: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val printerUrl by viewModel.printerUrl.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val sendingState by viewModel.sendingState.collectAsState()

    var urlInput by remember(printerUrl) { mutableStateOf(printerUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Moonraker Connection", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Printer URL") },
                        placeholder = { Text("http://192.168.1.100:7125") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateUrl(urlInput)
                                viewModel.testConnection()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Connect")
                        }

                        when (connectionState) {
                            is PrinterViewModel.ConnectionState.Testing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                            is PrinterViewModel.ConnectionState.Connected -> {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(36.dp))
                            }
                            else -> {}
                        }
                    }

                    // Show error reason inline when connection fails
                    if (connectionState is PrinterViewModel.ConnectionState.Failed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Error, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp))
                            Text(
                                (connectionState as PrinterViewModel.ConnectionState.Failed).reason,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Printer status card
            if (status.isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Print, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Printer Status", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        StatusRow("State", status.state.replaceFirstChar { it.uppercase() },
                            color = when {
                                status.isPrinting -> Color(0xFF4CAF50)
                                status.isPaused -> Color(0xFFFFC107)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (status.isPrinting || status.isPaused) {
                            StatusRow("Progress", "${status.progressPercent}%")
                            StatusRow("File", status.filename)
                            StatusRow("Print Time", status.printTimeFormatted)

                            LinearProgressIndicator(
                                progress = status.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // Temperature card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Temperatures", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary)

                        TempRow("Bed", status.bedTemp, status.bedTarget)

                        for (ext in status.extruders) {
                            TempRow(
                                "Extruder ${ext.index + 1}",
                                ext.temp, ext.target
                            )
                        }

                        if (status.extruders.isEmpty()) {
                            TempRow("Nozzle", status.nozzleTemp, status.nozzleTarget)
                        }
                    }
                }

                // Print control buttons
                if (status.isPrinting || status.isPaused) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (status.isPrinting) {
                            Button(
                                onClick = { viewModel.pausePrint() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107)
                                )
                            ) {
                                Icon(Icons.Default.Pause, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Pause")
                            }
                        }
                        if (status.isPaused) {
                            Button(
                                onClick = { viewModel.resumePrint() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Resume")
                            }
                        }
                        Button(
                            onClick = { viewModel.cancelPrint() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                }
            }

            // Sending state feedback
            when (val s = sendingState) {
                is PrinterViewModel.SendingState.Uploading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            Text("Uploading G-code to printer...")
                        }
                    }
                }
                is PrinterViewModel.SendingState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3D1E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            Text("Print started!", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is PrinterViewModel.SendingState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D1A1A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(s.message, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontWeight = FontWeight.Medium,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TempRow(name: String, actual: Float, target: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(
            "%.0f\u00B0C / %.0f\u00B0C".format(actual, target),
            fontWeight = FontWeight.Medium,
            color = if (target > 0 && actual > 0) {
                if (kotlin.math.abs(actual - target) < 5) Color(0xFF4CAF50) else Color(0xFFFFC107)
            } else MaterialTheme.colorScheme.onSurface
        )
    }
}
