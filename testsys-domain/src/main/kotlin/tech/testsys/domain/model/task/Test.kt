package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.Describable
import java.time.Instant

@JvmInline
value class TestId(
    override val value: Long,
) : DomainId

class TestData(
    val file: FileData,
    val versionData: VersionData<TestId, Test>,
    val name: String,
    val description: String,
)

class Test(
    id: TestId,
    createdAt: Instant,
    val data: TestData,
) : DomainEntity<TestId>(id, createdAt),
    Describable {

    override val name = data.name
    override val description = data.description
}