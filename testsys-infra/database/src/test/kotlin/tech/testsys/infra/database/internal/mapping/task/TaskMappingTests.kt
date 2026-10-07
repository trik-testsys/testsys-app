package tech.testsys.infra.database.internal.mapping.task

import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskStatusJpaEnum
import tech.testsys.infra.database.internal.mapping.EntityMappingTests
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@InternalDatabaseApi
class TaskMappingTests : EntityMappingTests<TaskMapping>() {

    override val mapping = TaskMapping

    @Test
    fun `should populate every exercise of a WIP revision from its associations`() {
        val revision = revision(listOf(ExerciseId(1), ExerciseId(2)))
        val builder = WipTaskContentBuilder()

        TaskContentMapping.populateWip(builder, revision)

        assertEquals(listOf(ExerciseId(1), ExerciseId(2)), builder.build().exercises.ids)
        assertEquals(
            setOf(1L to 20L, 2L to 20L),
            TaskContentMapping.toExerciseAssociations(20, revision.exerciseIds)
                .map { it.id.exerciseId to it.id.taskContentId }.toSet(),
        )
    }

    @Test
    fun `should reject a committed revision without exercise associations`() {
        val builder = CommittedTaskContentBuilder()

        assertFailsWith<IllegalArgumentException> { TaskContentMapping.populateCommitted(builder, revision(emptyList())) }
    }

    private fun revision(exerciseIds: List<ExerciseId>) = TaskContentRevision(
        jpaEntity = TaskContentJpaEntity(statementId = 10, id = 20),
        exerciseIds = exerciseIds,
        testIds = emptyList(),
        developerSolutionIds = emptyList(),
        supportedVersions = emptyList(),
    )

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
            jpaEntity = TaskContentJpaEntity(statementId = null, id = 20),
            exerciseIds = emptyList(),
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
