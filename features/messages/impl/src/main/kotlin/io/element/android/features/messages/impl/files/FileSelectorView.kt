/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.files.components.FilesTable
import io.element.android.features.messages.impl.files.components.FilesTabs
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectorView(
    state: FileSelectorState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf("documents") }
    LaunchedEffect(Unit) {
        state.eventSink.invoke(FileSelectorEvents.FetchFiles)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select Files",
                        style = ElementTheme.typography.aliasScreenTitle,
                    )
                },
                navigationIcon = { BackButton(onClick = onBackClick) }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp)
        ) {
            FilesTabs(
                displayType = selectedTab,
                setDisplayType = {tab -> selectedTab = tab}
            )
            if(selectedTab == "documents") {
                FilesTable(
                    state = state,
                    onDelete = {state.eventSink(FileSelectorEvents.DeleteFiles(it))},
                    onDownload = {state.eventSink(FileSelectorEvents.DownloadFiles(it))},
                    onUpload = {state.eventSink(FileSelectorEvents.UploadFiles)}
                )
            }
        }
    }
}
