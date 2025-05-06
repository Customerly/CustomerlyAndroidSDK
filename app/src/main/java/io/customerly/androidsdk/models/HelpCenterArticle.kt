package io.customerly.androidsdk.models

data class HelpCenterArticle(
    val knowledge_base_article_id: Long,
    val knowledge_base_collection_id: Long,
    val app_id: String,
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val sort: Int,
    val written_by: WrittenBy,
    val updated_at: Long
)

data class WrittenBy(
    val account_id: Long, val email: String?, val name: String
)