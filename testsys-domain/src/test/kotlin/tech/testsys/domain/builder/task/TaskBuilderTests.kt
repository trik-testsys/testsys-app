package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.taskDataCommited
import tech.testsys.domain.builder.api.taskDataNew
import tech.testsys.domain.builder.api.taskDataUncommited
import tech.testsys.domain.builder.util.chooser.TaskDataChooser
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData

class TaskBuilderTests : DomainEntityBuilderTests<Task, TaskData, TaskDataChooser>(
    TaskBuilder(),
    TaskDataChooser()
) {
    private fun commitedTaskContent(): CommittedTaskContentBuilder.() -> Unit = {
        owner(42)
        name = "Test Task"
        description = "A test task"
        exercise(1)
        supportedTrikStudioVersions(listOf("3.0.0"))
        statement(1)
    }

    private fun wipTaskContent(): WipTaskContentBuilder.() -> Unit = {
        owner(42)
        name = "Test Task"
    }

    override fun buildDataWithAllFields() = listOf(
        taskDataNew(wipTaskContent()),
        taskDataCommited(commitedTaskContent()),
        taskDataUncommited(wipTaskContent(), commitedTaskContent()),
    )
}
