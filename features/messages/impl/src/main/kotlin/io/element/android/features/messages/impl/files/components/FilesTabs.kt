/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.icons.ZebraIcons
import java.util.Locale
import androidx.compose.material3.Icon as MaterialIcon

@Composable
fun FilesTabs(
    displayType: String,
    setDisplayType: (String) -> Unit,
    setRowSelection: ((Map<String, Boolean>) -> Unit)? = null
) {
    val tabs = listOf("documents", "media")

    TabRow(
        selectedTabIndex = tabs.indexOf(displayType),
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == displayType,
                onClick = {
                    setDisplayType(tab)
                    if (tab == "media") {
                        setRowSelection?.invoke(emptyMap())
                    }
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    MaterialIcon(imageVector = when (tab) {
                        "documents" -> ZebraIcons.DocumentsIcon()
                        "media" -> ZebraIcons.MediaIcon()
                        else -> ZebraIcons.DocumentsIcon()
                    }, contentDescription = tab)
                    Text(
                        text = tab.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

