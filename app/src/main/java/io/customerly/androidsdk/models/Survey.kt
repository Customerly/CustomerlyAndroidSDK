package io.customerly.androidsdk.models

enum class SurveyQuestionType {
    Button, RadioButton, Select, Scale, Star, Integer, Textbox, Textarea
}

data class Survey(
    val survey_id: Long,
    val creator: Account,
    val thank_you_text: String? = null,
    val seen_at: Long? = null,
    val question: SurveyQuestion? = null
)

data class SurveyQuestion(
    val survey_id: Long,
    val survey_question_id: Long,
    val step: Int,
    val title: String? = null,
    val subtitle: String? = null,
    val type: SurveyQuestionType,
    val limits: SurveyQuestionLimits? = null,
    val choices: List<SurveyQuestionChoice>
)

data class SurveyQuestionLimits(
    val from: Int, val to: Int
)

data class SurveyQuestionChoice(
    val survey_id: Long,
    val survey_question_id: Long,
    val survey_choice_id: Long,
    val step: Int,
    val value: String? = null
)