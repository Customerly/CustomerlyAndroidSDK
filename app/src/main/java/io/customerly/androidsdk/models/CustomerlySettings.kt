package io.customerly.androidsdk.models

data class CustomerlySettings(
    val app_id: String,
    val accentColor: String? = null,
    val contrastColor: String? = null,
    val attachmentsAvailable: Boolean? = null,
    val singleConversation: Boolean? = null,
    val user_id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val email_hash: String? = null,
    val events: List<Event>? = null,
    val last_page_viewed: String? = null,
    val force_lead: Boolean? = null,
    val attributes: Map<String, Any>? = null,
    val company: Company? = null
) {
    data class Event(
        val name: String, val date: Long? = null
    )

    data class Company(
        val company_id: String,
        val name: String,
        val additionalAttributes: Map<String, Any> = emptyMap()
    )
}
