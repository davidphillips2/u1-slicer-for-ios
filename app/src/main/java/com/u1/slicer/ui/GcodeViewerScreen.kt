package com.u1.slicer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.u1.slicer.gcode.MoveType
import com.u1.slicer.gcode.ParsedGcode

// Extruder colors for multi-color prints
private val extruderColors = listOf(
    Color(0xFFE87A00),  // Orange (T0)
    Color(0xFF4FC3F7),  // Light blue (T1)
    Color(0xFF66BB6A),  // Green (T2)
    Color(0xFFEF5350),  // Red (T3)
)

private val travelColor = Color(0xFF333355)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GcodeViewerScreen(
    parsedGcode: ParsedGcode,
    onBack: () -> Unit
) {
    var currentLayer by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showTravel by remember { mutableStateOf(false) }

    val totalLayers = parsedGcode.layers.size
    val layer = if (totalLayers > 0 && currentLayer < totalLayers) {
        parsedGcode.layers[currentLayer]
    } else null

    // Compute bounding box of all moves for auto-fit
    val contentBounds = remember(parsedGcode) {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        for (l in parsedGcode.layers) {
            for (m in l.moves) {
                if (m.type == MoveType.EXTRUDE) {
                    minX = minOf(minX, m.x0, m.x1)
                    minY = minOf(minY, m.y0, m.y1)
                    maxX = maxOf(maxX, m.x0, m.x1)
                    maxY = maxOf(maxY, m.y0, m.y1)
                }
            }
        }
        if (minX < maxX && minY < maxY) {
            floatArrayOf(minX, minY, maxX, maxY)
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("G-code Viewer", fontWeight = FontWeight.Bold)
                        if (layer != null) {
                            Text(
                                "Layer ${currentLayer + 1}/$totalLayers  Z=%.2f mm".format(layer.z),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }) {
                        Icon(Icons.Default.FitScreen, "Reset view")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color(0xFF0A0A18)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Canvas area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A18))
            ) {
                if (layer != null) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 20f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val bedW = parsedGcode.bedWidth
                        val bedH = parsedGcode.bedHeight

                        // Auto-fit to model content if available, else bed
                        val bounds = contentBounds
                        val fitCenterX: Float
                        val fitCenterY: Float
                        val fitScale: Float
                        if (bounds != null) {
                            val contentW = (bounds[2] - bounds[0]).coerceAtLeast(1f)
                            val contentH = (bounds[3] - bounds[1]).coerceAtLeast(1f)
                            fitScale = minOf(canvasWidth / contentW, canvasHeight / contentH) * 0.85f
                            fitCenterX = (bounds[0] + bounds[2]) / 2f
                            fitCenterY = (bounds[1] + bounds[3]) / 2f
                        } else {
                            fitScale = minOf(canvasWidth / bedW, canvasHeight / bedH) * 0.9f
                            fitCenterX = bedW / 2f
                            fitCenterY = bedH / 2f
                        }
                        val totalScale = fitScale * scale
                        val cx = canvasWidth / 2 + offsetX
                        val cy = canvasHeight / 2 + offsetY

                        // Manual coordinate transform: model -> screen
                        fun tx(mx: Float) = cx + (mx - fitCenterX) * totalScale
                        fun ty(my: Float) = cy - (my - fitCenterY) * totalScale // flip Y

                        // Draw bed grid
                        val bedStroke = 1f
                        val gridColor2 = Color(0xFF1A1A30)
                        val borderColor2 = Color(0xFF333355)
                        val step = 10f
                        var gx = 0f
                        while (gx <= bedW) {
                            drawLine(if (gx == 0f || gx == bedW) borderColor2 else gridColor2,
                                Offset(tx(gx), ty(0f)), Offset(tx(gx), ty(bedH)),
                                strokeWidth = if (gx == 0f || gx == bedW) bedStroke else 0.5f)
                            gx += step
                        }
                        var gy = 0f
                        while (gy <= bedH) {
                            drawLine(if (gy == 0f || gy == bedH) borderColor2 else gridColor2,
                                Offset(tx(0f), ty(gy)), Offset(tx(bedW), ty(gy)),
                                strokeWidth = if (gy == 0f || gy == bedH) bedStroke else 0.5f)
                            gy += step
                        }

                        // Draw moves
                        for (move in layer.moves) {
                            if (move.type == MoveType.TRAVEL && !showTravel) continue

                            val color = if (move.type == MoveType.TRAVEL) {
                                travelColor
                            } else {
                                extruderColors.getOrElse(move.extruder) { extruderColors[0] }
                            }

                            val sw = if (move.type == MoveType.EXTRUDE) 2f else 0.5f

                            drawLine(
                                color = color,
                                start = Offset(tx(move.x0), ty(move.y0)),
                                end = Offset(tx(move.x1), ty(move.y1)),
                                strokeWidth = sw
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No G-code data", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Layer info row
                if (layer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${layer.moves.count { it.type == MoveType.EXTRUDE }} extrusion moves",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row {
                            Text(
                                "Travel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Switch(
                                checked = showTravel,
                                onCheckedChange = { showTravel = it },
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Layer slider
                if (totalLayers > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Slider(
                            value = currentLayer.toFloat(),
                            onValueChange = { currentLayer = it.toInt() },
                            valueRange = 0f..(totalLayers - 1).toFloat(),
                            steps = if (totalLayers > 2) totalLayers - 2 else 0,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text(
                            "$totalLayers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Layer number chip
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "Layer ${currentLayer + 1} / $totalLayers",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

