# Cookie Instructions & File Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an info dialog with cookie extraction instructions and a file import button to the MakerWorld settings section.

**Architecture:** Single-file change to `SettingsScreen.kt`. Adds a composable info dialog, an info icon button in the toggle row, and a SAF file picker launcher for `.txt` import. Follows existing patterns already in the file (backup import launcher at lines 372-386).

**Tech Stack:** Kotlin, Jetpack Compose, Material3, ActivityResultContracts (SAF)

**Spec:** `docs/superpowers/specs/2026-03-21-cookie-instructions-file-import-design.md`

---

### Task 1: Add Cookie Info Dialog Composable

**Files:**
- Modify: `app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt`

- [ ] **Step 1: Add the `CookieInfoDialog` composable**

Add this private composable at the bottom of the file (before the closing of the file, after the last existing composable):

```kotlin
@Composable
private fun CookieInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        title = { Text("How to get your MakerWorld cookies") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Get cookies from your browser:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                val browserSteps = listOf(
                    "Log in to makerworld.com in your browser",
                    "Press F12 to open Developer Tools\n(steps shown for Chrome; other browsers are similar)",
                    "Go to the Network tab",
                    "Check \"Preserve log\"",
                    "Navigate to any model page",
                    "Click the \"Doc\" filter to show page requests only",
                    "Click the first makerworld.com request",
                    "In Request Headers, find the Cookie header",
                    "Right-click the value → Copy value"
                )
                browserSteps.forEachIndexed { i, step ->
                    Text(
                        "${i + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Transfer to your phone:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                val transferSteps = listOf(
                    "Paste the cookie value into a text file (.txt)",
                    "Send it to your phone (email, Google Drive, USB, etc.)",
                    "Tap \"Import from File\" below"
                )
                transferSteps.forEachIndexed { i, step ->
                    Text(
                        "${i + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }
                Text(
                    "Or paste directly if you have a clipboard sync app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Cookies expire periodically. If downloads stop working, repeat these steps to refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    )
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt
git commit -m "feat: add CookieInfoDialog composable with browser extraction instructions"
```

---

### Task 2: Add Info Icon Button to Cookie Toggle Row

**Files:**
- Modify: `app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt:224-244`

- [ ] **Step 1: Add the `showCookieInfo` state and icon import**

Add the state variable after line 228 (after `val hasCookies = ...`):

```kotlin
var showCookieInfo by remember { mutableStateOf(false) }
```

Add this import at the top of the file (with the other icon imports):

```kotlin
import androidx.compose.material.icons.outlined.Info
```

- [ ] **Step 2: Modify the toggle Row to include the info icon**

Replace the existing Row (lines 231-244) with:

```kotlin
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
```

- [ ] **Step 3: Add the dialog invocation**

Add this right after the `showCookieInfo` state variable:

```kotlin
if (showCookieInfo) {
    CookieInfoDialog(onDismiss = { showCookieInfo = false })
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt
git commit -m "feat: add info icon button next to cookie toggle for instructions dialog"
```

---

### Task 3: Add File Import Launcher and Button

**Files:**
- Modify: `app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt:224-283`

- [ ] **Step 1: Add imports**

Add at the top of the file:

```kotlin
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow
```

- [ ] **Step 2: Add the file picker launcher**

First, move the existing `val context = LocalContext.current` from line 356 up to before line 224 (before the MakerWorld section), so it can be reused by both the cookie file launcher and the backup section. Remove the duplicate at line 356.

Then add this after the `showCookieInfo` dialog block, at the same scope level as the existing state variables (between `val hasCookies` and `SettingsSection("MakerWorld")`):

```kotlin
val cookieFileLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
) { uri: Uri? ->
    if (uri != null) {
        val size = context.contentResolver.openAssetFileDescriptor(uri, "r")
            ?.use { it.length } ?: 0L
        if (size > 65_536L) {
            Toast.makeText(context, "File too large — cookies should be a few KB", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText()?.trim()
        if (text == null) {
            Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
        } else if (text.isBlank()) {
            Toast.makeText(context, "File was empty", Toast.LENGTH_SHORT).show()
        } else {
            cookieInput = text
        }
    }
}
```

- [ ] **Step 3: Add the "Import from File" button to the button row**

Replace the existing button Row (lines 262-281) with:

```kotlin
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
                        ) { Text("Import File", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        if (hasCookies) {
                            OutlinedButton(
                                onClick = { viewModel.saveMakerWorldCookies("") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Clear", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt
git commit -m "feat: add Import from File button with SAF picker and size/empty guards"
```

---

### Task 4: Build and Manual Test

**Files:**
- None (verification only)

- [ ] **Step 1: Build and install**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL, installed on device

- [ ] **Step 2: Run existing unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: 464 tests PASS — no regressions

- [ ] **Step 3: Manual verification checklist**

On device, navigate to Settings > MakerWorld:
1. Info icon (i) is visible next to the toggle
2. Tapping info icon opens dialog with complete instructions
3. Dialog scrolls if content overflows
4. Dialog dismisses on "Got it" button
5. "Import File" button appears in the button row
6. Tapping "Import File" opens the system file picker
7. Selecting a .txt file with cookie content populates the input field
8. User can then tap "Save Cookies" to persist
9. Empty file shows "File was empty" toast
10. Large file (>64KB) shows size warning toast
