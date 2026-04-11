package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.task.graded
import tech.testsys.domain.builder.task.inProgress
import tech.testsys.domain.builder.task.queued
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.SubmissionStatus

/**
 * DSL chooser for selecting a [SubmissionStatus].
 *
 * @since %CURRENT_VERSION%
 */
class SubmissionStatusChooser : Chooser<SubmissionStatus>() {

    /**
     * Selects [SubmissionStatus.Graded] with a [GradingResult] configured via the [builder] block.
     *
     * @param builder a lambda on [GradingResult.Companion] that returns the grading result.
     * @since %CURRENT_VERSION%
     */
    fun graded(builder: GradingResult.Companion.() -> GradingResult) =
        makeChoice(builder(GradingResult.Companion).graded())

    /**
     * Selects [SubmissionStatus.Queued].
     *
     * @since %CURRENT_VERSION%
     */
    fun queued() = makeChoice(SubmissionStatus.queued())

    /**
     * Selects [SubmissionStatus.InProgress].
     *
     * @since %CURRENT_VERSION%
     */
    fun inProgress() = makeChoice(SubmissionStatus.inProgress())

}
