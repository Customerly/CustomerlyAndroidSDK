package io.customerly.androidsdk

import io.customerly.androidsdk.models.SurveyQuestionType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the manual widget-payload (de)serializers — the SDK's most
 * regression-prone code. Uses the real org.json implementation (added as a test
 * dependency) so no emulator/Robolectric is required.
 */
class JsonMappersTest {

    @Test
    fun `toUnreadMessage maps camelCase fields and nulls empty ones`() {
        val json = JSONObject(
            """
            {
              "accountId": 42,
              "accountName": "Support",
              "message": "Hello",
              "timestamp": 1700000000000,
              "userId": 7,
              "conversationId": 12345
            }
            """.trimIndent()
        )

        val msg = json.toUnreadMessage()

        assertEquals(42L, msg.account_id)
        assertEquals("Support", msg.account_name)
        assertEquals("Hello", msg.message)
        assertEquals(1700000000000L, msg.timestamp)
        assertEquals(7L, msg.user_id)
        assertEquals(12345L, msg.conversation_id)
    }

    @Test
    fun `toUnreadMessage treats zero ids and empty strings as null`() {
        val json = JSONObject(
            """
            { "accountId": 0, "accountName": "", "message": "", "timestamp": 1, "userId": 0, "conversationId": 99 }
            """.trimIndent()
        )

        val msg = json.toUnreadMessage()

        assertNull(msg.account_id)
        assertNull(msg.account_name)
        assertNull(msg.message)
        assertNull(msg.user_id)
        assertEquals(99L, msg.conversation_id)
    }

    @Test
    fun `toSurveyQuestion accepts both string and numeric type`() {
        fun questionJson(type: String) = JSONObject(
            """
            {
              "survey_id": 1, "survey_question_id": 2, "step": 0,
              "type": $type, "choices": []
            }
            """.trimIndent()
        )

        assertEquals(SurveyQuestionType.Star, questionJson("\"Star\"").toSurveyQuestion().type)
        assertEquals(SurveyQuestionType.Star, questionJson("4").toSurveyQuestion().type)
    }

    @Test
    fun `toSurveyQuestion throws on missing type`() {
        val json = JSONObject(
            """{ "survey_id": 1, "survey_question_id": 2, "step": 0, "choices": [] }"""
        )
        assertThrows(IllegalArgumentException::class.java) { json.toSurveyQuestion() }
    }

    @Test
    fun `toHelpCenterArticle maps nested writtenBy`() {
        val json = JSONObject(
            """
            {
              "knowledge_base_article_id": 10, "knowledge_base_collection_id": 20,
              "app_id": "abc", "slug": "s", "title": "t", "description": "d",
              "body": "<p>hi</p>", "sort": 1,
              "written_by": { "account_id": 5, "email": "a@b.c", "name": "Jane" },
              "updated_at": 123
            }
            """.trimIndent()
        )

        val article = json.toHelpCenterArticle()

        assertEquals(10L, article.knowledge_base_article_id)
        assertEquals("Jane", article.written_by.name)
        assertEquals(5L, article.written_by.account_id)
    }

    @Test
    fun `toRealtimeCall parses a SimpleAccount without is_ai and reads ts`() {
        // The realtime-call account is a SimpleAccount (no is_ai) — this must not
        // throw, otherwise the realtime-video callbacks are silently dropped.
        val json = JSONObject(
            """
            {
              "account": { "account_id": 5, "name": "Agent" },
              "url": "https://call.example/room",
              "ts": 1700000000000,
              "conversation_id": 77,
              "user": { "user_id": 9 }
            }
            """.trimIndent()
        )

        val call = json.toRealtimeCall()

        assertEquals(5L, call.account.account_id)
        assertNull(call.account.is_ai)
        assertEquals(1700000000000L, call.ts)
        assertEquals(77L, call.conversation_id)
        assertEquals(9L, call.user.user_id)
    }

    @Test
    fun `toAccount reads is_ai when present`() {
        val account = JSONObject("""{ "account_id": 1, "name": "AI", "is_ai": true }""").toAccount()
        assertEquals(true, account.is_ai)
    }

    @Test
    fun `SurveyQuestionType fromInt maps known values and rejects unknown`() {
        assertEquals(SurveyQuestionType.Button, SurveyQuestionType.fromInt(0))
        assertEquals(SurveyQuestionType.Textarea, SurveyQuestionType.fromInt(7))
        assertThrows(IllegalArgumentException::class.java) { SurveyQuestionType.fromInt(99) }
    }
}
