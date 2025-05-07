package io.customerly.androidsdk.models

enum class SurveyQuestionType {
    Button, RadioButton, Select, Scale, Star, Integer, Textbox, Textarea;

    companion object {
        fun fromInt(value: Int): SurveyQuestionType {
            return when (value) {
                0 -> Button
                1 -> RadioButton
                2 -> Select
                3 -> Scale
                4 -> Star
                5 -> Integer
                6 -> Textbox
                7 -> Textarea
                else -> throw IllegalArgumentException("Invalid survey question type: $value")
            }
        }
    }
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