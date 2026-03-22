# WebView MakerWorld Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an in-app WebView login screen so users can log in to MakerWorld directly and have cookies extracted automatically.

**Architecture:** New `MakerWorldLoginScreen` composable with an Android WebView that handles the Bambu Lab SSO flow across `makerworld.com` and `bambulab.com`. After successful login redirect, cookies are extracted via `CookieManager` and saved. Settings UI gets a login button as the primary action, with paste/import collapsed into an Advanced section.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Android WebView, CookieManager, Jetpack Navigation

**Spec:** `docs/superpowers/specs/2026-03-22-webview-makerworld-login-design.md`

---

### Task 1: Create MakerWorldLoginScreen

**Files:**
- Create: `app/src/main/java/com/u1/slicer/ui/MakerWorldLoginScreen.kt`

- [ ] **Step 1: Create the screen file with full implementation**

Create the new file with the following content:

```kotlin
package com.u1.slicer.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.u1.slicer.SlicerViewModel

private const val LOGIN_URL = "https://makerworld.com/en/login"
private val LOGIN_PATH_FRAGMENTS = listOf("/login", "/sign-in", "/sign-up", "/oauth", "sso")

private fun isPostLoginUrl(url: String): Boolean {
    if (!url.startsWith("https://makerworld.com")) return false
    return LOGIN_PATH_FRAGMENTS.none { url.contains(it, ignoreCase = true) }
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
    val context = LocalContext.current

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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.fillMaxSize()) {
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
                                    if (url != null && isPostLoginUrl(url)) {
                                        val cookies = CookieManager.getInstance()
                                            .getCookie("https://makerworld.com")
                                        if (!cookies.isNullOrBlank()) {
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
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/u1/slicer/ui/MakerWorldLoginScreen.kt
git commit -m "feat: add MakerWorldLoginScreen with WebView SSO login flow"
```

---

### Task 2: Add Navigation Route

**Files:**
- Modify: `app/src/main/java/com/u1/slicer/navigation/NavGraph.kt:15-24` (add route constant)
- Modify: `app/src/main/java/com/u1/slicer/navigation/NavGraph.kt:39-94` (add composable)

- [ ] **Step 1: Add the route constant**

In the `Routes` object (line 15-24), add:

```kotlin
const val MAKERWORLD_LOGIN = "makerworld_login"
```

- [ ] **Step 2: Add the composable route**

Add a new `composable` block inside `NavHost` (after the `MODEL_VIEWER` composable, before the closing `}`). Also add the import for `MakerWorldLoginScreen`:

```kotlin
import com.u1.slicer.ui.MakerWorldLoginScreen
```

```kotlin
        composable(Routes.MAKERWORLD_LOGIN) {
            MakerWorldLoginScreen(
                viewModel = viewModel,
                onLoginComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/u1/slicer/navigation/NavGraph.kt
git commit -m "feat: add makerworld_login route to NavGraph"
```

---

### Task 3: Update SettingsScreen with Login Button and Advanced Section

**Files:**
- Modify: `app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt:51-60` (add parameter)
- Modify: `app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt:259-328` (MakerWorld section)

- [ ] **Step 1: Add the navigation callback parameter**

Add `onNavigateMakerWorldLogin` to the `SettingsScreen` function signature (line 51-60). Replace the last parameter line and closing paren:

```kotlin
    onNavigateSettings: () -> Unit = {},
    onNavigateMakerWorldLogin: () -> Unit = {}
)
```

Note: `ExpandLess` and `ExpandMore` icons are already imported (lines 32-33). `AnimatedVisibility` is already imported (line 25). `Color` is already imported (line 37/40). No new imports needed for this task.

- [ ] **Step 2: Replace the MakerWorld SettingsSection content**

Replace the entire `SettingsSection("MakerWorld") { ... }` block (lines 259-328) with:

```kotlin
            SettingsSection("MakerWorld") {
                // Primary action: WebView login
                if (hasCookies) {
                    Text(
                        "Logged in to MakerWorld",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedButton(
                        onClick = onNavigateMakerWorldLogin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Log in again") }
                } else {
                    Button(
                        onClick = onNavigateMakerWorldLogin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Log in to MakerWorld") }
                }

                // Advanced: manual cookie entry (collapsed by default)
                var showAdvanced by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Advanced",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Icon(
                        if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showAdvanced) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                AnimatedVisibility(visible = showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Send cookies with URL downloads",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showCookieInfo = true }
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = "Cookie instructions",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = cookiesEnabled,
                                onCheckedChange = { viewModel.saveMakerWorldCookiesEnabled(it) }
                            )
                        }
                        if (cookiesEnabled) {
                            Text(
                                if (hasCookies) "Session cookies configured"
                                else "Add cookies for downloads requiring login",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasCookies) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            OutlinedTextField(
                                value = cookieInput,
                                onValueChange = { cookieInput = it },
                                label = { Text("MakerWorld Cookies") },
                                placeholder = { Text("Paste cookies from browser") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (cookieInput.isNotBlank()) {
                                            viewModel.saveMakerWorldCookies(cookieInput)
                                            cookieInput = ""
                                        }
                                    },
                                    enabled = cookieInput.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Save Cookies", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                OutlinedButton(
                                    onClick = { cookieFileLauncher.launch("text/plain") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Import from File", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                if (hasCookies) {
                                    OutlinedButton(
                                        onClick = { viewModel.saveMakerWorldCookies("") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Clear", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                        }
                    }
                }
            }
```

Note: `Color` is already imported as `androidx.compose.ui.graphics.Color`. If the existing code uses the fully qualified form `androidx.compose.ui.graphics.Color(0xFF4CAF50)`, replace with just `Color(0xFF4CAF50)` since the import exists at line 37.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt
git commit -m "feat: add MakerWorld login button and collapse paste/import into Advanced section"
```

---

### Task 4: Wire Up Navigation in MainActivity

**Files:**
- Modify: `app/src/main/java/com/u1/slicer/MainActivity.kt:407-417` (add callback)

- [ ] **Step 1: Add the navigation callback**

In the `SettingsScreen(...)` call inside `settingsContent` (line 407-417), add the new parameter:

```kotlin
                            onNavigateSettings = { },
                            onNavigateMakerWorldLogin = { navController.navigate(Routes.MAKERWORLD_LOGIN) }
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/u1/slicer/MainActivity.kt
git commit -m "feat: wire up MakerWorld login navigation in MainActivity"
```

---

### Task 5: Build, Test, and Verify

**Files:**
- None (verification only)

- [ ] **Step 1: Run unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: 464 tests PASS — no regressions

- [ ] **Step 2: Build and install**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL, installed on device

- [ ] **Step 3: Manual verification checklist**

On device, navigate to Settings > MakerWorld:
1. "Log in to MakerWorld" button is visible (when no cookies configured)
2. Tapping it navigates to the WebView login screen
3. WebView loads makerworld.com login page → redirects to Bambu Lab SSO
4. Progress indicator shows while pages load
5. Hardware back button navigates within WebView (not out of screen)
6. Top bar back button navigates within WebView history
7. Email/password login completes → cookies extracted → Toast "Logged in to MakerWorld" → auto-navigate back to Settings
8. After login, Settings shows "Logged in to MakerWorld" text and "Log in again" button
9. "Advanced" section is collapsed by default
10. Expanding Advanced shows the cookie toggle, paste field, Import from File, and Clear buttons
11. Verify social login (Google) — may show error from Google (known limitation)
