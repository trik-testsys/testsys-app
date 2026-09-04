package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.VerdictId

/**
 * DSL chooser of a [SubmissionStatus].
 *
 * @since %CURRENT_VERSION%
 */
class SubmissionStatusChooser : Chooser<SubmissionStatus>() {

    /**
     * Selects [SubmissionStatus.Graded] configured by [builder]; repeated calls accumulate configuration.
     *
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

/**
 * Builder of [SubmissionStatus.Graded]. Required: a choice in [status].
 *
 * @property status the chooser of the [GradingResult].
 * @since %CURRENT_VERSION%
 */
class GradingSubmissionStatusBuilder : Builder<SubmissionStatus> {

    val status = GradingResultStatusChooser()

    override fun build(): SubmissionStatus {
        return SubmissionStatus.Graded(status.build())
    }
}

/**
 * DSL chooser of a [GradingResult].
 *
 * @since %CURRENT_VERSION%
 */
class GradingResultStatusChooser : Chooser<GradingResult>() {

    /**
     * Selects [GradingResult.Timeout].
     *
     * @since %CURRENT_VERSION%
     */
    fun timeout() = makeChoice(object : Builder<GradingResult> {
        override fun build() = GradingResult.Timeout
    })

    /**
     * Selects [GradingResult.Success] configured by [builder]; repeated calls accumulate configuration.
     *
     * @since %CURRENT_VERSION%
     */
    fun success(builder: SuccessGradingResultBuilder.() -> Unit) {
        val currentBuilder = choice as? SuccessGradingResultBuilder ?: SuccessGradingResultBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [GradingResult.GradingError] configured by [builder]; repeated calls accumulate configuration.
     *
     * @since %CURRENT_VERSION%
     */
    fun error(builder: ErrorGradingResultBuilder.() -> Unit) {
        val currentBuilder = choice as? ErrorGradingResultBuilder ?: ErrorGradingResultBuilder()
        makeChoice(currentBuilder.apply(builder))
    }
}

/**
 * Builder of [GradingResult.Success]. Required: [verdict].
 *
 * @property verdict the id of the produced verdict, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class SuccessGradingResultBuilder : Builder<GradingResult> {

    var verdict: VerdictId? = null

    /**
     * Sets [verdict] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun verdict(verdictId: Long) {
        verdict = VerdictId(verdictId)
    }

    override fun build(): GradingResult {
        val verdict = requireField(verdict) { ::verdict }
        return GradingResult.Success(verdict.lazify())
    }
}

/**
 * Builder of [GradingResult.GradingError]. Required: [description].
 *
 * @property description the human-readable description of the failure, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class ErrorGradingResultBuilder : Builder<GradingResult> {

    var description: String? = null

    override fun build(): GradingResult {
        val description = requireField(description) { ::description }
        return GradingResult.GradingError(description)
    }
}
