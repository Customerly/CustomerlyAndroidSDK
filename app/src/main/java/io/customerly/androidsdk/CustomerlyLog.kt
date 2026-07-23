package io.customerly.androidsdk

import android.util.Log

/**
 * Internal logging facade. Error/warning logs stay on so integrators can diagnose
 * problems, but verbose logs — and anything that could contain message content or
 * other PII — are gated behind [BuildConfig.DEBUG] so they never reach logcat in
 * the published (release-built) SDK.
 */
internal object CustomerlyLog {
    private const val TAG = "CustomerlySDK"

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    /** Verbose/diagnostic logging. No-op in release builds of the SDK. */
    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }
}
