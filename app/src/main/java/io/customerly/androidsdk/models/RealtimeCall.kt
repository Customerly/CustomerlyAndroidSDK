package io.customerly.androidsdk.models

data class RealtimeCall(
    val account: Account,
    val url: String,
    val conversation_id: Long,
    val user: RealtimeCallUser
)

data class RealtimeCallUser(
    val user_id: Long
) 