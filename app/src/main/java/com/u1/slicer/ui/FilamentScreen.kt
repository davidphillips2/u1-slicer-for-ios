package com.u1.slicer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.u1.slicer.data.FilamentProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilamentScreen(
    filaments: List<FilamentProfile>,
    onAdd: (FilamentProfile) -> Unit,
    onUpdate: (FilamentProfile) -> Unit,
    onDelete: (FilamentProfile) -> Unit,
    onApply: (FilamentProfile) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingFilament by remember { mutableStateOf<FilamentProfile?>(null) }

    if (showDialog) {
        FilamentEditDialog(
            filament = editingFilament,
            onSave = { profile ->
                if (editingFilament != null) onUpdate(profile) else onAdd(profile)
                showDialog = false
                editingFilament = null
            },
            onDismiss = {
                showDialog = false
                editingFilament = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filament Library", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add filament")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (filaments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No filament profiles yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filaments, key = { it.id }) { filament ->
                    FilamentCard(
                        filament = filament,
                        onEdit = {
                            editingFilament = filament
                            showDialog = true
                        },
                        onDelete = { onDelete(filament) },
                        onApply = { onApply(filament) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilamentCard(
    filament: FilamentProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color dot
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(parseColor(filament.color))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(filament.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        filament.material,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("Nozzle", "${filament.nozzleTemp}\u00B0C")
                MiniStat("Bed", "${filament.bedTemp}\u00B0C")
                MiniStat("Speed", "${filament.printSpeed.toInt()} mm/s")
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onApply) { Text("Apply") }
                TextButton(onClick = onEdit) { Text("Edit") }
                if (!filament.isDefault) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilamentEditDialog(
    filament: FilamentProfile?,
    onSave: (FilamentProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(filament?.name ?: "") }
    var material by remember { mutableStateOf(filament?.material ?: "PLA") }
    var nozzleTemp by remember { mutableStateOf(filament?.nozzleTemp?.toString() ?: "210") }
    var bedTemp by remember { mutableStateOf(filament?.bedTemp?.toString() ?: "60") }
    var printSpeed by remember { mutableStateOf(filament?.printSpeed?.toInt()?.toString() ?: "60") }
    var retractLength by remember { mutableStateOf(filament?.retractLength?.toString() ?: "0.8") }
    var retractSpeed by remember { mutableStateOf(filament?.retractSpeed?.toInt()?.toString() ?: "45") }
    var color by remember { mutableStateOf(filament?.color ?: "#808080") }

    val materials = listOf("PLA", "PETG", "ABS", "TPU", "ASA", "PA", "PVA")
    var materialExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (filament != null) "Edit Filament" else "Add Filament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = materialExpanded,
                    onExpandedChange = { materialExpanded = it }
                ) {
                    OutlinedTextField(
                        value = material,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Material") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = materialExpanded,
                        onDismissRequest = { materialExpanded = false }
                    ) {
                        materials.forEach { mat ->
                            DropdownMenuItem(
                                text = { Text(mat) },
                                onClick = {
                                    material = mat
                                    materialExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nozzleTemp,
                        onValueChange = { nozzleTemp = it },
                        label = { Text("Nozzle \u00B0C") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bedTemp,
                        onValueChange = { bedTemp = it },
                        label = { Text("Bed \u00B0C") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = printSpeed,
                    onValueChange = { printSpeed = it },
                    label = { Text("Print Speed (mm/s)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = retractLength,
                        onValueChange = { retractLength = it },
                        label = { Text("Retract (mm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = retractSpeed,
                        onValueChange = { retractSpeed = it },
                        label = { Text("Retract spd") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color (hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(parseColor(color))
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val profile = (filament ?: FilamentProfile(
                        name = "", material = "", nozzleTemp = 0, bedTemp = 0,
                        printSpeed = 0f, retractLength = 0f, retractSpeed = 0f
                    )).copy(
                        name = name.ifBlank { "$material Filament" },
                        material = material,
                        nozzleTemp = nozzleTemp.toIntOrNull() ?: 210,
                        bedTemp = bedTemp.toIntOrNull() ?: 60,
                        printSpeed = printSpeed.toFloatOrNull() ?: 60f,
                        retractLength = retractLength.toFloatOrNull() ?: 0.8f,
                        retractSpeed = retractSpeed.toFloatOrNull() ?: 45f,
                        color = color,
                        isDefault = false
                    )
                    onSave(profile)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun parseColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$cleaned"))
    } catch (_: Exception) {
        Color.Gray
    }
}
