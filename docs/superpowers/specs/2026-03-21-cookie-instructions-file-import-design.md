# Cookie Instructions & File Import

**Date:** 2026-03-21
**Status:** Approved

## Problem

Users need MakerWorld cookies to download login-gated models, but the current settings UI provides no guidance on how to obtain them. Additionally, transferring a long cookie string from a PC browser to an Android phone is cumbersome — manual copy-paste across devices is error-prone.

## Solution

Two additions to the MakerWorld section of SettingsScreen:

1. **Info dialog** — triggered by an info icon button next to the cookie toggle. Contains step-by-step instructions for extracting cookies from browser dev tools and transferring them via text file.
2. **"Import from File" button** — opens Android's file picker (SAF) to read a `.txt` file containing cookies, populating the cookie field automatically.

## Design

### Info Icon Button

- Small outlined icon button with an info icon (`Icons.Outlined.Info`) placed between the label text and the switch in the existing Row (layout: `[Text (weight 1f)] [InfoIcon] [Switch]`)
- On tap, opens an `AlertDialog` with the instruction content

### Instruction Dialog Content

**Section 1 — Get cookies from your browser:**

1. Log in to makerworld.com in your browser
2. Press F12 to open Developer Tools (steps shown for Chrome; other browsers are similar)
3. Go to the Network tab
4. Check "Preserve log"
5. Navigate to any model page
6. Click the "Doc" filter to show page requests only
7. Click the first makerworld.com request
8. In Request Headers, find the Cookie header
9. Right-click the value → Copy value

**Section 2 — Transfer to your phone:**

1. Paste the cookie value into a text file (.txt)
2. Send it to your phone (email, Google Drive, USB, etc.)
3. Tap "Import from File" below

Alternative: paste directly if you have a clipboard sync app.

**Footer note:** Cookies expire periodically. If downloads stop working, repeat these steps to refresh.

### Import from File Button

- `OutlinedButton` placed between "Save Cookies" and "Clear" in the button row
- Uses `ActivityResultContracts.GetContent` with MIME type `text/plain`
- **Size guard:** Before reading, check file size via `contentResolver.openAssetFileDescriptor(uri, "r")?.length`. If > 64 KB, show a Toast ("File too large — cookies should be a few KB") and skip.
- Reads the file content, trims whitespace, and populates `cookieInput`
- **Empty file:** If the trimmed content is blank, show a Toast ("File was empty") and do not populate
- **Read failure:** If `openInputStream` returns null, show a Toast ("Could not read file")
- User still reviews and taps "Save Cookies" to confirm (no auto-save)

### Button Row Layout (when cookies enabled)

```
[ Save Cookies (filled) ] [ Import from File (outlined) ] [ Clear (outlined, if cookies exist) ]
```

All three buttons use `Modifier.weight(1f)` for equal sizing. Button text uses `maxLines = 1, overflow = TextOverflow.Ellipsis` as a safety net on narrow screens.

## Files Changed

| File | Change |
|------|--------|
| `app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt` | Add info icon button, AlertDialog composable, file picker launcher, "Import from File" button |

## Dependencies

None. Uses:
- `androidx.activity.compose.rememberLauncherForActivityResult` (already available via activity-compose dependency)
- `ActivityResultContracts.GetContent` (Android built-in)
- `AlertDialog` from Material3 (already used elsewhere in the app)

## Permissions

None required. SAF file picker does not need `READ_EXTERNAL_STORAGE`.

## Testing

- **Manual:** Verify info dialog opens with correct content, file picker launches and reads `.txt` files, cookie field populates correctly
- **No new unit tests needed** — this is purely UI with no new logic functions (file reading is a one-liner via `contentResolver.openInputStream`)

## Scope Exclusions

- No QR code scanning
- No local network transfer
- No deep links
- No browser extension
- No changes to cookie storage or sanitization logic
