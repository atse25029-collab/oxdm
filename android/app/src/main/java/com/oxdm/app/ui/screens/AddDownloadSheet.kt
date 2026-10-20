package com.oxdm.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oxdm.app.data.JobCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDownloadSheet(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (url: String, filename: String?, category: JobCategory, parts: Int) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var filename by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(JobCategory.All) }
    var partsCount by remember { mutableFloatStateOf(8f) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "New Download",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    if (filename.isEmpty()) {
                        filename = it.substringAfterLast("/").substringBefore("?").ifEmpty { "" }
                    }
                },
                label = { Text("Download URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = filename,
                onValueChange = { filename = it },
                label = { Text("Save As (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                listOf(JobCategory.All, JobCategory.Compressed, JobCategory.Video, JobCategory.Document).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Connection Parts: ${partsCount.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Slider(
                    value = partsCount,
                    onValueChange = { partsCount = it },
                    valueRange = 1f..32f,
                    steps = 30,
                    modifier = Modifier.weight(1.5f)
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            onConfirm(url, filename.ifBlank { null }, selectedCategory, partsCount.toInt())
                        }
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Download")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
