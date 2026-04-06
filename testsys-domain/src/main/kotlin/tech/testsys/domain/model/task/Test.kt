package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

@JvmInline
value class TestId(
    override val value: Long,
) : DomainId

class TestData(
    val file: FileData,
    val versionData: VersionData<TestId, Test>,
)

class Test(
    id: TestId,
    createdAt: Instant,
    val data: TestData,
) : DomainEntity<TestId>(id, createdAt)