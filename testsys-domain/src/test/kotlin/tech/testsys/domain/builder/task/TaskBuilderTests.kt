package tech.testsys.domain.builder.task

import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.VersionBucket
import java.util.UUID
import kotlin.test.assertEquals

class TaskBuilderTests : DomainEntityBuilderTests<Task, TaskData, TaskDataBuilder>(
    TaskBuilder(),
    TaskDataBuilder(),
) {
    private fun TaskDataBuilder.taskIdentity() {
        owner(42)
        name = "Test Task"
        description = "A test task"
    }

    private fun committedTaskContent(): CommittedTaskContentBuilder.() -> Unit = {
        exercise(1)
        statement(1)
        supportedTrikStudioVersions(listOf("3.0.0"))
    }

    private fun wipTaskContent(): WipTaskContentBuilder.() -> Unit = {}

    override fun buildDataWithAllFields() = listOf(
        taskData {
            taskIdentity()
            content.new(wipTaskContent())
        },
        taskData {
            taskIdentity()
            content.committed(committedTaskContent())
            uploadedResources.add(VersionBucket(UUID(0, 1)))
        },
        taskData {
            taskIdentity()
            content.uncommitted(
                wipBuilder = wipTaskContent(),
                lastCommittedBuilder = committedTaskContent(),
            )
        },
    )

    @Test
    fun `should build a task with no uploaded resources by default`() {
        val data = taskData {
            taskIdentity()
            content.new {}
        }

        assertEquals(emptySet(), data.uploadedResources)
    }

    @Test
    fun `should snapshot uploaded resources when the builder changes after build`() {
        val bucket = VersionBucket(UUID(0, 1))
        val builder = TaskDataBuilder().apply {
            taskIdentity()
            content.new {}
            uploadedResources.add(bucket)
        }

        val data = builder.build()
        builder.uploadedResources.clear()

        assertEquals(setOf(bucket), data.uploadedResources)
    }
}
