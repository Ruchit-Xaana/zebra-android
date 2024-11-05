/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.text.SpannableString
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayout
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemFileSearchQueryContent
import io.element.android.features.messages.impl.utils.containsOnlyEmojis
import io.element.android.libraries.designsystem.icons.ZebraIcons
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.mentionPillBackground
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
fun TimelineItemFileSearchQueryView(
    content: TimelineItemFileSearchQueryContent,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit = {},
) {
    val emojiOnly = (content.formattedBody == null || content.formattedBody.toString() == content.body) &&
        content.body.replace(" ", "").containsOnlyEmojis()
    val textStyle = when {
        emojiOnly -> ElementTheme.typography.fontHeadingXlRegular
        else -> ElementTheme.typography.fontBodyLgRegular
    }
    CompositionLocalProvider(
        LocalContentColor provides ElementTheme.colors.textPrimary,
        LocalTextStyle provides textStyle
    ) {
        val body = getTextWithResolvedMentions(content)
        Box(modifier.semantics { contentDescription = content.plainText }) {
            Column {
                content.fileNames.forEach { file ->
                    FilesPillWithoutRemove(file)
                }
                EditorStyledText(
                    text = body,
                    onLinkClickedListener = onLinkClick,
                    style = ElementRichTextEditorStyle.textStyle(),
                    onTextLayout = ContentAvoidingLayout.measureLegacyLastTextLine(onContentLayoutChange = onContentLayoutChange),
                    releaseOnDetach = false,
                )
            }
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun getTextWithResolvedMentions(content: TimelineItemFileSearchQueryContent): CharSequence {
    val userProfileCache = LocalRoomMemberProfilesCache.current
    val lastCacheUpdate by userProfileCache.lastCacheUpdate.collectAsState()
    val mentionSpanTheme = LocalMentionSpanTheme.current
    val formattedBody = content.formattedBody ?: content.pillifiedBody
    val textWithMentions = remember(formattedBody, mentionSpanTheme, lastCacheUpdate) {
        updateMentionSpans(formattedBody, userProfileCache)
        mentionSpanTheme.updateMentionStyles(formattedBody)
        formattedBody
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

@Composable
internal fun FilesPillWithoutRemove(fileName: String) {
    val truncatedName = if (fileName.length > 20) {
        "${fileName.take(18)}..."
    } else {
        fileName
    }

    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ElementTheme.colors.mentionPillBackground)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ZebraIcons.AttachmentIcon(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = truncatedName,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@Composable
internal fun FilesPillWithoutRemoveNoIcon(fileName: String) {
    val truncatedName = if (fileName.length > 20) {
        "${fileName.take(18)}..."
    } else {
        fileName
    }

    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ElementTheme.colors.mentionPillBackground)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = truncatedName,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


