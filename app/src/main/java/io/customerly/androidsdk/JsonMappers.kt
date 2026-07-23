package io.customerly.androidsdk

import io.customerly.androidsdk.models.Account
import io.customerly.androidsdk.models.AttachmentPayload
import io.customerly.androidsdk.models.HelpCenterArticle
import io.customerly.androidsdk.models.RealtimeCall
import io.customerly.androidsdk.models.RealtimeCallUser
import io.customerly.androidsdk.models.Survey
import io.customerly.androidsdk.models.SurveyQuestion
import io.customerly.androidsdk.models.SurveyQuestionChoice
import io.customerly.androidsdk.models.SurveyQuestionLimits
import io.customerly.androidsdk.models.SurveyQuestionType
import io.customerly.androidsdk.models.UnreadMessage
import io.customerly.androidsdk.models.WrittenBy
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manual JSON → data-class mappers for the widget's payloads. Kept as `internal`
 * top-level functions (rather than private inside [JSBridge]) so they can be
 * exercised directly by JVM unit tests — this is the most regression-prone code
 * in the SDK.
 */

internal fun JSONObject.toHelpCenterArticle(): HelpCenterArticle {
    return HelpCenterArticle(
        knowledge_base_article_id = getLong("knowledge_base_article_id"),
        knowledge_base_collection_id = getLong("knowledge_base_collection_id"),
        app_id = getString("app_id"),
        slug = getString("slug"),
        title = getString("title"),
        description = getString("description"),
        // body only exists on the "detailed" article; read leniently so the
        // payload still parses if a summary-only article is delivered.
        body = optString("body"),
        sort = getInt("sort"),
        written_by = getJSONObject("written_by").toWrittenBy(),
        updated_at = getLong("updated_at")
    )
}

internal fun JSONObject.toWrittenBy(): WrittenBy {
    return WrittenBy(
        account_id = getLong("account_id"), email = optString("email"), name = getString("name")
    )
}

internal fun JSONObject.toAttachmentPayload(): AttachmentPayload {
    return AttachmentPayload(
        name = getString("name"), size = getLong("size"), base64 = getString("base64")
    )
}

internal fun JSONObject.toSurvey(): Survey {
    return Survey(
        survey_id = getLong("survey_id"),
        creator = getJSONObject("creator").toAccount(),
        thank_you_text = optString("thank_you_text"),
        seen_at = optLong("seen_at"),
        question = optJSONObject("question")?.toSurveyQuestion()
    )
}

internal fun JSONObject.toAccount(): Account {
    return Account(
        account_id = getLong("account_id"),
        name = optString("name"),
        // is_ai is present on full accounts (e.g. survey creators) but absent on
        // the SimpleAccount used by realtime calls — read it defensively so the
        // whole payload doesn't fail to parse when it's missing.
        is_ai = if (has("is_ai")) getBoolean("is_ai") else null
    )
}

internal fun JSONObject.toSurveyQuestion(): SurveyQuestion {
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

internal fun JSONObject.toSurveyQuestionLimits(): SurveyQuestionLimits {
    return SurveyQuestionLimits(
        from = getInt("from"), to = getInt("to")
    )
}

internal fun JSONArray.toSurveyQuestionChoices(): List<SurveyQuestionChoice> {
    return List(length()) { i ->
        getJSONObject(i).toSurveyQuestionChoice()
    }
}

internal fun JSONObject.toSurveyQuestionChoice(): SurveyQuestionChoice {
    return SurveyQuestionChoice(
        survey_id = getLong("survey_id"),
        survey_question_id = getLong("survey_question_id"),
        survey_choice_id = getLong("survey_choice_id"),
        step = getInt("step"),
        value = optString("value")
    )
}

internal fun JSONObject.toRealtimeCall(): RealtimeCall {
    return RealtimeCall(
        account = getJSONObject("account").toAccount(),
        url = getString("url"),
        conversation_id = getLong("conversation_id"),
        user = getJSONObject("user").toRealtimeCallUser(),
        ts = if (has("ts")) getLong("ts") else null
    )
}

internal fun JSONObject.toRealtimeCallUser(): RealtimeCallUser {
    return RealtimeCallUser(
        user_id = getLong("user_id")
    )
}

internal fun JSONObject.toUnreadMessage(): UnreadMessage {
    return UnreadMessage(
        account_id = optLong("accountId").takeIf { it != 0L },
        account_name = optString("accountName").takeIf { it.isNotEmpty() },
        message = optString("message").takeIf { it.isNotEmpty() },
        timestamp = getLong("timestamp"),
        user_id = optLong("userId").takeIf { it != 0L },
        conversation_id = getLong("conversationId")
    )
}
