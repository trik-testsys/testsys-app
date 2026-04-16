package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.VerdictId

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
    fun graded(builder: GradingSubmissionStatusBuilder.() -> Unit) {
        val currentBuilder = choice as? GradingSubmissionStatusBuilder ?: GradingSubmissionStatusBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [SubmissionStatus.Queued].
     *
     * @since %CURRENT_VERSION%
     */
    fun queued() = makeChoice(object : Builder<SubmissionStatus> {
        override fun build() = SubmissionStatus.Queued
    })

    /**
     * Selects [SubmissionStatus.InProgress].
     *
     * @since %CURRENT_VERSION%
     */
    fun inProgress() = makeChoice(object : Builder<SubmissionStatus> {
        override fun build() = SubmissionStatus.InProgress
    })

}

class GradingSubmissionStatusBuilder : Builder<SubmissionStatus> {

    val status = GradingResultStatusChooser()

    override fun build(): SubmissionStatus {
        return SubmissionStatus.Graded(status.build())
    }
}

class GradingResultStatusChooser : Chooser<GradingResult>() {

    fun timeout() = makeChoice(object : Builder<GradingResult> {
        override fun build() = GradingResult.Timeout
    })

    fun success(builder: SuccessGradingResultBuilder.() -> Unit) {
        val currentBuilder = choice as? SuccessGradingResultBuilder ?: SuccessGradingResultBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    fun error(builder: ErrorGradingResultBuilder.() -> Unit) {
        val currentBuilder = choice as? ErrorGradingResultBuilder ?: ErrorGradingResultBuilder()
        makeChoice(currentBuilder.apply(builder))
    }
}

class SuccessGradingResultBuilder : Builder<GradingResult> {

    var verdict: VerdictId? = null

    fun verdict(verdictId: Long) {
        verdict = VerdictId(verdictId)
    }

    override fun build(): GradingResult {
        val verdict = requireField(verdict) { ::verdict }
        return GradingResult.Success(verdict.lazify())
    }
}

class ErrorGradingResultBuilder : Builder<GradingResult> {

    var description: String? = null

    override fun build(): GradingResult {
        val description = requireField(description) { ::description }
        return GradingResult.GradingError(description)
    }
}