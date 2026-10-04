package tech.testsys.infra.database.internal.mapping.task

import org.junit.jupiter.api.Test
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskStatusJpaEnum
import tech.testsys.infra.database.internal.mapping.EntityMappingTests
import java.util.UUID
import kotlin.test.assertEquals

@InternalDatabaseApi
class TaskMappingTests : EntityMappingTests<TaskMapping>() {

    override val mapping = TaskMapping

    @Test
    fun `should assemble uploaded resource chains separately from new content`() {
        val buckets = setOf(VersionBucket(UUID(0, 1)), VersionBucket(UUID(0, 2)))
        val row = TaskJpaEntity(
            name = "Task",
            description = "Description",
            ownerId = 10,
            status = TaskStatusJpaEnum.NEW,
            wipContentId = 20,
            committedContentId = null,
            id = 30,
        )
        val wip = TaskContentRevision(
            jpaEntity = TaskContentJpaEntity(exerciseId = null, statementId = null, id = 20),
            testIds = emptyList(),
            developerSolutionIds = emptyList(),
            supportedVersions = emptyList(),
        )

        val result = mapping.toDomain(
            jpaEntity = row,
            wip = wip,
            committed = null,
            sharedToIds = emptyList(),
            uploadedResourceBuckets = buckets,
        )

        assertEquals(buckets, result.data.uploadedResources)
        assertEquals(
            buckets.map { it.value }.toSet(),
            mapping.toUploadedResourceAssociations(30, buckets).map { it.id.versionBucket }.toSet(),
        )
    }
}
