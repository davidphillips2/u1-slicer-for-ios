# WebView MakerWorld Login

**Date:** 2026-03-22
**Status:** Approved

## Problem

Users need MakerWorld cookies to download login-gated models. The current flow requires extracting cookies from PC browser dev tools and transferring them to the phone — tedious even with the file import option. Users should be able to log in directly from the app.

## Solution

A new screen with a WebView that loads the MakerWorld login page. After the user completes the Bambu Lab SSO flow, the app extracts cookies from `CookieManager` and saves them automatically.

## Login Flow

1. User taps "Log in to MakerWorld" button in Settings
2. App navigates to `MakerWorldLoginScreen` (new route: `makerworld_login`)
3. WebView loads `https://makerworld.com/en/login` → redirects to `bambulab.com` SSO
4. User logs in via email/password or social login (Google/Facebook/Apple)
5. SSO redirects back to `makerworld.com`
6. `WebViewClient.onPageFinished()` detects a makerworld.com URL that is NOT a login/sign-in page
7. App calls `CookieManager.getInstance().getCookie("https://makerworld.com")` to extract cookies
8. Cookies saved via `viewModel.saveMakerWorldCookies(cookies)` AND `viewModel.saveMakerWorldCookiesEnabled(true)` (both calls required to auto-enable)
9. Success Toast shown, auto-navigate back to Settings

## WebView Configuration

- **JavaScript enabled** — required for login forms and SSO redirects
- **Third-party cookies enabled** — required for cross-domain Bambu Lab SSO (`CookieManager.setAcceptThirdPartyCookies(webView, true)`)
- **DOM storage enabled** — some login flows use localStorage
- **`WebViewClient`** — override `shouldOverrideUrlLoading` to return `false` for all URLs (keeps all navigation inside the WebView, no external browser). Do not override `onReceivedSslError` — the default cancel behavior is intentional for a login screen.
- **`WebChromeClient`** — override `onCreateWindow` to support social login popups: create a temporary child WebView, set it as the transport on the result message, and display it in a dialog or overlay. If this proves too complex during implementation, scope it as a known limitation and document that social login may require the fallback paste/import flow.
- **Clear WebView cookies before loading** — use the async callback form: `CookieManager.removeAllCookies { webView.loadUrl(...) }` to ensure the clear completes before the first page load.
- **User-agent** — use default WebView user-agent (don't override; some login forms block custom UAs)
- **Back navigation** — intercept hardware back press via `BackHandler`: if `webView.canGoBack()` call `webView.goBack()`, otherwise call `onBack()` to exit the screen. This prevents accidentally leaving the screen mid-SSO-flow.

## Cookie Extraction Logic

In `onPageFinished(url)`:
- Check if URL starts with `https://makerworld.com` AND does NOT contain `/login`, `/sign-in`, `/sign-up`, `/oauth`, or `sso`
- If so, call `CookieManager.getInstance().getCookie("https://makerworld.com")`
- If the cookie string is non-null and non-blank, save it and navigate back
- If cookies are empty after redirect, the user may not have fully completed login — do nothing, let them continue

## MakerWorldLoginScreen

A full-screen composable containing:
- Top bar with title "MakerWorld Login" and a back/close button
- WebView filling the remaining space
- A loading indicator while pages are loading (`onPageStarted` / `onPageFinished`)

The screen receives:
- `viewModel: SlicerViewModel` — to save cookies
- `onLoginComplete: () -> Unit` — callback to navigate back to Settings
- `onBack: () -> Unit` — callback for the back button

## Settings UI Changes

In the MakerWorld section of `SettingsScreen.kt`:
- Add a "Log in to MakerWorld" filled `Button` as the primary action, placed before the toggle row
- When cookies are already configured, show "Logged in to MakerWorld" status text and a "Log in again" `OutlinedButton` (for refreshing expired cookies)
- Keep existing paste field and Import from File as a collapsible "Advanced" section (collapsed by default, state resets on navigation — uses `remember` not `rememberSaveable`) for users whose social login doesn't work in WebView

## Navigation

- Add `Routes.MAKERWORLD_LOGIN = "makerworld_login"` to `NavGraph.kt`
- Add `composable(Routes.MAKERWORLD_LOGIN)` route with the new screen
- `SettingsScreen` gets an `onNavigateMakerWorldLogin: () -> Unit` callback

## Files Changed

| File | Change |
|------|--------|
| New: `app/src/main/java/com/u1/slicer/ui/MakerWorldLoginScreen.kt` | WebView login screen composable |
| Modify: `app/src/main/java/com/u1/slicer/navigation/NavGraph.kt` | Add route and composable for login screen |
| Modify: `app/src/main/java/com/u1/slicer/ui/SettingsScreen.kt` | Add login button, collapse paste/import into Advanced section, add `onNavigateMakerWorldLogin` parameter |
| Modify: `app/src/main/java/com/u1/slicer/MainActivity.kt` | Wire up `onNavigateMakerWorldLogin` navigation callback (coupled with SettingsScreen signature change) |

## Dependencies

None. `android.webkit.WebView` and `android.webkit.CookieManager` are Android framework classes.

## Permissions

None new. The app already has `android.permission.INTERNET`.

## Testing

- **Manual:** Verify email/password login completes and cookies are extracted
- **Manual:** Verify social login (Google) — may or may not work due to Google's WebView restrictions; confirm graceful behavior either way
- **Manual:** Verify back button works during SSO flow
- **Manual:** Verify cookies are cleared between login attempts
- **No new unit tests** — WebView is a platform component that can't be unit tested; the cookie save/enable paths are already tested

## Edge Cases

- **Social login blocked by provider:** User sees the provider's error page in the WebView. They can use the back button and fall back to paste/import.
- **Login timeout or network error:** WebView shows its default error page. User can retry or go back.
- **User navigates away from login flow:** If they browse to a non-login makerworld.com page, cookies are extracted. If they go somewhere unrelated, the back button takes them home.
- **Cookies already exist:** "Log in again" button clears and re-extracts, allowing refresh of expired cookies.

## Scope Exclusions

- No token-based auth or OAuth client registration — we're just capturing browser cookies
- No automatic cookie refresh — user must re-login when cookies expire
- No account management (logout, profile, etc.)
