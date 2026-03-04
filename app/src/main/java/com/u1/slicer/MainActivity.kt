package com.u1.slicer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.u1.slicer.data.ModelInfo
import com.u1.slicer.data.SliceResult

class MainActivity : ComponentActivity() {
    private val viewModel: SlicerViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadModel(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            U1SlicerTheme {
                SlicerScreen(
                    viewModel = viewModel,
                    onPickFile = {
                        filePickerLauncher.launch(arrayOf(
                            "application/sla",              // STL
                            "model/stl",
                            "application/vnd.ms-pki.stl",
                            "application/octet-stream",     // generic binary
                            "model/3mf",
                            "application/vnd.ms-3mfdocument",
                            "model/obj",
                            "*/*"                           // fallback
                        ))
                    }
                )
            }
        }
    }
}

// =============================================================================
// Theme
// =============================================================================
@Composable
fun U1SlicerTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFFE87A00),        // Prusa orange
        onPrimary = Color.White,
        primaryContainer = Color(0xFF3D2000),
        secondary = Color(0xFF4FC3F7),
        surface = Color(0xFF1A1A2E),
        surfaceVariant = Color(0xFF222240),
        background = Color(0xFF0F0F1E),
        onBackground = Color(0xFFE0E0E0),
        onSurface = Color(0xFFE0E0E0),
        error = Color(0xFFEF5350)
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

// =============================================================================
// Main Screen
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlicerScreen(
    viewModel: SlicerViewModel,
    onPickFile: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val config by viewModel.config.collectAsState()
    val coreVersion by viewModel.coreVersion.collectAsState()
    val gcodePreview by viewModel.gcodePreview.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("U1 Slicer", fontWeight = FontWeight.Bold)
                        Text(
                            coreVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (state !is SlicerViewModel.SlicerState.Idle) {
                        IconButton(onClick = { viewModel.clearModel() }) {
                            Icon(Icons.Default.Clear, "Clear model")
                        }
                    }
                }
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
            when (val s = state) {
                is SlicerViewModel.SlicerState.Idle -> {
                    IdleContent(onPickFile)
                }
                is SlicerViewModel.SlicerState.ModelLoaded -> {
                    ModelInfoCard(s.info)
                    ConfigCard(config, viewModel::updateConfig)
                    SliceButton(onClick = { viewModel.startSlicing() })
                }
                is SlicerViewModel.SlicerState.Slicing -> {
                    SlicingProgressCard(s.progress, s.stage)
                }
                is SlicerViewModel.SlicerState.SliceComplete -> {
                    SliceCompleteCard(s.result)
                    if (gcodePreview.isNotEmpty()) {
                        GcodePreviewCard(gcodePreview)
                    }
                }
                is SlicerViewModel.SlicerState.Error -> {
                    ErrorCard(s.message, onPickFile)
                }
            }
        }
    }
}

// =============================================================================
// UI Components
// =============================================================================

@Composable
fun IdleContent(onPickFile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Load a 3D Model",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Supports STL, 3MF, OBJ, STEP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Browse Files")
            }
        }
    }
}

@Composable
fun ModelInfoCard(info: ModelInfo) {
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
                Icon(Icons.Default.ViewInAr, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(info.filename, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            InfoRow("Format", info.format.uppercase())
            InfoRow("Dimensions", info.dimensionString)
            InfoRow("Triangles", "%,d".format(info.triangleCount))
            InfoRow("Volumes", info.volumeCount.toString())
            InfoRow("Manifold", if (info.isManifold) "✓ Yes" else "✗ No")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ConfigCard(
    config: com.u1.slicer.data.SliceConfig,
    onUpdate: ((com.u1.slicer.data.SliceConfig) -> com.u1.slicer.data.SliceConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Slice Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle settings"
                    )
                }
            }

            // Quick summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat("Layer", "%.2f mm".format(config.layerHeight))
                QuickStat("Infill", "%.0f%%".format(config.fillDensity * 100))
                QuickStat("Support", if (config.supportEnabled) "On" else "Off")
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text("Layer Height (mm)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = config.layerHeight,
                        onValueChange = { v -> onUpdate { it.copy(layerHeight = v) } },
                        valueRange = 0.05f..0.6f,
                        steps = 10
                    )

                    Text("Infill Density (%)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = config.fillDensity,
                        onValueChange = { v -> onUpdate { it.copy(fillDensity = v) } },
                        valueRange = 0f..1f,
                        steps = 19
                    )

                    Text("Perimeters", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = config.perimeters.toFloat(),
                        onValueChange = { v -> onUpdate { it.copy(perimeters = v.toInt()) } },
                        valueRange = 1f..6f,
                        steps = 4
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Support Enabled")
                        Switch(
                            checked = config.supportEnabled,
                            onCheckedChange = { v -> onUpdate { it.copy(supportEnabled = v) } }
                        )
                    }

                    InfoRow("Nozzle Temp", "${config.nozzleTemp}°C")
                    InfoRow("Bed Temp", "${config.bedTemp}°C")
                    InfoRow("Nozzle Diameter", "${config.nozzleDiameter} mm")
                    InfoRow("Filament", config.filamentType)
                }
            }
        }
    }
}

@Composable
fun QuickStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun SliceButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text("Slice Model", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SlicingProgressCard(progress: Int, stage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.size(80.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Text("$progress%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(stage, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun SliceCompleteCard(result: SliceResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B3D1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                Spacer(Modifier.width(8.dp))
                Text("Slicing Complete", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = Color(0xFF81C784))
            }
            Divider(color = Color.White.copy(alpha = 0.1f))
            InfoRow("Layers", result.totalLayers.toString())
            InfoRow("Est. Time", result.estimatedTimeFormatted)
            InfoRow("Filament", result.estimatedFilamentFormatted)
        }
    }
}

@Composable
fun GcodePreviewCard(gcode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("G-code Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D1A))
                    .padding(12.dp)
            ) {
                Text(
                    gcode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF80CBC4),
                    maxLines = 30,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D1A1A)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Error", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error)
            }
            Text(message, color = Color.White.copy(alpha = 0.8f))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Try Again")
            }
        }
    }
}
