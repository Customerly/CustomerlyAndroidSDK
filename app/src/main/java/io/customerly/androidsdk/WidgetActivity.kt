package io.customerly.androidsdk

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout

class WidgetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getStringExtra("action") == "hide") {
            finish()
            return
        }

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val webView = Customerly.getWebView()
        if (webView != null) {
            // Remove from previous parent to avoid crash
            (webView.parent as? ViewGroup)?.removeView(webView)

            rootLayout.addView(
                webView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            Log.d("CustomerlySDK", "WebView attached to WidgetActivity")
        } else {
            Log.e("CustomerlySDK", "WebView was null in WidgetActivity")
            finish()
            return
        }

        setContentView(rootLayout)
    }
}
