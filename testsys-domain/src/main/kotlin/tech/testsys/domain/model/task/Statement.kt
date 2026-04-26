package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant
import java.util.UUID

@JvmInline
value class StatementId(
    override val value: Long,
) : DomainId

class StatementData(
    val file: FileData,
    val name: String,
    val description: String,
    val versionBucket: UUID,
)

class Statement(
    id: StatementId,
    createdAt: Instant,
    val data: StatementData,
) : DomainEntity<StatementId>(id, createdAt)
