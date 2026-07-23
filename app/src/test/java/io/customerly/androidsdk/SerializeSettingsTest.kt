package io.customerly.androidsdk

import io.customerly.androidsdk.models.CustomerlySettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializeSettingsTest {

    private fun serialize(settings: CustomerlySettings): JSONObject =
        JSONObject(Customerly.serializeSettings(settings))

    @Test
    fun `always forces sdkMode and showBackInsteadOfClose`() {
        val json = serialize(CustomerlySettings(app_id = "APP"))

        assertTrue(json.getBoolean("sdkMode"))
        assertTrue(json.getBoolean("showBackInsteadOfClose"))
        assertEquals("APP", json.getString("app_id"))
    }

    @Test
    fun `omits null optional fields`() {
        val json = serialize(CustomerlySettings(app_id = "APP"))

        assertFalse(json.has("user_id"))
        assertFalse(json.has("email"))
        assertFalse(json.has("company"))
    }

    @Test
    fun `includes provided user identity fields`() {
        val json = serialize(
            CustomerlySettings(
                app_id = "APP",
                user_id = "123",
                email = "gb@customerly.io",
                name = "Giorgio"
            )
        )

        assertEquals("123", json.getString("user_id"))
        assertEquals("gb@customerly.io", json.getString("email"))
        assertEquals("Giorgio", json.getString("name"))
    }

    @Test
    fun `merges company additional attributes`() {
        val json = serialize(
            CustomerlySettings(
                app_id = "APP",
                company = CustomerlySettings.Company(
                    company_id = "c1",
                    name = "Acme",
                    additionalAttributes = mapOf("plan" to "pro")
                )
            )
        )

        val company = json.getJSONObject("company")
        assertEquals("c1", company.getString("company_id"))
        assertEquals("Acme", company.getString("name"))
        assertEquals("pro", company.getString("plan"))
    }

    @Test
    fun `escapes values that would otherwise break the JSON or injected JS`() {
        // A name containing quotes / backslashes must survive as data, not break out.
        val nasty = "O'Brien \" ); alert(1); //\n\\"
        val json = serialize(CustomerlySettings(app_id = "APP", name = nasty))

        // Round-trips back to exactly the original string.
        assertEquals(nasty, json.getString("name"))
    }
}
