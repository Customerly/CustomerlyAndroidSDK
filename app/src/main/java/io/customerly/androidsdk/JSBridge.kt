package io.customerly.androidsdk

import android.util.Log
import android.webkit.JavascriptInterface
import io.customerly.androidsdk.models.*
import org.json.JSONArray
import org.json.JSONObject

interface CustomerlyCallback {
    fun onChatClosed() {}
    fun onChatOpened() {}
    fun onHelpCenterArticleOpened(article: HelpCenterArticle) {}
    fun onLeadGenerated(email: String?) {}
    fun onMessageRead(conversationId: Int, conversationMessageId: Int) {}
    fun onMessengerInitialized() {}
    fun onNewConversation(message: String, attachments: List<AttachmentPayload>) {}
    fun onNewMessageReceived(
        accountId: Int?, message: String?, timestamp: Long, userId: Int?, conversationId: Int
    ) {
    }
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

class JSBridge(private val showNotification: (String?, Int, Int) -> Unit) {
    private val callbacks = mutableMapOf<String, CustomerlyCallback>()

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

                "onNewConversation" -> {
                    // We don't need to show a notification because this callback is triggered when the user creates a new conversation
                    val message = data?.getString("message") ?: ""
                    val attachments = data?.optJSONArray("attachments")?.let { array ->
                        List(array.length()) { array.getJSONObject(it).toAttachmentPayload() }
                    } ?: emptyList()
                    callbacks["onNewConversation"]?.onNewConversation(message, attachments)
                }

                "onNewMessageReceived" -> {
                    val accountId = data?.optInt("accountId")
                    val message = data?.optString("message")
                    val timestamp = data?.getLong("timestamp") ?: 0L
                    val userId = data?.optInt("userId")
                    val conversationId = data?.getInt("conversationId") ?: 0

                    // Generate notification ID from conversationId and timestamp
                    val notificationId = (conversationId + timestamp).toInt()
                    showNotification(message, notificationId, conversationId)

                    callbacks["onNewMessageReceived"]?.onNewMessageReceived(
                        accountId, message, timestamp, userId, conversationId
                    )
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
            Log.e("CustomerlySDK", "Error processing message: $message", e)
        }
    }

    private fun JSONObject.toHelpCenterArticle(): HelpCenterArticle {
        return HelpCenterArticle(
            knowledge_base_article_id = getLong("knowledge_base_article_id"),
            knowledge_base_collection_id = getLong("knowledge_base_collection_id"),
            app_id = getString("app_id"),
            slug = getString("slug"),
            title = getString("title"),
            description = getString("description"),
            body = getString("body"),
            sort = getInt("sort"),
            written_by = getJSONObject("written_by").toWrittenBy(),
            updated_at = getLong("updated_at")
        )
    }

    private fun JSONObject.toWrittenBy(): WrittenBy {
        return WrittenBy(
            account_id = getLong("account_id"), email = optString("email"), name = getString("name")
        )
    }

    private fun JSONObject.toAttachmentPayload(): AttachmentPayload {
        return AttachmentPayload(
            name = getString("name"), size = getLong("size"), base64 = getString("base64")
        )
    }

    private fun JSONObject.toSurvey(): Survey {
        return Survey(
            survey_id = getLong("survey_id"),
            creator = getJSONObject("creator").toAccount(),
            thank_you_text = optString("thank_you_text"),
            seen_at = optLong("seen_at"),
            question = optJSONObject("question")?.toSurveyQuestion()
        )
    }

    private fun JSONObject.toAccount(): Account {
        return Account(
            account_id = getLong("account_id"),
            name = optString("name"),
            is_ai = getBoolean("is_ai")
        )
    }

    private fun JSONObject.toSurveyQuestion(): SurveyQuestion {
        return SurveyQuestion(
            survey_id = getLong("survey_id"),
            survey_question_id = getLong("survey_question_id"),
            step = getInt("step"),
            title = optString("title"),
            subtitle = optString("subtitle"),
            type = if (has("type")) {
                when (val typeValue = get("type")) {
                    is String -> SurveyQuestionType.valueOf(typeValue)
                    is Number -> SurveyQuestionType.fromInt(typeValue.toInt())
                    else -> throw IllegalArgumentException("Invalid type value: $typeValue")
                }
            } else {
                throw IllegalArgumentException("Missing type field")
            },
            limits = optJSONObject("limits")?.toSurveyQuestionLimits(),
            choices = getJSONArray("choices").toSurveyQuestionChoices()
        )
    }

    private fun JSONObject.toSurveyQuestionLimits(): SurveyQuestionLimits {
        return SurveyQuestionLimits(
            from = getInt("from"), to = getInt("to")
        )
    }

    private fun JSONArray.toSurveyQuestionChoices(): List<SurveyQuestionChoice> {
        return List(length()) { i ->
            getJSONObject(i).toSurveyQuestionChoice()
        }
    }

    private fun JSONObject.toSurveyQuestionChoice(): SurveyQuestionChoice {
        return SurveyQuestionChoice(
            survey_id = getLong("survey_id"),
            survey_question_id = getLong("survey_question_id"),
            survey_choice_id = getLong("survey_choice_id"),
            step = getInt("step"),
            value = optString("value")
        )
    }

    private fun JSONObject.toRealtimeCall(): RealtimeCall {
        return RealtimeCall(
            account = getJSONObject("account").toAccount(),
            url = getString("url"),
            conversation_id = getLong("conversation_id"),
            user = getJSONObject("user").toRealtimeCallUser()
        )
    }

    private fun JSONObject.toRealtimeCallUser(): RealtimeCallUser {
        return RealtimeCallUser(
            user_id = getLong("user_id")
        )
    }
}
