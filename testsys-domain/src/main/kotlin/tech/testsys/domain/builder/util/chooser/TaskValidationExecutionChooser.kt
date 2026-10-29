package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.*
import java.time.Instant

/**
 * DSL chooser of the execution state of a task validation request.
 *
 * @since 1.0.0-beta.1
 */
class TaskValidationExecutionChooser : Chooser<TaskValidationExecution>() {
    /**
     * Selects [TaskValidationExecution.PendingDiagnostics].
     *
     * @since 1.0.0-beta.1
     */
    fun pendingDiagnostics() = makeChoice(object : Builder<TaskValidationExecution> {
        override fun build() = TaskValidationExecution.PendingDiagnostics
    })

    /**
     * Selects [TaskValidationExecution.AwaitingSubmissions] configured by [builder].
     * Repeated calls accumulate configuration.
     *
     * @since 1.0.0-beta.1
     */
    fun awaitingSubmissions(builder: AwaitingSubmissionsBuilder.() -> Unit) {
        val currentBuilder = choice as? AwaitingSubmissionsBuilder ?: AwaitingSubmissionsBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [TaskValidationExecution.StoppedByDiagnostics] configured by [builder].
     * Repeated calls accumulate configuration.
     *
     * @since 1.0.0-beta.1
     */
    fun stoppedByDiagnostics(builder: StoppedByDiagnosticsBuilder.() -> Unit) {
        val currentBuilder = choice as? StoppedByDiagnosticsBuilder ?: StoppedByDiagnosticsBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [TaskValidationExecution.SubmissionsCreated] configured by [builder].
     * Repeated calls accumulate configuration.
     *
     * @since 1.0.0-beta.1
     */
    fun submissionsCreated(builder: SubmissionsCreatedBuilder.() -> Unit) {
        val currentBuilder = choice as? SubmissionsCreatedBuilder ?: SubmissionsCreatedBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [TaskValidationExecution.Completed] configured by [builder].
     * Repeated calls accumulate configuration.
     *
     * @since 1.0.0-beta.1
     */
    fun completed(builder: CompletedValidationBuilder.() -> Unit) {
        val currentBuilder = choice as? CompletedValidationBuilder ?: CompletedValidationBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [TaskValidationExecution.TechnicalFailure.IncompleteDiagnostics] configured by [builder].
     * Repeated calls accumulate configuration.
     *
     * @since 1.0.0-beta.1
     */
    fun incompleteDiagnosticsFailure(builder: IncompleteDiagnosticsFailureBuilder.() -> Unit) {
        val currentBuilder = choice as? IncompleteDiagnosticsFailureBuilder ?: IncompleteDiagnosticsFailureBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [TaskValidationExecution.TechnicalFailure.CompletedDiagnostics] configured by [builder].
     * Repeated calls accumulate configuration.
     *
     * @since 1.0.0-beta.1
     */
    fun completedDiagnosticsFailure(builder: CompletedDiagnosticsFailureBuilder.() -> Unit) {
        val currentBuilder = choice as? CompletedDiagnosticsFailureBuilder ?: CompletedDiagnosticsFailureBuilder()
        makeChoice(currentBuilder.apply(builder))
    }

    /**
     * Selects [TaskValidationExecution.TechnicalFailure.CreatedSubmissions] configured by [builder].
     * Repeated calls accumulate configuration.
     *
     * @since 1.0.0-beta.1
     */
    fun createdSubmissionsFailure(builder: CreatedSubmissionsFailureBuilder.() -> Unit) {
        val currentBuilder = choice as? CreatedSubmissionsFailureBuilder ?: CreatedSubmissionsFailureBuilder()
        makeChoice(currentBuilder.apply(builder))
    }
}

/**
 * Builder of [TaskValidationExecution.AwaitingSubmissions].
 *
 * @property diagnostics the results of the completed diagnostic stage.
 * @since 1.0.0-beta.1
 */
class AwaitingSubmissionsBuilder : Builder<TaskValidationExecution> {
    var diagnostics: MutableList<TestDiagnosticResult> = mutableListOf()

    override fun build(): TaskValidationExecution = TaskValidationExecution.AwaitingSubmissions(
        diagnostics = diagnostics.toList(),
    )
}

/**
 * Builder of [TaskValidationExecution.StoppedByDiagnostics]. Required: [completedAt].
 *
 * @property diagnostics the results of the completed diagnostic stage.
 * @property completedAt the terminal stop moment, or `null` if not set.
 * @since 1.0.0-beta.1
 */
class StoppedByDiagnosticsBuilder : Builder<TaskValidationExecution> {
    var diagnostics: MutableList<TestDiagnosticResult> = mutableListOf()
    var completedAt: Instant? = null

    override fun build(): TaskValidationExecution = TaskValidationExecution.StoppedByDiagnostics(
        diagnostics = diagnostics.toList(),
        completedAt = requireField(completedAt) { ::completedAt },
    )
}

/**
 * Builder of [TaskValidationExecution.SubmissionsCreated].
 *
 * @property diagnostics the results of the completed diagnostic stage.
 * @property submissions the identifiers of the created submissions.
 * @since 1.0.0-beta.1
 */
class SubmissionsCreatedBuilder : Builder<TaskValidationExecution> {
    var diagnostics: MutableList<TestDiagnosticResult> = mutableListOf()
    var submissions: MutableList<SubmissionId> = mutableListOf()

    /**
     * Sets [submissions] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun submissions(submissions: Iterable<Long>) {
        this.submissions = submissions.map { SubmissionId(it) }.toMutableList()
    }

    override fun build(): TaskValidationExecution = TaskValidationExecution.SubmissionsCreated(
        diagnostics = diagnostics.toList(),
        submissions = submissions.toList().lazify(),
    )
}

/**
 * Builder of [TaskValidationExecution.Completed]. Required: [completedAt].
 *
 * @property diagnostics the completed diagnostic results.
 * @property submissions the ordered author submission identifiers.
 * @property failures the failed author submissions, empty if testing succeeded.
 * @property completedAt the terminal completion moment, or `null` if not set.
 * @since 1.0.0-beta.1
 */
class CompletedValidationBuilder : Builder<TaskValidationExecution> {
    var diagnostics: MutableList<TestDiagnosticResult> = mutableListOf()
    var submissions: MutableList<SubmissionId> = mutableListOf()
    var failures: MutableList<AuthorSubmissionFailure> = mutableListOf()
    var completedAt: Instant? = null

    /**
     * Sets [submissions] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun submissions(submissions: Iterable<Long>) {
        this.submissions = submissions.map { SubmissionId(it) }.toMutableList()
    }

    override fun build(): TaskValidationExecution = TaskValidationExecution.Completed(
        diagnostics = diagnostics.toList(),
        submissions = submissions.toList().lazify(),
        failures = failures.toList(),
        completedAt = requireField(completedAt) { ::completedAt },
    )
}

/**
 * Builder of [TaskValidationExecution.TechnicalFailure.IncompleteDiagnostics]. Required: [failure].
 *
 * @property failure the technical stop, or `null` if not set.
 * @since 1.0.0-beta.1
 */
class IncompleteDiagnosticsFailureBuilder : Builder<TaskValidationExecution> {
    var failure: TaskValidationTechnicalFailure? = null

    override fun build(): TaskValidationExecution = TaskValidationExecution.TechnicalFailure.IncompleteDiagnostics(
        failure = requireField(failure) { ::failure },
    )
}

/**
 * Builder of [TaskValidationExecution.TechnicalFailure.CompletedDiagnostics]. Required: [failure].
 *
 * @property diagnostics the results of the completed diagnostic stage.
 * @property failure the technical stop, or `null` if not set.
 * @since 1.0.0-beta.1
 */
class CompletedDiagnosticsFailureBuilder : Builder<TaskValidationExecution> {
    var diagnostics: MutableList<TestDiagnosticResult> = mutableListOf()
    var failure: TaskValidationTechnicalFailure? = null

    override fun build(): TaskValidationExecution = TaskValidationExecution.TechnicalFailure.CompletedDiagnostics(
        diagnostics = diagnostics.toList(),
        failure = requireField(failure) { ::failure },
    )
}

/**
 * Builder of [TaskValidationExecution.TechnicalFailure.CreatedSubmissions]. Required: [failure].
 *
 * @property diagnostics the results of the completed diagnostic stage.
 * @property submissions the identifiers of the created submissions.
 * @property failure the technical stop, or `null` if not set.
 * @since 1.0.0-beta.1
 */
class CreatedSubmissionsFailureBuilder : Builder<TaskValidationExecution> {
    var diagnostics: MutableList<TestDiagnosticResult> = mutableListOf()
    var submissions: MutableList<SubmissionId> = mutableListOf()
    var failure: TaskValidationTechnicalFailure? = null

    /**
     * Sets [submissions] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun submissions(submissions: Iterable<Long>) {
        this.submissions = submissions.map { SubmissionId(it) }.toMutableList()
    }

    override fun build(): TaskValidationExecution = TaskValidationExecution.TechnicalFailure.CreatedSubmissions(
        diagnostics = diagnostics.toList(),
        submissions = submissions.toList().lazify(),
        failure = requireField(failure) { ::failure },
    )
}
