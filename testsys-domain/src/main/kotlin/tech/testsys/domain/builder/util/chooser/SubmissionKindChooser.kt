package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.task.developerSolutionTest
import tech.testsys.domain.builder.task.grading
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
    fun grading(contest: ContestId) = makeChoice(SubmissionKind.grading(contest))

    /**
     * Selects [SubmissionKind.Grading] for the given contest.
     *
     * @param contest the raw contest ID value.
     * @since %CURRENT_VERSION%
     */
    fun grading(contest: Long) = makeChoice(SubmissionKind.grading(contest))

    /**
     * Selects [SubmissionKind.DeveloperSolutionTest].
     *
     * @since %CURRENT_VERSION%
     */
    fun developerSolutionTest() = makeChoice(SubmissionKind.developerSolutionTest())

}
