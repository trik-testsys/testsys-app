package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.TaskData

/**
 * DSL chooser for selecting a [TaskData].
 *
 * @since %CURRENT_VERSION%
 */
class TaskDataChooser : Chooser<TaskData>() {

    fun new(wipBuilder: WipTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? NewTaskDataBuilder ?: NewTaskDataBuilder()
        currentBuilder.wip.apply(wipBuilder)
        makeChoice(currentBuilder)
    }

    fun uncommited(
        wipBuilder: WipTaskContentBuilder.() -> Unit,
        lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit,
    ) {
        val currentBuilder = choice as? UncommitedTaskDataBuilder ?: UncommitedTaskDataBuilder()
        currentBuilder.wip.apply(wipBuilder)
        currentBuilder.lastCommited.apply(lastCommitedBuilder)
        makeChoice(currentBuilder)
    }

    fun commited(lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? CommittedTaskDataBuilder ?: CommittedTaskDataBuilder()
        currentBuilder.lastCommited.apply(lastCommitedBuilder)
        makeChoice(currentBuilder)
    }
}

class NewTaskDataBuilder : Builder<TaskData> {
    val wip = WipTaskContentBuilder()
    override fun build() = TaskData.New(wip.build())
}

class UncommitedTaskDataBuilder : Builder<TaskData> {
    val wip = WipTaskContentBuilder()
    val lastCommited = CommittedTaskContentBuilder()
    override fun build() = TaskData.Uncommited(wip.build(), lastCommited.build())
}

class CommittedTaskDataBuilder : Builder<TaskData> {
    val lastCommited = CommittedTaskContentBuilder()
    override fun build() = TaskData.Committed(lastCommited.build())
}