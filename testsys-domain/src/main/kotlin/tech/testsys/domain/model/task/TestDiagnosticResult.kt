package tech.testsys.domain.model.task

/**
 * Severity of a polygon diagnostic message.
 *
 * @since 1.0.0-beta.1
 */
enum class DiagnosticSeverity {
    /**
     * An informational message.
     *
     * @since 1.0.0-beta.1
     */
    Info,

    /**
     * A warning message.
     *
     * @since 1.0.0-beta.1
     */
    Warning,

    /**
     * An error message.
     *
     * @since 1.0.0-beta.1
     */
    Error,
}

/**
 * A single indexed element in an XML location.
 *
 * @property tag the element tag.
 * @property index the one-based index among same-tag siblings.
 * @since 1.0.0-beta.1
 */
data class DiagnosticPathSegment(val tag: String, val index: Int)

/**
 * The location of a concrete XML node or attribute.
 *
 * @property path the indexed element path.
 * @property attribute the attribute name, or `null` for an element.
 * @since 1.0.0-beta.1
 */
data class DiagnosticLocation(val path: List<DiagnosticPathSegment>, val attribute: String?)

/**
 * Typed reasons and parameters of polygon diagnostic messages.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DiagnosticData {
    /**
     * The unknown element diagnostic reason.
     *
     * @property tag the unknown tag.
     * @since 1.0.0-beta.1
     */
    data class UnknownElement(val tag: String) : DiagnosticData

    /**
     * The malformed XML diagnostic reason.
     *
     * @property details the XML syntax error.
     * @since 1.0.0-beta.1
     */
    data class MalformedXml(val details: String) : DiagnosticData

    /**
     * The missing child diagnostic reason.
     *
     * @property tag the required child tag.
     * @since 1.0.0-beta.1
     */
    data class MissingChild(val tag: String) : DiagnosticData

    /**
     * The missing attribute diagnostic reason.
     *
     * @property attribute the required attribute.
     * @since 1.0.0-beta.1
     */
    data class MissingAttribute(val attribute: String) : DiagnosticData

    /**
     * The invalid attribute value diagnostic reason.
     *
     * @property attribute the attribute.
     * @property expected the required value type.
     * @property actual the supplied value.
     * @since 1.0.0-beta.1
     */
    data class InvalidAttributeValue(val attribute: String, val expected: String, val actual: String) : DiagnosticData

    /**
     * The invalid child count diagnostic reason.
     *
     * @property expected the required child count.
     * @property actual the supplied count.
     * @since 1.0.0-beta.1
     */
    data class InvalidChildCount(val expected: Int, val actual: Int) : DiagnosticData

    /**
     * The missing time limit diagnostic reason.
     *
     * @since 1.0.0-beta.1
     */
    data object MissingTimeLimit : DiagnosticData

    /**
     * The multiple time limits diagnostic reason.
     *
     * @property count the number of limits.
     * @since 1.0.0-beta.1
     */
    data class MultipleTimeLimits(val count: Int) : DiagnosticData

    /**
     * The negative time limit diagnostic reason.
     *
     * @property value the negative limit.
     * @since 1.0.0-beta.1
     */
    data class NegativeTimeLimit(val value: Long) : DiagnosticData

    /**
     * The excessive time limit diagnostic reason.
     *
     * @property value the limit.
     * @property maximum the configured maximum.
     * @since 1.0.0-beta.1
     */
    data class ExcessiveTimeLimit(val value: Long, val maximum: Long) : DiagnosticData

    /**
     * The invalid event id diagnostic reason.
     *
     * @property id the unresolved event identifier.
     * @since 1.0.0-beta.1
     */
    data class InvalidEventId(val id: String) : DiagnosticData

    /**
     * The missing score output diagnostic reason.
     *
     * @since 1.0.0-beta.1
     */
    data object MissingScoreOutput : DiagnosticData
}

/**
 * A typed diagnostic message for a polygon.
 *
 * @property severity the message severity.
 * @property data the reason and its parameters.
 * @property location the XML location, or `null` for a whole-file message.
 * @since 1.0.0-beta.1
 */
data class DiagnosticReport(
    val severity: DiagnosticSeverity,
    val data: DiagnosticData,
    val location: DiagnosticLocation?,
)

/**
 * Diagnostic results of one polygon of a [TaskValidationRequest].
 *
 * @property testId the concrete polygon version.
 * @property reports the messages, including an empty list after successful analysis.
 * @since 1.0.0-beta.1
 */
data class TestDiagnosticResult(val testId: TestId, val reports: List<DiagnosticReport>)
