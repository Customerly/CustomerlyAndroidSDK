# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **Customerly Android SDK** — a thin native wrapper published to Maven Central as `io.customerly:customerlyandroidsdk`. The entire messenger UI (live chat, help center, surveys, realtime video, lead gen) is the Customerly **web widget** running inside a WebView. The Kotlin code is a bridge: it hosts the WebView, translates native API calls into JavaScript, and translates JS events back into native callbacks and Android notifications. There is almost no native UI.

Public API and usage examples are documented in `README.md` — consult it for the full surface of `Customerly.*` methods and callbacks rather than re-deriving them.

## Modules

- **`app/`** — the SDK library (`com.android.library`, namespace `io.customerly.androidsdk`). This is the published artifact.
- **`sampleapp/`** — a demo app (`com.android.application`) that consumes the SDK and exercises every feature. Set your `app_id` in `sampleapp/.../MainActivity.kt` to run it.

## Build & test

```bash
./gradlew build                          # build all modules
./gradlew :app:assembleRelease           # build just the SDK
./gradlew :sampleapp:installDebug        # build + install the sample app
./gradlew :app:testDebugUnitTest         # JVM unit tests (app module)
./gradlew :app:connectedAndroidTest      # instrumented tests (needs device/emulator)
./gradlew publishAndReleaseToMavenCentral   # publish (CI only; needs signing secrets)
```

Toolchain: AGP 8.10.1, Kotlin 1.8.0, `compileSdk`/`targetSdk` 34, `minSdk` 24, Java 8 bytecode. The test files under `app/src/{test,androidTest}` are still the AndroidStudio-generated stubs — there is effectively no test coverage yet.

## Architecture

`Customerly` (`Customerly.kt`) is a **singleton `object`** and the single entry point. Flow:

1. **`load()`** stores settings + a `Context` statically, registers an `ActivityLifecycleCallbacks` (to flush cookies), and calls **`preloadWebView()`**.
2. `preloadWebView()` builds a **headless `WebView`** held on the singleton (not attached to any view yet). It loads an inline HTML string that (a) injects the standard Customerly JS launcher snippet, (b) registers every `customerly.onX` handler to forward events via `CustomerlyNative.postMessage(...)`, and (c) calls `customerly.load(<serialized settings>)`. Base URL is `https://customerly.io/`; the widget itself is fetched from `messenger.customerly.io/launcher.js`.
3. **`MessengerActivity`** is a bare activity that **re-parents the singleton's WebView** into a full-screen `FrameLayout` (removing it from its previous parent first). Showing/hiding the messenger = starting/finishing this activity; the WebView instance and its JS state persist across show/hide.

### The two-way bridge

- **Native → JS:** every action method (`show`, `hide`, `event`, `attribute`, `logout`, `showArticle`, `registerLead`, `navigateToConversation`, …) is implemented via `evaluateJavascript("customerly.xxx(...)")`. Some use an internal `_customerly_sdk.*` JS API (`back`, `navigateToConversation`).
- **JS → Native:** `JSBridge.kt` is the `@JavascriptInterface` exposed as `CustomerlyNative`. Its `postMessage(json)` dispatches on a `type` field to the registered `CustomerlyCallback`s. Manual `JSONObject` → data-class deserializers live at the bottom of `JSBridge.kt`.
- Some JS events drive **native side effects** beyond firing the user callback: `onChatClosed` → `Customerly.hide()`, `onRealtimeVideoReceived` / `onSurveyPresented` → `Customerly.show(...)`, `onNewMessageReceived` → posts a system notification via `NotificationsHelper`.

### Settings & notifications

- `serializeSettings()` hand-builds the settings JSON (see `CustomerlySettings.kt`). It always forces `sdkMode: true` and `showBackInsteadOfClose: true`, and appends a `device` block (OS, app name/version, model) read from `Build` / `PackageManager`.
- `NotificationsHelper.kt` owns the `customerly_messages` notification channel. Notification taps launch `MessengerActivity` with a `CONVERSATION_ID` extra, which deep-links via `navigateToConversation`. Notification bodies are HTML-stripped and truncated by `abstractify()`.

## Things to be aware of

- **Versioning is triple-sourced and CI-enforced.** The git tag, `def libraryVersion` in `app/build.gradle`, and the `implementation 'io.customerly:customerlyandroidsdk:X.Y.Z'` line in `README.md` must all match. The `Release` workflow (`.github/workflows/`) runs on a `*.*.*` tag push, fails and auto-bumps the two files if they disagree, then requires a manual re-run. When changing the version, update **all three**.
- **JS is built by string interpolation.** Method args (event names, messages, attribute values, emails) are spliced directly into `evaluateJavascript` strings with single quotes. A value containing a quote/newline breaks the call — keep this in mind when adding methods, and prefer JSON-encoding for anything user-supplied.
- **Statically-held `Context` and WebView.** `@SuppressLint("StaticFieldLeak")` is intentional (singleton lifetime = app lifetime). `setContext()` destroys and rebuilds the WebView when the host swaps context.
- **File attachments** rely on the host activity forwarding results — `MessengerActivity` calls `Customerly.handleActivityResult(...)` in `onActivityResult`, which feeds the WebView's `filePathCallback`. The `MANAGE_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (maxSdk 28) permissions in the manifest exist for this.
- `WebView.setWebContentsDebuggingEnabled(true)` is currently **always on**, including release builds.
- Model classes use snake_case field names on purpose — they mirror the widget's JSON payloads to keep the manual (de)serializers straightforward.
