package com.ahu.ahutong.data

object GradeEvaluationGate {
    const val MESSAGE = "请先完成评教"

    private val htmlTagRegex = Regex("<[^>]+>")
    private val whitespaceRegex = Regex("\\s+")

    fun isRequiredPayload(payload: String?): Boolean {
        val source = payload.orEmpty()
        if (source.isBlank()) return false

        val normalized = source
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
        val plainText = normalized
            .replace(htmlTagRegex, "")
            .replace(whitespaceRegex, "")
        val lower = normalized.lowercase()

        val hasEvalWord = plainText.contains("评教") ||
            plainText.contains("评价") ||
            plainText.contains("教学质量")
        val hasPromptWord = plainText.contains("请先") ||
            plainText.contains("先完成") ||
            plainText.contains("完成后")

        return (hasPromptWord && hasEvalWord) ||
            lower.contains("student-summation") ||
            lower.contains("summation-forstudent")
    }

    fun isRequiredThrowable(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        return isRequiredPayload(throwable.message) ||
            isRequiredThrowable(throwable.cause)
    }
}
