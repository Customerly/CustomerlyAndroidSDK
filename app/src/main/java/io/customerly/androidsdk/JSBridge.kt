package io.customerly.androidsdk

import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject

interface CustomerlyCallback {
    fun onChatClosed() {}
    fun onChatOpened() {}
    fun onHelpCenterArticleOpened(article: JSONObject) {}
    fun onLeadGenerated(email: String?) {}
    fun onNewConversation(message: String, attachments: List<JSONObject>) {}
    fun onNewMessageReceived(
        accountId: Int, message: String, timestamp: Long, userId: Int, conversationId: Int
    ) {
    }

    fun onNewConversationReceived(conversationId: Int) {}
    fun onProfilingQuestionAnswered(attribute: String, value: String) {}
    fun onProfilingQuestionAsked(attribute: String) {}
}

class JSBridge(private val showNotification: (String, Int, Int) -> Unit) {
    private val callbacks = mutableMapOf<String, CustomerlyCallback>()

    fun setCallback(type: String, callback: CustomerlyCallback) {
        callbacks[type] = callback
    }

    @Suppress("NAME_SHADOWING")
    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")
            val data = json.optJSONObject("data")

            when (type) {
                "onChatClosed" -> {
                    Customerly.hide()
                    callbacks["onChatClosed"]?.onChatClosed()
                }
                "onChatOpened" -> callbacks["onChatOpened"]?.onChatOpened()
                "onHelpCenterArticleOpened" -> {
                    val article = data ?: JSONObject()
                    callbacks["onHelpCenterArticleOpened"]?.onHelpCenterArticleOpened(article)
                }
                "onLeadGenerated" -> {
                    val email = data?.optString("email")
                    callbacks["onLeadGenerated"]?.onLeadGenerated(email)
                }
                "onNewConversation" -> {
                    val message = data?.getString("message") ?: ""
                    val attachments = data?.optJSONArray("attachments")?.let { array ->
                        List(array.length()) { array.getJSONObject(it) }
                    } ?: emptyList()
                    callbacks["onNewConversation"]?.onNewConversation(message, attachments)
                }
                "onNewMessageReceived" -> {
                    val accountId = data?.getInt("accountId") ?: 0
                    val message = data?.getString("message") ?: ""
                    val timestamp = data?.getLong("timestamp") ?: 0L
                    val userId = data?.getInt("userId") ?: 0
                    val conversationId = data?.getInt("conversationId") ?: 0

                    // Generate notification ID from conversationId and timestamp
                    val notificationId = (conversationId + timestamp).toInt()
                    showNotification(message, notificationId, conversationId)

                    callbacks["onNewMessageReceived"]?.onNewMessageReceived(
                        accountId, message, timestamp, userId, conversationId
                    )
                }
                "onNewConversationReceived" -> {
                    val conversationId = data?.getInt("conversationId") ?: 0
                    callbacks["onNewConversationReceived"]?.onNewConversationReceived(conversationId)
                }
                "onProfilingQuestionAnswered" -> {
                    val attribute = data?.getString("attribute") ?: ""
                    val value = data?.getString("value") ?: ""
                    callbacks["onProfilingQuestionAnswered"]?.onProfilingQuestionAnswered(
                        attribute, value
                    )
                }
                "onProfilingQuestionAsked" -> {
                    val attribute = data?.getString("attribute") ?: ""
                    callbacks["onProfilingQuestionAsked"]?.onProfilingQuestionAsked(attribute)
                }
            }
        } catch (e: Exception) {
            Log.e("CustomerlySDK", "Error processing message: $message", e)
        }
    }
}
