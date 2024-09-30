/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.confirmaccountprovider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.dialogs.SlidingSyncNotSupportedDialog
import io.element.android.features.login.impl.error.ChangeServerError
import io.element.android.features.login.impl.screens.createaccount.AccountCreationNotSupported
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtom
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtomSize
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.pages.OnBoardingPage
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.whiteText
import io.element.android.libraries.matrix.api.auth.OidcDetails
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun SplashScreenView(
    state: ConfirmAccountProviderState,
    onOidcDetails: (OidcDetails) -> Unit,
    onNeedLoginPassword: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onCreateAccountContinue: (url: String) -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onReportProblem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading by remember(state.loginFlow) {
        derivedStateOf {
            state.loginFlow is AsyncData.Loading
        }
    }
    val eventSink = state.eventSink
    OnBoardingPage(
        modifier = modifier,
        footer = {
            ButtonColumnMolecule {
                Button(
                    text = stringResource(id = CommonStrings.action_continue),
                    showProgress = isLoading,
                    onClick = { eventSink.invoke(ConfirmAccountProviderEvents.Continue) },
                    enabled = state.submitEnabled || isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.loginContinue)
                )
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable(onClick = onReportProblem),
                    text = stringResource(id = CommonStrings.common_report_a_problem),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
    ) {
        SplashScreenContent(
            state = state,
            onOpenDeveloperSettings = onOpenDeveloperSettings
        )
        when (state.loginFlow) {
            is AsyncData.Failure -> {
                when (val error = state.loginFlow.error) {
                    is ChangeServerError.Error -> {
                        ErrorDialog(
                            content = error.message(),
                            onSubmit = {
                                eventSink.invoke(ConfirmAccountProviderEvents.ClearError)
                            }
                        )
                    }
                    is ChangeServerError.SlidingSyncAlert -> {
                        SlidingSyncNotSupportedDialog(
                            onLearnMoreClick = {
                                onLearnMoreClick()
                                eventSink(ConfirmAccountProviderEvents.ClearError)
                            },
                            onDismiss = {
                                eventSink(ConfirmAccountProviderEvents.ClearError)
                            }
                        )
                    }
                    is AccountCreationNotSupported -> {
                        ErrorDialog(
                            content = stringResource(CommonStrings.error_account_creation_not_possible),
                            onSubmit = {
                                eventSink.invoke(ConfirmAccountProviderEvents.ClearError)
                            }
                        )
                    }
                }
            }
            is AsyncData.Loading -> Unit // The Continue button shows the loading state
            is AsyncData.Success -> {
                when (val loginFlowState = state.loginFlow.data) {
                    is LoginFlow.OidcFlow -> onOidcDetails(loginFlowState.oidcDetails)
                    LoginFlow.PasswordLogin -> onNeedLoginPassword()
                    is LoginFlow.AccountCreationFlow -> onCreateAccountContinue(loginFlowState.url)
                }
            }
            AsyncData.Uninitialized -> Unit
        }
    }
}
@Composable
private fun SplashScreenContent(
    state: ConfirmAccountProviderState,
    onOpenDeveloperSettings: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(
                horizontalBias = 0f,
                verticalBias = -0.4f
            )
        ) {
            ElementLogoAtom(
                size = ElementLogoAtomSize.Large,
                modifier = Modifier.padding(top = ElementLogoAtomSize.Large.shadowRadius / 2)
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(
                horizontalBias = 0f,
                verticalBias = 0.6f
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = CommonStrings.screen_onboarding_welcome_title),
                    color = ElementTheme.colors.whiteText,
                    style = ElementTheme.typography.fontHeadingLgBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = CommonStrings.screen_onboarding_welcome_message, state.productionApplicationName),
                    color = ElementTheme.materialColors.secondary,
                    style = ElementTheme.typography.fontBodyLgRegular.copy(fontSize = 17.sp),
                    textAlign = TextAlign.Center
                )
            }
        }
        if (state.isDebugBuild) {
            IconButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = onOpenDeveloperSettings,
            ) {
                Icon(
                    imageVector = CompoundIcons.SettingsSolid(),
                    contentDescription = stringResource(CommonStrings.common_settings)
                )
            }
        }
    }
}
