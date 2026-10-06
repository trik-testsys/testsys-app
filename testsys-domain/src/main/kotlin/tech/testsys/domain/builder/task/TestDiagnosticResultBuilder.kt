package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.chooser.DiagnosticDataChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.*
import java.time.Instant

/**
 * Builder of [TestDiagnosticResult]. Required: [testId].
 *
 * @property testId the concrete polygon identifier, or `null` if not set.
 * @property reports the diagnostic messages; an empty list represents completed analysis without messages.
 * @since %CURRENT_VERSION%
 */
class TestDiagnosticResultBuilder : Builder<TestDiagnosticResult> {
    var testId: TestId? = null
    var reports: MutableList<DiagnosticReport> = mutableListOf()

    /**
     * Sets [testId] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun testId(testId: Long) { this.testId = TestId(testId) }

    override fun build(): TestDiagnosticResult = TestDiagnosticResult(
        testId = requireField(testId) { ::testId },
        reports = reports.toList(),
    )
}

/**
 * Builder of [DiagnosticReport]. Required: [severity] and a choice in [data].
 *
 * @property severity the level, or `null` if not set.
 * @property data the reason chooser.
 * @property location the XML location, or `null` for a file message.
 * @since %CURRENT_VERSION%
 */
class DiagnosticReportBuilder : Builder<DiagnosticReport> {
    var severity: DiagnosticSeverity? = null
    val data = DiagnosticDataChooser()
    var location: DiagnosticLocation? = null
    override fun build(): DiagnosticReport = DiagnosticReport(
        severity = requireField(severity) { ::severity },
        data = data.build(),
        location = location,
    )
}

/**
 * Builder of [DiagnosticLocation].
 *
 * @property path the indexed XML element path.
 * @property attribute the attribute name, or `null` for an element.
 * @since %CURRENT_VERSION%
 */
class DiagnosticLocationBuilder : Builder<DiagnosticLocation> {
    var path: MutableList<DiagnosticPathSegment> = mutableListOf()
    var attribute: String? = null

    override fun build(): DiagnosticLocation = DiagnosticLocation(
        path = path.toList(),
        attribute = attribute,
    )
}

/**
 * Builder of [DiagnosticPathSegment]. Required: [tag], [index].
 *
 * @property tag the element tag, or `null` if not set.
 * @property index the one-based same-tag sibling index, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class DiagnosticPathSegmentBuilder : Builder<DiagnosticPathSegment> {
    var tag: String? = null
    var index: Int? = null

    override fun build(): DiagnosticPathSegment = DiagnosticPathSegment(
        tag = requireField(tag) { ::tag },
        index = requireField(index) { ::index },
    )
}

/**
 * Builder of [TaskValidationTechnicalFailure]. Required: [description], [occurredAt].
 *
 * @property description the technical failure details, or `null` if not set.
 * @property occurredAt the recorded failure moment, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class TaskValidationTechnicalFailureBuilder : Builder<TaskValidationTechnicalFailure> {
    var description: String? = null
    var occurredAt: Instant? = null

    override fun build(): TaskValidationTechnicalFailure = TaskValidationTechnicalFailure(
        description = requireField(description) { ::description },
        occurredAt = requireField(occurredAt) { ::occurredAt },
    )
}
