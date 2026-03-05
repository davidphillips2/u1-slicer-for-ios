package com.u1.slicer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.u1.slicer.SlicerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SlicerViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    var nozzleTemp by remember(config) { mutableStateOf(config.nozzleTemp.toString()) }
    var bedTemp by remember(config) { mutableStateOf(config.bedTemp.toString()) }
    var printSpeed by remember(config) { mutableStateOf(config.printSpeed.toInt().toString()) }
    var travelSpeed by remember(config) { mutableStateOf(config.travelSpeed.toInt().toString()) }
    var firstLayerSpeed by remember(config) { mutableStateOf(config.firstLayerSpeed.toInt().toString()) }
    var retractLength by remember(config) { mutableStateOf("%.1f".format(config.retractLength)) }
    var retractSpeed by remember(config) { mutableStateOf(config.retractSpeed.toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.updateConfig {
                            it.copy(
                                nozzleTemp = nozzleTemp.toIntOrNull() ?: it.nozzleTemp,
                                bedTemp = bedTemp.toIntOrNull() ?: it.bedTemp,
                                printSpeed = printSpeed.toFloatOrNull() ?: it.printSpeed,
                                travelSpeed = travelSpeed.toFloatOrNull() ?: it.travelSpeed,
                                firstLayerSpeed = firstLayerSpeed.toFloatOrNull() ?: it.firstLayerSpeed,
                                retractLength = retractLength.toFloatOrNull() ?: it.retractLength,
                                retractSpeed = retractSpeed.toFloatOrNull() ?: it.retractSpeed
                            )
                        }
                        viewModel.saveConfig()
                        onBack()
                    }) {
                        Icon(Icons.Default.Save, "Save")
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
            // Printer info
            SettingsSection("Printer") {
                InfoRow("Model", "Snapmaker U1")
                InfoRow("Build Volume", "270 x 270 x 270 mm")
                InfoRow("Extruders", "4")
            }

            // Temperature
            SettingsSection("Temperature") {
                SettingsTextField("Nozzle Temp (\u00B0C)", nozzleTemp) { nozzleTemp = it }
                SettingsTextField("Bed Temp (\u00B0C)", bedTemp) { bedTemp = it }
            }

            // Speed
            SettingsSection("Speed (mm/s)") {
                SettingsTextField("Print Speed", printSpeed) { printSpeed = it }
                SettingsTextField("Travel Speed", travelSpeed) { travelSpeed = it }
                SettingsTextField("First Layer Speed", firstLayerSpeed) { firstLayerSpeed = it }
            }

            // Retraction
            SettingsSection("Retraction") {
                SettingsTextField("Retract Length (mm)", retractLength) { retractLength = it }
                SettingsTextField("Retract Speed (mm/s)", retractSpeed) { retractSpeed = it }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
