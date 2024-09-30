/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.confirmaccountprovider

import io.element.android.appconfig.ApplicationConfig
import io.element.android.features.login.impl.accountprovider.AccountProvider
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.auth.OidcDetails

// Do not use default value, so no member get forgotten in the presenters.
data class ConfirmAccountProviderState(
    val accountProvider: AccountProvider,
    val isAccountCreation: Boolean,
    val loginFlow: AsyncData<LoginFlow>,
    val eventSink: (ConfirmAccountProviderEvents) -> Unit,
    /**
     * Temporary default values
     */
    val productionApplicationName: String = ApplicationConfig.PRODUCTION_APPLICATION_NAME,
    val isDebugBuild: Boolean=true,
) {
    val submitEnabled: Boolean get() = accountProvider.url.isNotEmpty() && (loginFlow is AsyncData.Uninitialized || loginFlow is AsyncData.Loading)
}

sealed interface LoginFlow {
    data object PasswordLogin : LoginFlow
    data class OidcFlow(val oidcDetails: OidcDetails) : LoginFlow
    data class AccountCreationFlow(val url: String) : LoginFlow
}
