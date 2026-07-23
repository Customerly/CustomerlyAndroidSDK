# Consumer ProGuard/R8 rules — applied automatically to any app that depends on
# this SDK. These MUST stay in sync with the JS bridge, otherwise a consumer's
# minified release build will rename/strip the bridge and silently break the
# entire messenger at runtime (works in debug, dead in production).

# The Customerly web widget calls window.CustomerlyNative.postMessage(...) by
# name. Keep the @JavascriptInterface methods so R8 cannot rename or remove them.
-keepclassmembers class io.customerly.androidsdk.JSBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Public model types handed to consumer callbacks — keep their names stable so
# the public API surface survives minification.
-keep class io.customerly.androidsdk.models.** { *; }
