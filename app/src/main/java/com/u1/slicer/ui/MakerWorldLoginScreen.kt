package com.u1.slicer.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.u1.slicer.SlicerViewModel

private const val LOGIN_URL = "https://makerworld.com/en/login"
private val LOGIN_PATH_FRAGMENTS = listOf("/login", "/sign-in", "/sign-up", "/oauth", "/sso")

private fun isPostLoginUrl(url: String): Boolean {
    val parsed = Uri.parse(url)
    if (parsed.host != "makerworld.com") return false
    val path = parsed.path ?: "/"
    return LOGIN_PATH_FRAGMENTS.none { path.contains(it, ignoreCase = true) }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakerWorldLoginScreen(
    viewModel: SlicerViewModel,
    onLoginComplete: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loginHandled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MakerWorld Login") },
                navigationIcon = {
                    IconButton(onClick = {
                        val wv = webView
                        if (wv != null && wv.canGoBack()) {
                            wv.goBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                if (!loginHandled && url != null && isPostLoginUrl(url)) {
                                    val cookies = CookieManager.getInstance()
                                        .getCookie("https://makerworld.com")
                                    if (!cookies.isNullOrBlank()) {
                                        loginHandled = true
                                        CookieManager.getInstance().flush()
                                        viewModel.saveMakerWorldCookies(cookies)
                                        viewModel.saveMakerWorldCookiesEnabled(true)
                                        Toast.makeText(ctx, "Logged in to MakerWorld", Toast.LENGTH_SHORT).show()
                                        onLoginComplete()
                                    }
                                }
                            }
                        }

                        // Note: onCreateWindow is not overridden. Social login
                        // (Google/Facebook/Apple) may fail silently in this WebView.
                        // Users should use Advanced > paste/import as a fallback.
                        webChromeClient = WebChromeClient()

                        // Clear cookies before loading to ensure fresh session
                        CookieManager.getInstance().removeAllCookies {
                            post { loadUrl(LOGIN_URL) }
                        }
                    }
                },
                update = { wv ->
                    webView = wv
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
