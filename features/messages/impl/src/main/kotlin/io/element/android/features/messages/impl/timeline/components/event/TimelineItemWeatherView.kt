/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.timeline.components.event.widget.WeatherWidget
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayout
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemWeatherContent
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.wysiwyg.compose.EditorStyledText

@Composable
fun TimelineItemWeatherView(
    content: TimelineItemWeatherContent,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit = {},
) {
    CompositionLocalProvider(
        LocalContentColor provides ElementTheme.colors.textPrimary,
        LocalTextStyle provides ElementTheme.typography.fontBodyLgRegular
    ) {
        val weatherData = content.formattedBody
        Box() {
            if(weatherData!=null)
            {
                WeatherWidget(weatherData)
            }
            else{
                EditorStyledText(
                    text = stringResource(R.string.event_timeline_weather_error),
                    onLinkClickedListener = onLinkClick,
                    style = ElementRichTextEditorStyle.textStyle(),
                    onTextLayout = ContentAvoidingLayout.measureLegacyLastTextLine(onContentLayoutChange = onContentLayoutChange),
                    releaseOnDetach = false,
                )
            }
        }
    }
}
