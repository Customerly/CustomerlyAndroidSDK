package io.customerly.androidsdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.*
import io.customerly.androidsdk.models.AttachmentPayload
import io.customerly.androidsdk.models.HelpCenterArticle
import io.customerly.androidsdk.models.RealtimeCall
import io.customerly.androidsdk.models.Survey
import org.json.JSONObject

@SuppressLint("StaticFieldLeak")
object Customerly {
    private var appId: String? = null
    private var initializedWebView: WebView? = null
    private var jsBridge: JSBridge? = null
    private var context: Context? = null
    private var notificationsHelper: NotificationsHelper? = null

    fun load(context: Context, appId: String) {
        this.appId = appId
        this.context = context
        this.notificationsHelper = NotificationsHelper(context)
        preloadWebView()
    }

    fun setContext(context: Context) {
        this.initializedWebView?.destroy()

        this.context = context
        this.notificationsHelper = NotificationsHelper(context)
        preloadWebView()
    }

    fun requestNotificationPermissionIfNeeded() {
        if (context == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        this.notificationsHelper?.requestNotificationPermissionIfNeeded(context!!)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun preloadWebView() {
        if (initializedWebView != null || context == null) {
            return
        }

        val webView = WebView(context!!)
        jsBridge = JSBridge { message, notificationId, conversationId ->
            this.notificationsHelper?.showNotification(
                context!!, message, notificationId, conversationId
            )
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false

        WebView.setWebContentsDebuggingEnabled(true)

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("CustomerlySDK", "WebView finished loading")
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e("CustomerlySDK", "WebView error: ${error?.description}")
            }
        }

        webView.addJavascriptInterface(jsBridge!!, "CustomerlyNative")

        val html = """
            <!DOCTYPE html>
            <html>
              <head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
              <body>
                <script>
                  !function(){var e=window,i=document,t="customerly",n="queue",o="load",r="settings",u=e[t]=e[t]||[];if(u.t){return void u.i("[customerly] SDK already initialized. Snippet included twice.")}u.t=!0;u.loaded=!1;u.o=["event","attribute","update","show","hide","open","close"];u[n]=[];u.i=function(t){e.console&&!u.debug&&console.error&&console.error(t)};u.u=function(e){return function(){var t=Array.prototype.slice.call(arguments);return t.unshift(e),u[n].push(t),u}};u[o]=function(t){u[r]=t||{};if(u.loaded){return void u.i("[customerly] SDK already loaded. Use `customerly.update` to change settings.")}u.loaded=!0;var e=i.createElement("script");e.type="text/javascript",e.async=!0,e.src="https://messenger.customerly.io/launcher.js";var n=i.getElementsByTagName("script")[0];n.parentNode.insertBefore(e,n)};u.o.forEach(function(t){u[t]=u.u(t)})}();
      
                  customerly.load({
                    "app_id": "$appId",
                    "sdkMode": true
                  });
                  
                  // Register callbacks
                  customerly.onChatClosed = function() {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onChatClosed"}));
                  };
                  
                  customerly.onChatOpened = function() {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onChatOpened"}));
                  };
                  
                  customerly.onHelpCenterArticleOpened = function(article) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onHelpCenterArticleOpened",
                      data: article
                    }));
                  };
                  
                  customerly.onLeadGenerated = function(email) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onLeadGenerated",
                      data: {email: email}
                    }));
                  };
                  
                  customerly.onNewConversation = function(message, attachments) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onNewConversation",
                      data: {message: message, attachments: attachments}
                    }));
                  };
                  
                  customerly.onNewMessageReceived = function(message) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onNewMessageReceived",
                      data: message
                    }));
                  };
                  
                  customerly.onNewConversationReceived = function(conversationId) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onNewConversationReceived",
                      data: {conversationId: conversationId}
                    }));
                  };
                  
                  customerly.onProfilingQuestionAnswered = function(attribute, value) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onProfilingQuestionAnswered",
                      data: {attribute: attribute, value: value}
                    }));
                  };
                  
                  customerly.onProfilingQuestionAsked = function(attribute) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onProfilingQuestionAsked",
                      data: {attribute: attribute}
                    }));
                  };
                </script>
              </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(
            "https://customerly.io/", html, "text/html", "utf-8", null
        )

        initializedWebView = webView
    }

    fun getWebView(): WebView? = initializedWebView

    private fun evaluateJavascript(script: String) {
        if (initializedWebView == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        initializedWebView?.post {
            initializedWebView?.evaluateJavascript(script, null)
        }
    }

    fun show() {
        if (initializedWebView == null || context == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        evaluateJavascript("customerly.open()")
        evaluateJavascript("_customerly_sdk.navigate('/', true)")

        val intent = Intent(context, WidgetActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context!!.startActivity(intent)
    }

    fun hide() {
        if (initializedWebView == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        val context = initializedWebView?.context
        if (context != null) {
            val intent = Intent(context, WidgetActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(WidgetActivity.ExtraKey.ACTION.name, WidgetActivity.Action.HIDE.name)
            context.startActivity(intent)
        }
    }

    fun logout() {
        evaluateJavascript("customerly.logout()")
    }

    fun event(name: String) {
        evaluateJavascript("customerly.event('$name')")
    }

    fun attribute(name: String, value: Any) {
        val valueJson = when (value) {
            is String -> "'$value'"
            is Number, is Boolean -> value.toString()
            else -> JSONObject().put("value", value).toString()
        }

        evaluateJavascript("customerly.attribute('$name', $valueJson)")
    }

    fun update(settings: Map<String, Any>) {
        val settingsJson = JSONObject(settings).toString()
        evaluateJavascript("customerly.update($settingsJson)")
    }

    fun showNewMessage(message: String) {
        show()
        evaluateJavascript("customerly.showNewMessage('$message')")
    }

    fun sendNewMessage(message: String) {
        show()
        evaluateJavascript("customerly.sendNewMessage('$message')")
    }

    fun showArticle(collectionSlug: String, articleSlug: String) {
        show()
        evaluateJavascript("customerly.showArticle('$collectionSlug', '$articleSlug')")
    }

    fun registerLead(email: String, attributes: Map<String, String>? = null) {
        val attributesJson = attributes?.let { JSONObject(it).toString() } ?: "null"
        evaluateJavascript("customerly.registerLead('$email', $attributesJson)")
    }

    fun back() {
        evaluateJavascript("_customerly_sdk.back()")
    }

    fun navigateToConversation(conversationId: Int) {
        evaluateJavascript("_customerly_sdk.navigateToConversation($conversationId)")
    }

    // Callback registration methods
    private fun registerCallback(type: String, callback: CustomerlyCallback) {
        if (jsBridge == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        jsBridge?.setCallback(type, callback)
    }

    fun setOnChatClosed(callback: () -> Unit) {
        registerCallback("onChatClosed", object : CustomerlyCallback {
            override fun onChatClosed() {
                hide()
                callback()
            }
        })
    }

    fun setOnChatOpened(callback: () -> Unit) {
        registerCallback("onChatOpened", object : CustomerlyCallback {
            override fun onChatOpened() = callback()
        })
    }

    fun setOnHelpCenterArticleOpened(callback: (HelpCenterArticle) -> Unit) {
        registerCallback("onHelpCenterArticleOpened", object : CustomerlyCallback {
            override fun onHelpCenterArticleOpened(article: HelpCenterArticle) = callback(article)
        })
    }

    fun setOnLeadGenerated(callback: (String?) -> Unit) {
        registerCallback("onLeadGenerated", object : CustomerlyCallback {
            override fun onLeadGenerated(email: String?) = callback(email)
        })
    }

    fun setOnNewConversation(callback: (String, List<AttachmentPayload>) -> Unit) {
        registerCallback("onNewConversation", object : CustomerlyCallback {
            override fun onNewConversation(message: String, attachments: List<AttachmentPayload>) =
                callback(message, attachments)
        })
    }

    fun setOnNewMessageReceived(callback: (Int, String, Long, Int, Int) -> Unit) {
        registerCallback("onNewMessageReceived", object : CustomerlyCallback {
            override fun onNewMessageReceived(
                accountId: Int, message: String, timestamp: Long, userId: Int, conversationId: Int
            ) = callback(accountId, message, timestamp, userId, conversationId)
        })
    }

    fun setOnNewConversationReceived(callback: (Int) -> Unit) {
        registerCallback("onNewConversationReceived", object : CustomerlyCallback {
            override fun onNewConversationReceived(conversationId: Int) = callback(conversationId)
        })
    }

    fun setOnProfilingQuestionAnswered(callback: (String, String) -> Unit) {
        registerCallback("onProfilingQuestionAnswered", object : CustomerlyCallback {
            override fun onProfilingQuestionAnswered(attribute: String, value: String) =
                callback(attribute, value)
        })
    }

    fun setOnProfilingQuestionAsked(callback: (String) -> Unit) {
        registerCallback("onProfilingQuestionAsked", object : CustomerlyCallback {
            override fun onProfilingQuestionAsked(attribute: String) = callback(attribute)
        })
    }

    fun setOnRealtimeVideoAnswered(callback: (RealtimeCall) -> Unit) {
        registerCallback("onRealtimeVideoAnswered", object : CustomerlyCallback {
            override fun onRealtimeVideoAnswered(call: RealtimeCall) = callback(call)
        })
    }

    fun setOnRealtimeVideoCanceled(callback: () -> Unit) {
        registerCallback("onRealtimeVideoCanceled", object : CustomerlyCallback {
            override fun onRealtimeVideoCanceled() = callback()
        })
    }

    fun setOnRealtimeVideoReceived(callback: (RealtimeCall) -> Unit) {
        registerCallback("onRealtimeVideoReceived", object : CustomerlyCallback {
            override fun onRealtimeVideoReceived(call: RealtimeCall) = callback(call)
        })
    }

    fun setOnRealtimeVideoRejected(callback: () -> Unit) {
        registerCallback("onRealtimeVideoRejected", object : CustomerlyCallback {
            override fun onRealtimeVideoRejected() = callback()
        })
    }

    fun setOnSurveyAnswered(callback: () -> Unit) {
        registerCallback("onSurveyAnswered", object : CustomerlyCallback {
            override fun onSurveyAnswered() = callback()
        })
    }

    fun setOnSurveyPresented(callback: (Survey) -> Unit) {
        registerCallback("onSurveyPresented", object : CustomerlyCallback {
            override fun onSurveyPresented(survey: Survey) = callback(survey)
        })
    }

    fun setOnSurveyRejected(callback: () -> Unit) {
        registerCallback("onSurveyRejected", object : CustomerlyCallback {
            override fun onSurveyRejected() = callback()
        })
    }
}
