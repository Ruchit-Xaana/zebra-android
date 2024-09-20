/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayout
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemEmptyMessageContent
import io.element.android.features.messages.impl.timeline.model.event.WebSearchResultViewModel
import io.element.android.libraries.designsystem.theme.font.rubikFontFamily
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.wysiwyg.compose.EditorStyledText

@Composable
fun TimelineItemWebSearchResultView(
    roomId: String?,
    eventId: String?,
    content: TimelineItemEmptyMessageContent,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
    viewModel: WebSearchResultViewModel = viewModel(),
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit = {},
) {
    if(eventId!=null&&eventId.toString()!=viewModel.eventId)viewModel.initializeTextAndLinks()


    val body = content.body
    val debugInfo = content.contentInfo
    val displayedText by viewModel.displayedText
    val links by viewModel.displayedLinks.collectAsState()
    var hasFetchedData by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalContentColor provides ElementTheme.colors.textPrimary,
        LocalTextStyle provides ElementTheme.typography.fontBodyLgRegular
    ) {

        Box(modifier = modifier.padding(10.dp)) {
            Column() {
                Text(text = "Sources:", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = rubikFontFamily) // Heading for the links
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    links.forEach { link ->
                        val context = LocalContext.current
                        ClickableText(
                            text = buildAnnotatedString {
                                pushStyle(style = SpanStyle(fontSize = 18.sp)) // Increase bullet point size
                                append("\u2022")
                                pop() // Bullet point with space
                                pushStyle(
                                    style = SpanStyle(
                                        color = Color.Blue, // Set link color
                                        textDecoration = TextDecoration.Underline, // Add underline
                                        fontFamily = rubikFontFamily
                                    )
                                )
                                append(link.toString()) // Append the link text
                                pop()
                            },
                            onClick = {
                                val url = link.toString() // Get the URL from the Spanned object
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp)) // Add some space between links and text
                Text(text = "Answer:", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = rubikFontFamily) // Heading for the links
                Spacer(modifier = Modifier.height(6.dp))
                EditorStyledText(
                    text = displayedText,
                    onLinkClickedListener = onLinkClick,
                    style = ElementRichTextEditorStyle.textStyle(),
                    onTextLayout = ContentAvoidingLayout.measureLegacyLastTextLine(onContentLayoutChange = onContentLayoutChange),
                    releaseOnDetach = false,
                )
            }

            LaunchedEffect(Unit) {
                if (!hasFetchedData) viewModel.fetchAndProcessData(debugInfo,roomId,eventId)
                hasFetchedData = true
            }
        }

        }
}
