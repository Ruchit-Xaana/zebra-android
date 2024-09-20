/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import io.element.android.features.messages.impl.timeline.components.event.widget.WeatherData

data class TimelineItemWeatherContent(
    val body: String,
    val formattedBody: WeatherData?,
): TimelineItemEventContent {
    override val type: String = "TimelineItemWeatherContent"
}
