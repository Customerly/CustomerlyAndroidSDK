package io.customerly.androidsdk.models

data class UnreadMessage(
    val account_id: Long? = null,
    val account_name: String? = null,
    val message: String? = null,
    val timestamp: Long,
    val user_id: Long? = null,
    val conversation_id: Long
)
