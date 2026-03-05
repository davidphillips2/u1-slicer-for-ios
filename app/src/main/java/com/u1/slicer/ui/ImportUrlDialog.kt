package com.u1.slicer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.u1.slicer.network.MakerWorldClient

@Composable
fun ImportUrlDialog(
    isLoading: Boolean,
    progress: Int,
    error: String?,
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val isValid = MakerWorldClient.isValidInput(url)

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Import from MakerWorld") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste a MakerWorld URL or design ID to download and import a 3MF file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL or Design ID") },
                    placeholder = { Text("https://makerworld.com/en/models/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = url.isNotBlank() && !isValid
                )

                if (url.isNotBlank() && !isValid) {
                    Text(
                        "Invalid MakerWorld URL or design ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Downloading... $progress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (error != null) {
                    Text(
                        error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(url) },
                enabled = isValid && !isLoading
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
