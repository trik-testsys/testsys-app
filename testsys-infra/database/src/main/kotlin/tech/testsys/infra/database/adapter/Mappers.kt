package tech.testsys.infra.database.adapter

import tech.testsys.domain.model.task.TrikSupportedLanguage

fun TrikSupportedLanguage.toDbString(): String = when (this) {
    is TrikSupportedLanguage.Python -> "PYTHON"
    is TrikSupportedLanguage.JavaScript -> "JAVASCRIPT"
    is TrikSupportedLanguage.VisualLanguage -> "VISUAL_LANGUAGE"
}

fun trikLanguageFromDb(value: String): TrikSupportedLanguage = when (value) {
    "PYTHON" -> TrikSupportedLanguage.Python
    "JAVASCRIPT" -> TrikSupportedLanguage.JavaScript
    "VISUAL_LANGUAGE" -> TrikSupportedLanguage.VisualLanguage
    else -> error("Unknown TrikSupportedLanguage: $value")
}