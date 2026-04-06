package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

@JvmInline
value class SolutionId(
    override val value: Long,
) : DomainId

class SolutionData(
    val file: FileData,
    val language: TrikSupportedLanguage,
)

class Solution(
    id: SolutionId,
    createdAt: Instant,
    val data: SolutionData,
) : DomainEntity<SolutionId>(id, createdAt)