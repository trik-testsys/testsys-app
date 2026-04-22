package tech.testsys.domain.model.task

import tech.testsys.domain.model.Describable
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

@JvmInline
value class StatementId(
    override val value: Long,
) : DomainId

class StatementData(
    val file: FileData,
    val name: String,
    val description: String,
)

class Statement(
    id: StatementId,
    createdAt: Instant,
    val versionData: VersionData<StatementId, Statement>,
    val data: StatementData
) : DomainEntity<StatementId>(id, createdAt),
    Describable {

    override val name = data.name
    override val description = data.description
}