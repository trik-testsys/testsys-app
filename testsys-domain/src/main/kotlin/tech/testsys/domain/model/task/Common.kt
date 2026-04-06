package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity

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

data class VersionData<Id: DomainId, Entity: DomainEntity<Id>>(
    val root: LazyEntity<Id, Entity>,
    val index: Long,
) // TODO: add constraint "root+index unique"
