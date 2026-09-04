package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.SubmissionKind

/**
 * DSL chooser of a [SubmissionKind].
 *
 * @since %CURRENT_VERSION%
 */
class SubmissionKindChooser : Chooser<SubmissionKind>() {

    /**
     * Selects [SubmissionKind.Grading] configured by [builder]; repeated calls accumulate configuration.
     *
     * @since %CURRENT_VERSION%
     */
    fun grading(builder: GradingSubmissionKindBuilder.() -> Unit) {
        val currentBuilder = choice as? GradingSubmissionKindBuilder ?: GradingSubmissionKindBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [SubmissionKind.DeveloperSolutionTest].
     *
     * @since %CURRENT_VERSION%
     */
    fun developerSolutionTest() = makeChoice(
        object : Builder<SubmissionKind> {
            override fun build() = SubmissionKind.DeveloperSolutionTest
        },
    )
}

/**
 * Builder of [SubmissionKind.Grading]. Required: [contest].
 *
 * @property contest the id of the contest the submission was made in, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class GradingSubmissionKindBuilder : Builder<SubmissionKind> {

    var contest: ContestId? = null

    /**
     * Sets [contest] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun contest(contestId: Long) {
        contest = ContestId(contestId)
    }

    override fun build(): SubmissionKind {
        val contest = requireField(contest) { ::contest }
        return SubmissionKind.Grading(contest.lazify())
    }
}
