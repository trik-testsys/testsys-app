package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData

class TaskBuilderTests : DomainEntityBuilderTests<Task, TaskData, TaskDataBuilder>(
    TaskBuilder(),
    TaskDataBuilder()
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
        },
        taskData {
            taskIdentity()
            content.uncommitted(
                wipBuilder = wipTaskContent(),
                lastCommittedBuilder = committedTaskContent(),
            )
        },
    )
}
