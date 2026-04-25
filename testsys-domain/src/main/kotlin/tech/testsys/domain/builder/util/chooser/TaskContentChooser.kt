package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.TaskContent

/**
 * DSL chooser for selecting a [TaskContent] variant
 * ([TaskContent.New], [TaskContent.Uncommited], or [TaskContent.Committed]).
 *
 * @since %CURRENT_VERSION%
 */
class TaskContentChooser : Chooser<TaskContent>() {

    /**
     * Selects [TaskContent.New] populated by the given [wipBuilder] block.
     *
     * @param wipBuilder configuration block applied to the WIP content builder.
     * @since %CURRENT_VERSION%
     */
    fun new(wipBuilder: WipTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? NewTaskContentBuilder ?: NewTaskContentBuilder()
        currentBuilder.wip.apply(wipBuilder)
        makeChoice(currentBuilder)
    }

    /**
     * Selects [TaskContent.Uncommited] populated by the given builder blocks.
     *
     * @param wipBuilder configuration block applied to the WIP content builder.
     * @param lastCommitedBuilder configuration block applied to the last-committed content builder.
     * @since %CURRENT_VERSION%
     */
    fun uncommited(
        wipBuilder: WipTaskContentBuilder.() -> Unit,
        lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit,
    ) {
        val currentBuilder = choice as? UncommitedTaskContentBuilder ?: UncommitedTaskContentBuilder()
        currentBuilder.wip.apply(wipBuilder)
        currentBuilder.lastCommited.apply(lastCommitedBuilder)
        makeChoice(currentBuilder)
    }

    /**
     * Selects [TaskContent.Committed] populated by the given [lastCommitedBuilder] block.
     *
     * @param lastCommitedBuilder configuration block applied to the last-committed content builder.
     * @since %CURRENT_VERSION%
     */
    fun committed(lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit) {
        val currentBuilder = choice as? CommittedTaskContentVariantBuilder ?: CommittedTaskContentVariantBuilder()
        currentBuilder.lastCommited.apply(lastCommitedBuilder)
        makeChoice(currentBuilder)
    }
}

/**
 * Internal builder producing a [TaskContent.New] from a [WipTaskContentBuilder].
 *
 * @since %CURRENT_VERSION%
 */
class NewTaskContentBuilder : Builder<TaskContent> {
    val wip = WipTaskContentBuilder()
    override fun build() = TaskContent.New(wip.build())
}

/**
 * Internal builder producing a [TaskContent.Uncommited] from WIP and last-committed builders.
 *
 * @since %CURRENT_VERSION%
 */
class UncommitedTaskContentBuilder : Builder<TaskContent> {
    val wip = WipTaskContentBuilder()
    val lastCommited = CommittedTaskContentBuilder()
    override fun build() = TaskContent.Uncommited(wip.build(), lastCommited.build())
}

/**
 * Internal builder producing a [TaskContent.Committed] from a last-committed builder.
 *
 * @since %CURRENT_VERSION%
 */
class CommittedTaskContentVariantBuilder : Builder<TaskContent> {
    val lastCommited = CommittedTaskContentBuilder()
    override fun build() = TaskContent.Committed(lastCommited.build())
}
