package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.SubmissionKind

/**
 * DSL chooser for selecting a [SubmissionKind].
 *
 * @since %CURRENT_VERSION%
 */
class SubmissionKindChooser : Chooser<SubmissionKind>() {

    /**
     * Selects [SubmissionKind.Grading] for the given contest.
     *
     * @param contest the contest ID.
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

class GradingSubmissionKindBuilder : Builder<SubmissionKind> {

    var contest: ContestId? = null

    fun contest(contestId: Long) {
        contest = ContestId(contestId)
    }

    override fun build(): SubmissionKind {
        val contest = requireField(contest) { ::contest }
        return SubmissionKind.Grading(contest.lazify())
    }
}
