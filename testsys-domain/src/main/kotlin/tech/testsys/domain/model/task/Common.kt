package tech.testsys.domain.model.task


sealed interface TrikSupportedLanguage {
    object Python: TrikSupportedLanguage
    object JavaScript: TrikSupportedLanguage
    object VisualLanguage: TrikSupportedLanguage
}

@JvmInline
value class Score(
    val value: Int,
)

@JvmInline
value class TrikStudioVersion(
    val version: String
)

class FileData(
    val uploadedFilename: String,
    val content: ByteArray,
)

