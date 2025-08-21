package io.customerly.androidsdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import io.customerly.androidsdk.models.AttachmentPayload
import io.customerly.androidsdk.models.CustomerlySettings
import io.customerly.androidsdk.models.HelpCenterArticle
import io.customerly.androidsdk.models.RealtimeCall
import io.customerly.androidsdk.models.Survey
import io.customerly.androidsdk.models.UnreadMessage
import org.json.JSONObject
import androidx.core.net.toUri

@SuppressLint("StaticFieldLeak")
object Customerly {
    private var settings: CustomerlySettings? = null
    private var initializedWebView: WebView? = null
    private var jsBridge: JSBridge? = null
    private var context: Context? = null
    private var notificationsHelper: NotificationsHelper? = null

    private const val FILE_CHOOSER_RESULT_CODE = 10001
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    fun load(context: Context, settings: CustomerlySettings) {
        this.settings = settings
        this.context = context
        this.notificationsHelper = NotificationsHelper(context)
        registerLifecycleCallback(context)
        preloadWebView()
    }

    private fun registerLifecycleCallback(context: Context) {
        val applicationContext = context as? Application ?: context.applicationContext
        if (applicationContext != null && applicationContext is Application) {
            var lifecycleCallback = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (activity.isFinishing) {
                        saveCookies()
                    }
                }
            }

            applicationContext.registerActivityLifecycleCallbacks(lifecycleCallback)
        }
    }

    fun setContext(context: Context) {
        saveCookies()
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

    private fun serializeSettings(settings: CustomerlySettings): String {
        val settingsMap = buildMap<String, Any> {
            put("app_id", settings.app_id)
            put("sdkMode", true)
            put("showBackInsteadOfClose", true)
            settings.accentColor?.let { put("accentColor", it) }
            settings.contrastColor?.let { put("contrastColor", it) }
            settings.attachmentsAvailable?.let { put("attachmentsAvailable", it) }
            settings.singleConversation?.let { put("singleConversation", it) }
            settings.user_id?.let { put("user_id", it) }
            settings.name?.let { put("name", it) }
            settings.email?.let { put("email", it) }
            settings.email_hash?.let { put("email_hash", it) }
            settings.events?.let {
                put("events", it.map { event ->
                    mapOf(
                        "name" to event.name, "date" to (event.date ?: JSONObject.NULL)
                    )
                })
            }
            settings.last_page_viewed?.let { put("last_page_viewed", it) }
            settings.force_lead?.let { put("force_lead", it) }
            settings.attributes?.let { put("attributes", it) }
            settings.company?.let { company ->
                put(
                    "company", mapOf(
                        "company_id" to company.company_id, "name" to company.name
                    ).plus(company.additionalAttributes)
                )
            }
        }
        return JSONObject(settingsMap).toString()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun preloadWebView() {
        if (initializedWebView != null || context == null || settings == null) {
            return
        }

        val webView = WebView(context!!)
        jsBridge = JSBridge { title, body, notificationId, conversationId ->
            this.notificationsHelper?.showNotification(
                context!!, title, body, notificationId, conversationId
            )
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
        }

        WebView.setWebContentsDebuggingEnabled(true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathPickerCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (context == null) {
                    return false
                }

                val intent = fileChooserParams?.createIntent()
                try {
                    val messengerActivity = MessengerActivity.getCurrentInstance()
                    if (messengerActivity != null) {
                        messengerActivity.startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
                        filePathPickerCallback?.let { callback ->
                            filePathCallback = callback
                        }
                        return true
                    } else {
                        Log.e("CustomerlySDK", "MessengerActivity not available")
                    }
                } catch (e: Exception) {
                    Log.e("CustomerlySDK", "Error launching file chooser", e)
                }
                return false
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e("CustomerlySDK", "WebView error: ${error?.description}")
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString()
                if (url != null) {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context?.startActivity(intent)
                    return true
                }
                return false
            }
        }

        webView.addJavascriptInterface(jsBridge!!, "CustomerlyNative")

        val settingsJson = serializeSettings(settings!!)

        val html = """
            <!DOCTYPE html>
            <html>
              <head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
              <body>
                <script>
                  !function(){var e=window,i=document,t="customerly",n="queue",o="load",r="settings",u=e[t]=e[t]||[];if(u.t){return void u.i("[customerly] SDK already initialized. Snippet included twice.")}u.t=!0;u.loaded=!1;u.o=["event","attribute","update","show","hide","open","close"];u[n]=[];u.i=function(t){e.console&&!u.debug&&console.error&&console.error(t)};u.u=function(e){return function(){var t=Array.prototype.slice.call(arguments);return t.unshift(e),u[n].push(t),u}};u[o]=function(t){u[r]=t||{};if(u.loaded){return void u.i("[customerly] SDK already loaded. Use `customerly.update` to change settings.")}u.loaded=!0;var e=i.createElement("script");e.type="text/javascript",e.async=!0,e.src="https://messenger.customerly.io/launcher.js";var n=i.getElementsByTagName("script")[0];n.parentNode.insertBefore(e,n)};u.o.forEach(function(t){u[t]=u.u(t)})}();
                  
                  // Register callbacks
                  customerly.onMessengerInitialized = function() {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onMessengerInitialized"}));
                  };
                  
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
                  
                  customerly.onMessageRead = function(conversationId, conversationMessageId) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onMessageRead",
                      data: {conversationId: conversationId, conversationMessageId: conversationMessageId}
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

                  customerly.onRealtimeVideoAnswered = function(call) {
                    CustomerlyNative.postMessage(JSON.stringify({
                      type: "onRealtimeVideoAnswered",
                      data: call
                    }));
                  };
                  
                  customerly.onRealtimeVideoCanceled = function() {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onRealtimeVideoCanceled"}));
                  };
                  
                  customerly.onRealtimeVideoReceived = function(call) {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onRealtimeVideoReceived", data: call}));
                  };

                  customerly.onRealtimeVideoRejected = function() {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onRealtimeVideoRejected"}));
                  };
                  
                  customerly.onSurveyAnswered = function() {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onSurveyAnswered"}));
                  };
                  
                  customerly.onSurveyPresented = function(survey) {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onSurveyPresented", data: survey}));
                  };

                  customerly.onSurveyRejected = function() {
                    CustomerlyNative.postMessage(JSON.stringify({type: "onSurveyRejected"}));
                  };
      
                  // Load Customerly Messenger
                  customerly.load($settingsJson);
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

    private fun saveCookies() {
        if (initializedWebView == null || context == null) {
            return
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.flush()
    }

    private fun evaluateJavascript(
        script: String, safe: Boolean = false, resultCallback: ValueCallback<String>? = null
    ) {
        if (initializedWebView == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        if (safe) {
            initializedWebView?.post {
                initializedWebView?.evaluateJavascript(script, resultCallback)
            }
        } else {
            initializedWebView?.evaluateJavascript(script, resultCallback)
        }
    }

    fun show(withoutNavigation: Boolean = false, safe: Boolean = false) {
        if (initializedWebView == null || context == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        evaluateJavascript("customerly.open()", safe)
        if (!withoutNavigation) {
            evaluateJavascript("_customerly_sdk.navigate('/', true)", safe)
        }

        if (!MessengerActivity.isActivityRunning()) {
            val intent = Intent(context, MessengerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context!!.startActivity(intent)
        }
    }

    fun hide() {
        if (initializedWebView == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        val context = initializedWebView?.context
        if (context != null) {
            val intent = Intent(context, MessengerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(
                MessengerActivity.ExtraKey.ACTION.name, MessengerActivity.Action.HIDE.name
            )
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

    fun update(settings: CustomerlySettings) {
        val settingsJson = serializeSettings(settings)
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

    fun getUnreadMessagesCount(resultCallback: ValueCallback<Int>) {
        return evaluateJavascript("customerly.unreadMessagesCount", resultCallback = { value ->
            resultCallback.onReceiveValue(value?.toIntOrNull() ?: 0)
        })
    }

    fun getUnreadConversationsCount(resultCallback: ValueCallback<Int>) {
        return evaluateJavascript("customerly.unreadConversationsCount", resultCallback = { value ->
            resultCallback.onReceiveValue(value?.toIntOrNull() ?: 0)
        })
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
            override fun onChatClosed() = callback()
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

    fun setOnMessageRead(callback: (Int, Int) -> Unit) {
        registerCallback("onMessageRead", object : CustomerlyCallback {
            override fun onMessageRead(
                conversationId: Int, conversationMessageId: Int
            ) = callback(conversationId, conversationMessageId)
        })
    }

    fun setOnMessengerInitialized(callback: () -> Unit) {
        registerCallback("onMessengerInitialized", object : CustomerlyCallback {
            override fun onMessengerInitialized() = callback()
        })
    }

    fun setOnNewConversation(callback: (String, List<AttachmentPayload>) -> Unit) {
        registerCallback("onNewConversation", object : CustomerlyCallback {
            override fun onNewConversation(message: String, attachments: List<AttachmentPayload>) =
                callback(message, attachments)
        })
    }

    fun setOnNewMessageReceived(callback: (UnreadMessage) -> Unit) {
        registerCallback("onNewMessageReceived", object : CustomerlyCallback {
            override fun onNewMessageReceived(unreadMessage: UnreadMessage) = callback(unreadMessage)
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

    // Remove callback methods
    private fun removeCallback(type: String) {
        if (jsBridge == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        jsBridge?.removeCallback(type)
    }

    fun removeOnChatClosed() = removeCallback("onChatClosed")
    fun removeOnChatOpened() = removeCallback("onChatOpened")
    fun removeOnHelpCenterArticleOpened() = removeCallback("onHelpCenterArticleOpened")
    fun removeOnLeadGenerated() = removeCallback("onLeadGenerated")
    fun removeOnMessageRead() = removeCallback("onMessageRead")
    fun removeOnMessengerInitialized() = removeCallback("onMessengerInitialized")
    fun removeOnNewConversation() = removeCallback("onNewConversation")
    fun removeOnNewMessageReceived() = removeCallback("onNewMessageReceived")
    fun removeOnNewConversationReceived() = removeCallback("onNewConversationReceived")
    fun removeOnProfilingQuestionAnswered() = removeCallback("onProfilingQuestionAnswered")
    fun removeOnProfilingQuestionAsked() = removeCallback("onProfilingQuestionAsked")
    fun removeOnRealtimeVideoAnswered() = removeCallback("onRealtimeVideoAnswered")
    fun removeOnRealtimeVideoCanceled() = removeCallback("onRealtimeVideoCanceled")
    fun removeOnRealtimeVideoReceived() = removeCallback("onRealtimeVideoReceived")
    fun removeOnRealtimeVideoRejected() = removeCallback("onRealtimeVideoRejected")
    fun removeOnSurveyAnswered() = removeCallback("onSurveyAnswered")
    fun removeOnSurveyPresented() = removeCallback("onSurveyPresented")
    fun removeOnSurveyRejected() = removeCallback("onSurveyRejected")

    fun removeAllCallbacks() {
        if (jsBridge == null) {
            Log.e("CustomerlySDK", "Customerly is not initialized. Call load() first.")
            return
        }

        jsBridge?.removeAllCallbacks()
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (filePathCallback == null) {
                return
            }

            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val dataString = data.dataString
                    if (dataString != null) {
                        results = arrayOf(dataString.toUri())
                    } else {
                        val clipData = data.clipData
                        if (clipData != null) {
                            results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                        }
                    }
                }
            }

            filePathCallback!!.onReceiveValue(results)
            filePathCallback = null
        }
    }
}
