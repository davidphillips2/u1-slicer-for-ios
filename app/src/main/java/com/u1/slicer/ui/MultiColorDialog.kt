package com.u1.slicer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.u1.slicer.data.FilamentProfile

/**
 * Data class for per-extruder assignment.
 */
data class ExtruderAssignment(
    val extruderIndex: Int,
    val color: String, // hex color e.g. "#FF0000"
    val temperature: Int = 210,
    val filamentName: String = ""
)

/**
 * Dialog shown when a multi-color 3MF is loaded.
 * Shows detected colors and lets users assign filament profiles to each extruder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiColorDialog(
    detectedColors: List<String>,
    extruderCount: Int,
    filaments: List<FilamentProfile>,
    onConfirm: (List<ExtruderAssignment>) -> Unit,
    onDismiss: () -> Unit
) {
    val assignments = remember(detectedColors, extruderCount) {
        mutableStateListOf<ExtruderAssignment>().apply {
            for (i in 0 until extruderCount) {
                add(ExtruderAssignment(
                    extruderIndex = i,
                    color = detectedColors.getOrElse(i) { "#808080" },
                    temperature = 210
                ))
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Multi-Color Detected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$extruderCount extruder(s) detected. Assign filament to each.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(assignments) { index, assignment ->
                        ExtruderRow(
                            assignment = assignment,
                            filaments = filaments,
                            onUpdate = { updated -> assignments[index] = updated }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Wipe tower note
                if (extruderCount > 1) {
                    Text(
                        "A wipe tower will be generated for color transitions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(assignments.toList()) }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtruderRow(
    assignment: ExtruderAssignment,
    filaments: List<FilamentProfile>,
    onUpdate: (ExtruderAssignment) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color swatch
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(parseHexColor(assignment.color))
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Extruder ${assignment.extruderIndex + 1}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            // Filament selector
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = assignment.filamentName.ifEmpty { "Default (${assignment.temperature}C)" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Default (210C)") },
                        onClick = {
                            onUpdate(assignment.copy(temperature = 210, filamentName = ""))
                            expanded = false
                        }
                    )
                    filaments.forEach { filament ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(filament.color))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("${filament.name} (${filament.nozzleTemp}C)")
                                }
                            },
                            onClick = {
                                onUpdate(assignment.copy(
                                    temperature = filament.nozzleTemp,
                                    filamentName = filament.name
                                ))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        when (cleaned.length) {
            6 -> Color(android.graphics.Color.parseColor("#FF$cleaned"))
            8 -> Color(android.graphics.Color.parseColor("#$cleaned"))
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}
