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
     * Selects [TaskContent.Uncommitted].
     *
     * @param wipBuilder the configuration block of the work-in-progress revision.
     * @param lastCommittedBuilder the configuration block of the last committed revision.
     * @since %CURRENT_VERSION%
     */
    fun uncommitted(wipBuilder: WipTaskContentBuilder.() -> Unit, lastCommittedBuilder: CommittedTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? UncommittedTaskContentBuilder ?: UncommittedTaskContentBuilder()
        currentBuilder.wip.apply(wipBuilder)
        currentBuilder.lastCommitted.apply(lastCommittedBuilder)
        makeChoice(currentBuilder)
    }

    /**
     * Selects [TaskContent.Committed] with the committed revision configured by [lastCommittedBuilder].
     *
     * @since %CURRENT_VERSION%
     */
    fun committed(lastCommittedBuilder: CommittedTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? CommittedTaskContentVariantBuilder ?: CommittedTaskContentVariantBuilder()
        currentBuilder.lastCommitted.apply(lastCommittedBuilder)
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
 * Builder of [TaskContent.Uncommitted].
 *
 * @property wip the builder of the work-in-progress revision.
 * @property lastCommitted the builder of the last committed revision.
 * @since %CURRENT_VERSION%
 */
class UncommittedTaskContentBuilder : Builder<TaskContent> {
    val wip = WipTaskContentBuilder()

    val lastCommitted = CommittedTaskContentBuilder()
    override fun build() = TaskContent.Uncommitted(wip.build(), lastCommitted.build())
}

/**
 * Builder of [TaskContent.Committed].
 *
 * @property lastCommitted the builder of the last committed revision.
 * @since %CURRENT_VERSION%
 */
class CommittedTaskContentVariantBuilder : Builder<TaskContent> {
    val lastCommitted = CommittedTaskContentBuilder()
    override fun build() = TaskContent.Committed(lastCommitted.build())
}
