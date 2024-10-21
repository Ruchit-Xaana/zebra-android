/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Pass schema and data as JSON strings
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Form(schema: String, data: String) {
    val context = LocalContext.current

    val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        webViewClient = WebViewClient()
        addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onFormSubmit(data: String) {
                println("Form submitted: $data")
            }
        }, "AndroidInterface")
        loadUrl("file:///android_asset/form.html")
    }
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            webView.evaluateJavascript("initializeForm($schema, $data)",null)
        }
    }

    AndroidView(
        factory = { webView },
    )
}
