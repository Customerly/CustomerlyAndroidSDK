package io.customerly.androidsdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout

class MessengerActivity : AppCompatActivity() {
    enum class ExtraKey {
        CONVERSATION_ID, ACTION
    }

    enum class Action {
        HIDE
    }

    companion object {
        private var isRunning = false
        private var currentInstance: MessengerActivity? = null

        fun isActivityRunning(): Boolean = isRunning
        fun getCurrentInstance(): MessengerActivity? = currentInstance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isRunning = true
        currentInstance = this

        if (intent.getStringExtra(ExtraKey.ACTION.name) == Action.HIDE.name) {
            finish()
            return
        }

        // Check if we have a message from the notification
        val conversationId = intent.getIntExtra(ExtraKey.CONVERSATION_ID.name, -1)
        if (conversationId != -1) {
            Customerly.navigateToConversation(conversationId)
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
        } else {
            Log.e("CustomerlySDK", "WebView was null in MessengerActivity")
            finish()
            return
        }

        setContentView(rootLayout)
    }

    override fun onBackPressed() {
        Customerly.back()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        currentInstance = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Customerly.handleActivityResult(requestCode, resultCode, data)
    }
}
