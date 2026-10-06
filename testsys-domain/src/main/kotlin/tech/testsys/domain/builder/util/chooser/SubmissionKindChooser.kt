package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.TrikStudioVersion

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
     * Selects [SubmissionKind.DeveloperSolutionTest] configured by [builder]; repeated calls accumulate configuration.
     *
     * @since %CURRENT_VERSION%
     */
    fun developerSolutionTest(builder: DeveloperSolutionTestSubmissionKindBuilder.() -> Unit) {
        val currentBuilder = choice as? DeveloperSolutionTestSubmissionKindBuilder
            ?: DeveloperSolutionTestSubmissionKindBuilder()
        makeChoice(currentBuilder.apply(builder))
    }
}

/**
 * Builder of [SubmissionKind.DeveloperSolutionTest]. Required: [trikStudioVersion].
 *
 * @property trikStudioVersion the TRIK Studio version used for the test run, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionTestSubmissionKindBuilder : Builder<SubmissionKind> {

    var trikStudioVersion: TrikStudioVersion? = null

    /**
     * Sets [trikStudioVersion] from a raw version tag.
     *
     * @since %CURRENT_VERSION%
     */
    fun trikStudioVersion(version: String) {
        trikStudioVersion = TrikStudioVersion(version = version)
    }

    override fun build(): SubmissionKind {
        val trikStudioVersion = requireField(trikStudioVersion) { ::trikStudioVersion }
        return SubmissionKind.DeveloperSolutionTest(trikStudioVersion = trikStudioVersion)
    }
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
