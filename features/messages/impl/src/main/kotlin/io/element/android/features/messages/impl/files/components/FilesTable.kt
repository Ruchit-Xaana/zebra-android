/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files.components

import android.annotation.SuppressLint
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.files.FileSelectorEvents
import io.element.android.features.messages.impl.files.FileSelectorState
import io.element.android.features.messages.impl.files.MatrixFile
import io.element.android.libraries.designsystem.icons.ZebraIcons
import io.element.android.libraries.designsystem.theme.components.Checkbox
import io.element.android.libraries.designsystem.theme.floatingActionDoneColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FilesTable(
    state: FileSelectorState,
    onDelete: (List<MatrixFile>) -> Unit,
    onDone: (List<MatrixFile>) -> Unit,
    onDownload: (List<MatrixFile>) -> Unit,
    onUpload: () -> Unit,
) {
    val files = state.documents
    val scrollState = rememberScrollState()
    val selectedMatrixFiles = remember { mutableStateListOf<MatrixFile>() }
    if (state.completedFileOp) {
        selectedMatrixFiles.clear()
        LaunchedEffect(Unit) {
            state.eventSink(FileSelectorEvents.ResetSelection)
            state.eventSink(FileSelectorEvents.FetchFiles)
        }
    }
    val allSelected = selectedMatrixFiles.size == files.size && files.isNotEmpty()
    val toggleSelectAll = { isSelected: Boolean ->
        selectedMatrixFiles.clear()
        if (isSelected) {
            selectedMatrixFiles.addAll(files)
        }
    }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Calculate weights based on available width
    val checkboxWidth = screenWidth * 0.10f
    val nameWidth = screenWidth * 0.4f // 25% of screen width for name
    val senderWidth = screenWidth * 0.4f // 25% of screen width for sender
    val roomWidth = screenWidth * 0.20f // 20% of screen width for room
    val sharedOnWidth = screenWidth * 0.35f // 15% of screen width for shared on
    val fileSizeWidth = screenWidth * 0.35f // 15% of screen width for file size
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onDelete(selectedMatrixFiles.toList()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = ElementTheme.colors.iconDisabled
                    ),
                    enabled = selectedMatrixFiles.isNotEmpty(),
                    modifier = Modifier.weight(0.2f)
                ) {
                    Icon(ZebraIcons.DeleteIcon(), contentDescription = "Delete")
                }
                Row(
                    modifier = Modifier.weight(0.8f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
                ) {
                    Button(
                        onClick = { onDownload(selectedMatrixFiles.toList()) },
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.LightGray, disabledContentColor = ElementTheme.colors.iconDisabled),
                        enabled = selectedMatrixFiles.isNotEmpty(),
                    ) {
                        Icon(ZebraIcons.DownloadIcon(), contentDescription = "Download")
                    }
                    if (state.isBusy) {
                        CircularProgressIndicator(
                            progress = { state.progress / 3f },
                            color = Color.Green,
                            trackColor = Color.LightGray
                        )
                    } else {
                        Button(onClick = onUpload) {
                            Icon(ZebraIcons.UploadIcon(), contentDescription = "Upload")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { isChecked -> toggleSelectAll(isChecked) },
                            modifier = Modifier.width(checkboxWidth).padding(end = 8.dp)
                        )
                        Text("Name", fontWeight = FontWeight.Bold, modifier = Modifier.width(nameWidth))
                        Text("Sender", fontWeight = FontWeight.Bold, modifier = Modifier.width(senderWidth))
                        Text("Room", fontWeight = FontWeight.Bold, modifier = Modifier.width(roomWidth))
                        Text("Shared On", fontWeight = FontWeight.Bold, modifier = Modifier.width(sharedOnWidth))
                        Text("File Size", fontWeight = FontWeight.Bold, modifier = Modifier.width(fileSizeWidth))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                items(files){ file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedMatrixFiles.contains(file),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    selectedMatrixFiles.add(file)
                                } else {
                                    selectedMatrixFiles.remove(file)
                                }
                            },
                            modifier = Modifier.width(checkboxWidth).padding(end = 8.dp)
                        )
                        Text(file.name, modifier = Modifier.width(nameWidth))
                        Text(file.sender, modifier = Modifier.width(senderWidth))
                        Text(file.roomId ?: "N/A", modifier = Modifier.width(roomWidth))
                        Text(formatDate(file.timestamp), modifier = Modifier.width(sharedOnWidth))
                        Text(prettyFileSize(file.fileSize?.toLong() ?: 0), modifier = Modifier.width(fileSizeWidth))
                    }
                }
            }
        }
        if(selectedMatrixFiles.isNotEmpty()) {
            FloatingActionButton(
                onClick = {onDone(selectedMatrixFiles.toList())},
                containerColor = ElementTheme.colors.floatingActionDoneColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 16.dp, top = 16.dp, bottom = 50.dp, end = 30.dp)
            ) {
                Icon(ZebraIcons.DoneIcon(), contentDescription = "Done")
            }
        }
    }
}


@SuppressLint("DefaultLocale")
private fun prettyFileSize(bytes: Long): String {
    val units = arrayOf("Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    val finalSize = if (unitIndex <= 1) {
        Math.round(size).toString()
    } else {
        String.format("%.1f", size).replace(".0", "")
    }

    return "$finalSize ${units[unitIndex]}"
}

private fun formatDate(timestamp: Date): String {
    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val todayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    return if (isToday(timestamp)) {
        dateFormat.format(timestamp)
    } else {
        todayFormat.format(timestamp)
    }
}

private fun isToday(date: Date): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = date
    val today = Calendar.getInstance()

    return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

