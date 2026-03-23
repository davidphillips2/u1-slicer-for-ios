package com.u1.slicer.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
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

private const val START_URL = "https://makerworld.com/en"

/**
 * Extract a clean filename from a Content-Disposition header and URL.
 * Handles RFC 5987 `filename*=UTF-8''...` syntax that [URLUtil.guessFileName] may misparse.
 */
internal fun resolveDownloadFilename(url: String, contentDisposition: String?, mimeType: String?): String {
    var filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
    // URLUtil.guessFileName can return the raw disposition on filename* extended syntax
    if (filename.contains(";") || filename.contains("'")) {
        val match = Regex("filename=\"?([^\";\n]+)\"?").find(contentDisposition ?: "")
        if (match != null) filename = match.groupValues[1].trim()
    }
    return filename
}

/** Sanitize a filename to prevent path traversal and invalid filesystem characters. */
internal fun sanitizeFilename(filename: String): String =
    filename.replace(Regex("[/\\\\:*?\"<>|]"), "_")

/**
 * Check whether cookies contain a real auth token (not just Cloudflare bot management).
 * MakerWorld's two-stage login sets auth cookies only after the user clicks "Continue".
 * We look for known auth cookie names or a cookie string long enough to contain a JWT.
 * False positives are harmless — saving unauthenticated cookies just means the OkHttp
 * pipeline gets a 403 and falls back to the normal error message.
 */
internal fun hasAuthCookies(cookies: String): Boolean =
    cookies.contains("token=") ||
        cookies.contains("sessionid") ||
        cookies.length > 500

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakerWorldBrowserScreen(
    viewModel: SlicerViewModel,
    onModelDownloaded: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

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
                title = { Text("MakerWorld") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
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
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        val mainWebView = this

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
                                // Try to extract auth cookies on every makerworld.com page load.
                                // The two-stage login sets real auth cookies only after the
                                // second "Continue" step, so we check every page.
                                if (url != null) {
                                    val parsed = Uri.parse(url)
                                    if (parsed.host == "makerworld.com") {
                                        val cookies = CookieManager.getInstance()
                                            .getCookie("https://makerworld.com") ?: ""
                                        if (hasAuthCookies(cookies)) {
                                            Log.i("MWBrowser", "Auth cookies detected (${cookies.length} chars)")
                                            viewModel.saveMakerWorldCookies(cookies)
                                            viewModel.saveMakerWorldCookiesEnabled(true)
                                        }
                                    }
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val popupWebView = WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.javaScriptCanOpenWindowsAutomatically = true
                                    settings.setSupportMultipleWindows(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            popupView: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val targetUrl = request?.url?.toString() ?: return false
                                            mainWebView.loadUrl(targetUrl)
                                            popupView?.destroy()
                                            return true
                                        }

                                        override fun onPageStarted(
                                            popupView: WebView?,
                                            url: String?,
                                            favicon: Bitmap?
                                        ) {
                                            if (url.isNullOrBlank()) return
                                            mainWebView.loadUrl(url)
                                            popupView?.stopLoading()
                                            popupView?.destroy()
                                        }
                                    }
                                }

                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                    ?: return false
                                transport.webView = popupWebView
                                resultMsg.sendToTarget()
                                return true
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                            val filename = sanitizeFilename(
                                resolveDownloadFilename(url, contentDisposition, mimeType)
                            )
                            Log.i("MWBrowser", "Download: $filename from $url")

                            // Save to Downloads folder via DownloadManager
                            val request = DownloadManager.Request(Uri.parse(url)).apply {
                                setMimeType(mimeType)
                                addRequestHeader("User-Agent", userAgent)
                                val cookies = CookieManager.getInstance().getCookie(url)
                                if (!cookies.isNullOrBlank()) {
                                    addRequestHeader("Cookie", cookies)
                                }
                                setTitle(filename)
                                setDescription("Downloading from MakerWorld")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            }
                            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)

                            Toast.makeText(ctx, "Downloading $filename…", Toast.LENGTH_SHORT).show()

                            // Also download to cache and load into slicer
                            val is3mf = filename.endsWith(".3mf", ignoreCase = true)
                            val isStl = filename.endsWith(".stl", ignoreCase = true)
                            if (is3mf || isStl) {
                                viewModel.downloadAndLoadModel(url, filename, userAgent)
                                onModelDownloaded()
                            }
                        }

                        // Preserve existing cookies so users stay logged in between sessions
                        loadUrl(START_URL)
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
