package com.ahu.ahutong.data.model

import com.google.gson.annotations.SerializedName

data class EvalApiResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null,
    val ok: Boolean = false
)

data class EvalLoginResult(
    val token: String = "",
    val needCaptcha: Boolean = false,
    val captchaToken: String? = null,
    val loginName: String = ""
)

data class EvalAccount(
    val loginName: String = "",
    val currentIdentity: String = "",
    val currentBizTypeId: String = "",
    val currentSemesterId: String = ""
)

data class EvalSemester(
    val id: String = "",
    val nameZh: String = "",
    val nameEn: String = "",
    val code: String = "",
    val schoolYear: String = ""
)

data class EvalSearchResult(
    @SerializedName("data") val items: List<EvalTaskItem> = emptyList()
)

data class EvalTaskItem(
    val lessonId: String = "",
    val studentId: String = "",
    val courseName: String = "",
    val lessonCode: String = "",
    val lessonNameZh: String = "",
    val taskList: List<EvalTask> = emptyList(),
    val status: Boolean = false
)

data class EvalTask(
    val stdSumEvaBatchId: String = "",
    val showTotalScore: Boolean = false,
    val showQuestionScore: Boolean = false,
    val evaluationQuestionnaireId: String = "",
    val evaluationQuestionnaireName: String = "",
    val teachers: List<EvalTeacher> = emptyList(),
    val timeStatus: Boolean = false,
    val hours: String? = null,
    val minutes: String? = null,
    val days: String = "",
    val autoPublishDeadline: String? = null,
    val stdSumTaskId: String = ""
)

data class EvalTeacher(
    val stdSumTaskId: String = "",
    val teacherId: String = "",
    val personId: String = "",
    val portraitId: String? = null,
    val role: String = "",
    val num: Int = 0,
    val teacherName: String = "",
    val status: String = "",
    val code: String = ""
)

data class EvalQuestionnaire(
    val id: String = "",
    val nameZh: String = "",
    val nameEn: String? = null,
    val introduction: String? = null,
    val status: Boolean = false,
    val questions: String = "[]",
    val enable: Boolean = true,
    val needScoring: Boolean = false,
    val totalScore: Double = 0.0,
    val questionNum: String = "",
    val evaluateTypeId: String = "",
    val name: String = ""
)

data class EvalQuestion(
    val index: Int = 0,
    val attribute: EvalQuestionAttribute = EvalQuestionAttribute(),
    val options: List<EvalOption> = emptyList(),
    val optionSetting: EvalOptionSetting = EvalOptionSetting()
)

data class EvalQuestionAttribute(
    val questionCategoryNameZh: String = "",
    val enableDesc: Boolean = false,
    val typeName: String = "",
    val enableScore: Boolean = false,
    val required: Boolean = false,
    val questionItemNameZh: String = "",
    val score: Double = 0.0,
    val questionItemId: String = "",
    val name: String = "",
    val questionCategoryId: String = "",
    val typeId: Int = 0,
    val id: Int = 0,
    val desc: String = ""
)

data class EvalOption(
    val isInfinite: Boolean = false,
    val optionId: Int = 0,
    val optionScore: Double = 0.0,
    val value: String = ""
)

data class EvalOptionSetting(
    val layout: Int = 4,
    val randomSort: Boolean = false,
    val maxWords: Int? = null,
    val textBoxHeight: Int? = null,
    val minWords: String? = null,
    val isOpendMargin: Boolean = false
)

data class EvalCheckParam(
    val radioCheck: Boolean = false,
    val scoreCheck: Boolean = false
)

data class EvalSubmitRequest(
    val id: String? = null,
    val stdSumTaskId: String = "",
    val anonymous: Boolean = false,
    val evaluationQuestionnaireRes: EvalQuestionnaireRes = EvalQuestionnaireRes()
)

data class EvalQuestionnaireRes(
    val id: String? = null,
    val questionnaireId: String = "",
    val questionnaireName: String = "",
    val enable: Boolean = true,
    val answer: String = "[]",
    val score: Double = 0.0,
    val questionAnsSaveList: List<EvalQuestionAnswer> = emptyList()
)

data class EvalQuestionAnswer(
    val questionId: String = "",
    val questionnaireId: String = "",
    val type: String = "",
    val score: Double = 0.0,
    val answer: String? = null,
    val questionAnsExpSaveList: List<EvalAnswerOption> = emptyList()
)

data class EvalAnswerOption(
    val optionId: String = "",
    val questionnaireId: String = "",
    val value: String? = null,
    val score: Double? = null
)

data class EvalPreset(
    val radioOptionIndex: Int = 0,
    val radioOptionIndexes: Map<String, Int> = emptyMap(),
    val textAnswer: String = "老师授课认真负责，课堂内容清晰，学习收获较多。",
    val textAnswers: Map<String, String> = emptyMap(),
    val anonymous: Boolean = false
)
