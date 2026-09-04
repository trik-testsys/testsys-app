package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.TaskContent

/**
 * DSL chooser of a [TaskContent] variant. Repeated calls of the same variant accumulate configuration.
 *
 * @since %CURRENT_VERSION%
 */
class TaskContentChooser : Chooser<TaskContent>() {

    /**
     * Selects [TaskContent.New] with the work-in-progress revision configured by [wipBuilder].
     *
     * @since %CURRENT_VERSION%
     */
    fun new(wipBuilder: WipTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? NewTaskContentBuilder ?: NewTaskContentBuilder()
        currentBuilder.wip.apply(wipBuilder)
        makeChoice(currentBuilder)
    }

    /**
     * Selects [TaskContent.Uncommited].
     *
     * @param wipBuilder the configuration block of the work-in-progress revision.
     * @param lastCommitedBuilder the configuration block of the last committed revision.
     * @since %CURRENT_VERSION%
     */
    fun uncommited(wipBuilder: WipTaskContentBuilder.() -> Unit, lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? UncommitedTaskContentBuilder ?: UncommitedTaskContentBuilder()
        currentBuilder.wip.apply(wipBuilder)
        currentBuilder.lastCommited.apply(lastCommitedBuilder)
        makeChoice(currentBuilder)
    }

    /**
     * Selects [TaskContent.Committed] with the committed revision configured by [lastCommitedBuilder].
     *
     * @since %CURRENT_VERSION%
     */
    fun committed(lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? CommittedTaskContentVariantBuilder ?: CommittedTaskContentVariantBuilder()
        currentBuilder.lastCommited.apply(lastCommitedBuilder)
        makeChoice(currentBuilder)
    }
}

/**
 * Builder of [TaskContent.New].
 *
 * @property wip the builder of the work-in-progress revision.
 * @since %CURRENT_VERSION%
 */
class NewTaskContentBuilder : Builder<TaskContent> {
    val wip = WipTaskContentBuilder()
    override fun build() = TaskContent.New(wip.build())
}

/**
 * Builder of [TaskContent.Uncommited].
 *
 * @property wip the builder of the work-in-progress revision.
 * @property lastCommited the builder of the last committed revision.
 * @since %CURRENT_VERSION%
 */
class UncommitedTaskContentBuilder : Builder<TaskContent> {
    val wip = WipTaskContentBuilder()

    val lastCommited = CommittedTaskContentBuilder()
    override fun build() = TaskContent.Uncommited(wip.build(), lastCommited.build())
}

/**
 * Builder of [TaskContent.Committed].
 *
 * @property lastCommited the builder of the last committed revision.
 * @since %CURRENT_VERSION%
 */
class CommittedTaskContentVariantBuilder : Builder<TaskContent> {
    val lastCommited = CommittedTaskContentBuilder()
    override fun build() = TaskContent.Committed(lastCommited.build())
}
