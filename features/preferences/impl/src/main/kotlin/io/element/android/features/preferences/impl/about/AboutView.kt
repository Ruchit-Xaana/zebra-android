/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.preferences.impl.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.components.preferences.PreferenceText
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun AboutView(
    onFAQClick: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        modifier = modifier,
        onBackClick = onBackClick,
        title = stringResource(id = CommonStrings.common_about)
    ) {
        PreferenceText(
            title = stringResource(id = CommonStrings.common_faq),
            onClick = onFAQClick,
        )
        PreferenceText(
            title = stringResource(id = CommonStrings.common_open_source_licenses),
            onClick = onOpenSourceLicensesClick,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun AboutViewPreview(@PreviewParameter(AboutStateProvider::class) state: AboutState) = ElementPreview {
    AboutView(
        onFAQClick = {},
        onOpenSourceLicensesClick = {},
        onBackClick = {},
    )
}
