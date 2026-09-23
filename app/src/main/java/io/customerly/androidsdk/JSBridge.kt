package io.customerly.androidsdk

import android.webkit.JavascriptInterface
import io.customerly.androidsdk.models.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

interface CustomerlyCallback {
    fun onChatClosed() {}
    fun onChatOpened() {}
    fun onHelpCenterArticleOpened(article: HelpCenterArticle) {}
    fun onLeadGenerated(email: String?) {}
    fun onMessageRead(conversationId: Int, conversationMessageId: Int) {}
    fun onMessengerInitialized() {}
    fun onMessengerLoadFailed(failure: MessengerLoadFailure) {}
    fun onNewConversation(message: String, attachments: List<AttachmentPayload>) {}
    fun onNewMessageReceived(unreadMessage: UnreadMessage) {}
    fun onNewConversationReceived(conversationId: Int) {}
    fun onProfilingQuestionAnswered(attribute: String, value: String) {}
    fun onProfilingQuestionAsked(attribute: String) {}
    fun onRealtimeVideoAnswered(call: RealtimeCall) {}
    fun onRealtimeVideoCanceled() {}
    fun onRealtimeVideoReceived(call: RealtimeCall) {}
    fun onRealtimeVideoRejected() {}
    fun onSurveyAnswered() {}
    fun onSurveyPresented(survey: Survey) {}
    fun onSurveyRejected() {}
}

class JSBridge(private val showNotification: (String?, String?, Int, Int) -> Unit) {
    // Written from the caller's thread, read from the WebView's JavaBridge thread.
    private val callbacks = ConcurrentHashMap<String, CustomerlyCallback>()

    fun setCallback(type: String, callback: CustomerlyCallback) {
        callbacks[type] = callback
    }

    fun removeCallback(type: String) {
        callbacks.remove(type)
    }

    fun removeAllCallbacks() {
        callbacks.clear()
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
                    val article = data?.toHelpCenterArticle() ?: return
                    callbacks["onHelpCenterArticleOpened"]?.onHelpCenterArticleOpened(article)
                }

                "onLeadGenerated" -> {
                    val email = data?.optString("email")
                    callbacks["onLeadGenerated"]?.onLeadGenerated(email)
                }

                "onMessageRead" -> {
                    val conversationId = data?.getInt("conversationId") ?: 0
                    val conversationMessageId = data?.getInt("conversationMessageId") ?: 0
                    callbacks["onMessageRead"]?.onMessageRead(conversationId, conversationMessageId)
                }

                "onMessengerInitialized" -> callbacks["onMessengerInitialized"]?.onMessengerInitialized()

                "onMessengerLoadFailed" -> {
                    val failure = data?.toMessengerLoadFailure() ?: MessengerLoadFailure()
                    callbacks["onMessengerLoadFailed"]?.onMessengerLoadFailed(failure)
                }

                "onNewConversation" -> {
                    // We don't need to show a notification because this callback is triggered when the user creates a new conversation
                    val message = data?.getString("message") ?: ""
                    val attachments = data?.optJSONArray("attachments")?.let { array ->
                        List(array.length()) { array.getJSONObject(it).toAttachmentPayload() }
                    } ?: emptyList()
                    callbacks["onNewConversation"]?.onNewConversation(message, attachments)
                }

                "onNewMessageReceived" -> {
                    val unreadMessage = data?.toUnreadMessage() ?: return

                    // Use the conversation id as the notification id so that
                    // multiple messages in the same conversation collapse into
                    // (and update) a single notification, rather than spawning a
                    // new one per message. Truncating to Int is safe here because
                    // the deep-link path already treats conversation ids as Int.
                    val notificationId = unreadMessage.conversation_id.toInt()
                    showNotification(
                        unreadMessage.account_name,
                        unreadMessage.message,
                        notificationId,
                        unreadMessage.conversation_id.toInt()
                    )

                    callbacks["onNewMessageReceived"]?.onNewMessageReceived(unreadMessage)
                }

                "onNewConversationReceived" -> {
                    // We don't need to show a notification because when this callback is triggered, should also be triggered the onNewMessageReceived callback
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

                "onRealtimeVideoAnswered" -> {
                    val call = data?.toRealtimeCall() ?: return
                    callbacks["onRealtimeVideoAnswered"]?.onRealtimeVideoAnswered(call)
                }

                "onRealtimeVideoCanceled" -> callbacks["onRealtimeVideoCanceled"]?.onRealtimeVideoCanceled()
                "onRealtimeVideoReceived" -> {
                    Customerly.show(safe = true)

                    val call = data?.toRealtimeCall() ?: return
                    callbacks["onRealtimeVideoReceived"]?.onRealtimeVideoReceived(call)
                }

                "onRealtimeVideoRejected" -> callbacks["onRealtimeVideoRejected"]?.onRealtimeVideoRejected()
                "onSurveyAnswered" -> callbacks["onSurveyAnswered"]?.onSurveyAnswered()
                "onSurveyPresented" -> {
                    Customerly.show(withoutNavigation = true, safe = true)

                    val survey = data?.toSurvey() ?: return
                    callbacks["onSurveyPresented"]?.onSurveyPresented(survey)
                }

                "onSurveyRejected" -> {
                    callbacks["onSurveyRejected"]?.onSurveyRejected()
                }
            }
        } catch (e: Exception) {
            // Do not log the raw message payload: it can contain message content / PII.
            CustomerlyLog.e("Error processing a bridge message", e)
        }
    }
}
