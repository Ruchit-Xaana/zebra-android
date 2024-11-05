/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayout
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemPdfSearchContent
import io.element.android.libraries.designsystem.icons.ZebraIcons
import io.element.android.libraries.designsystem.theme.font.rubikFontFamily
import io.element.android.libraries.designsystem.theme.hyperlinkColor
import io.element.android.libraries.designsystem.theme.promptColor
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.ui.messages.LocalRoomMemberProfilesCache
import io.element.android.libraries.matrix.ui.messages.RoomMemberProfilesCache
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.libraries.textcomposer.mentions.LocalMentionSpanTheme
import io.element.android.libraries.textcomposer.mentions.MentionSpan
import io.element.android.libraries.textcomposer.mentions.getMentionSpans
import io.element.android.libraries.textcomposer.mentions.updateMentionStyles
import io.element.android.wysiwyg.compose.EditorStyledText

@Composable
fun TimelineItemPdfSearchFinalView(
    content: TimelineItemPdfSearchContent,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
    onPromptClick: (String) -> Unit,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit = {},
) {
    val body = getBodyWithResolvedMentions(content.body,content.formattedBody)
    val fileNames = content.additionalData?.file_names
    val webUrls = content.additionalData?.web_url
    val prompt = content.additionalData?.file_prompt
    val question= content.additionalData?.raw_question?.let {
        if (!it.endsWith("?")) "$it?" else it
    }
    CompositionLocalProvider(
        LocalContentColor provides ElementTheme.colors.textPrimary,
        LocalTextStyle provides ElementTheme.typography.fontBodyLgRegular
    ) {
        Box(modifier = modifier.padding(10.dp)) {
            Column (){
                if(question!=null)IconWithText(Icons.Filled.QuestionAnswer,question, FontWeight.Bold,20.sp,35.dp)
                Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                IconWithText(ZebraIcons.AttachmentIcon(),"Files:", FontWeight.Bold,16.sp,35.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    fileNames?.forEach { name ->
                        Row(
                            modifier = Modifier.padding(vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Circle, // Use your desired icon here
                                contentDescription = null, // Provide a description for accessibility
                                modifier = Modifier.size(10.dp) // Adjust size as needed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilesPillWithoutRemoveNoIcon(name)
                        }
                    }
                }
                Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                IconWithText(Icons.AutoMirrored.Filled.List,"Sources:", FontWeight.Bold,16.sp,35.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    webUrls?.forEach { link ->
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier.padding(vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Icon(
                                imageVector = Icons.Default.Circle, // Use your desired icon here
                                contentDescription = null, // Provide a description for accessibility
                                modifier = Modifier.size(10.dp) // Adjust size as needed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ClickableText(
                                text = buildAnnotatedString{
                                    pushStyle(style = SpanStyle(fontSize = 18.sp))
                                    pop()
                                    pushStyle(
                                        style = SpanStyle(
                                            color = ElementTheme.colors.hyperlinkColor,
                                            textDecoration = TextDecoration.Underline,
                                            fontFamily = rubikFontFamily
                                        )
                                    )
                                    append(link)
                                    pop()
                                },
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                    }
                }
                Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                IconWithText(Icons.Outlined.Notifications,"Answer:", FontWeight.Bold,16.sp,35.dp)
                Spacer(modifier = Modifier.height(6.dp))
                EditorStyledText(
                    text = body,
                    onLinkClickedListener = onLinkClick,
                    style = ElementRichTextEditorStyle.textStyle(),
                    onTextLayout = ContentAvoidingLayout.measureLegacyLastTextLine(onContentLayoutChange = onContentLayoutChange),
                    releaseOnDetach = false,
                )
                Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                IconWithText(Icons.Filled.Search,"Related:", FontWeight.Bold,16.sp,35.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Column { // Display related questions in a column
                    prompt?.forEach { relatedPrompt ->
                        Row(
                            modifier = Modifier.padding(vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ClickableText(
                                text = buildAnnotatedString{
                                    pushStyle(style = SpanStyle(fontSize = 18.sp))
                                    pop()
                                    pushStyle(
                                        style = SpanStyle(
                                            color = ElementTheme.colors.promptColor,
                                            fontFamily = rubikFontFamily
                                        )
                                    )
                                    append(relatedPrompt)
                                    pop()
                                },
                                onClick = {
                                    onPromptClick(relatedPrompt)
                                },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                    }
                }
            }
        }
    }
}

@Suppress("LocalVariableName")
@Composable
private fun getBodyWithResolvedMentions(body: String, formattedBody: CharSequence?): CharSequence {
    val userProfileCache = LocalRoomMemberProfilesCache.current
    val lastCacheUpdate by userProfileCache.lastCacheUpdate.collectAsState()
    val mentionSpanTheme = LocalMentionSpanTheme.current
    val formatted_Body = formattedBody ?: body
    val textWithMentions = remember(formatted_Body, mentionSpanTheme, lastCacheUpdate) {
        updateMentionSpans(formatted_Body, userProfileCache)
        mentionSpanTheme.updateMentionStyles(formatted_Body)
        formatted_Body
    }
    return SpannableString(textWithMentions)
}
private fun updateMentionSpans(text: CharSequence, cache: RoomMemberProfilesCache): Boolean {
    var changedContents = false
    for (mentionSpan in text.getMentionSpans()) {
        when (mentionSpan.type) {
            MentionSpan.Type.USER -> {
                val displayName = cache.getDisplayName(UserId(mentionSpan.rawValue)) ?: mentionSpan.rawValue
                if (mentionSpan.text != displayName) {
                    changedContents = true
                    mentionSpan.text = displayName
                }
            }
            // There's no need to do anything for `@room` pills
            MentionSpan.Type.EVERYONE -> Unit
            // Nothing yet for room mentions
            MentionSpan.Type.ROOM -> Unit
        }
    }
    return changedContents
}
