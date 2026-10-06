package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.DiagnosticData

/**
 * DSL chooser of a typed polygon diagnostic reason.
 *
 * @since %CURRENT_VERSION%
 */
class DiagnosticDataChooser : Chooser<DiagnosticData>() {
    /**
     * Selects [DiagnosticData.UnknownElement] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun unknownElement(builder: UnknownElementDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? UnknownElementDiagnosticDataBuilder ?: UnknownElementDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.MalformedXml] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun malformedXml(builder: MalformedXmlDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? MalformedXmlDiagnosticDataBuilder ?: MalformedXmlDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.MissingChild] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun missingChild(builder: MissingChildDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? MissingChildDiagnosticDataBuilder ?: MissingChildDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.MissingAttribute] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun missingAttribute(builder: MissingAttributeDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? MissingAttributeDiagnosticDataBuilder ?: MissingAttributeDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.InvalidAttributeValue] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun invalidAttributeValue(builder: InvalidAttributeValueDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? InvalidAttributeValueDiagnosticDataBuilder ?: InvalidAttributeValueDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.InvalidChildCount] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun invalidChildCount(builder: InvalidChildCountDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? InvalidChildCountDiagnosticDataBuilder ?: InvalidChildCountDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.MissingTimeLimit].
     *
     * @since %CURRENT_VERSION%
     */
    fun missingTimeLimit() = makeChoice(object : Builder<DiagnosticData> {
        override fun build() = DiagnosticData.MissingTimeLimit
    })

    /**
     * Selects [DiagnosticData.MultipleTimeLimits] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun multipleTimeLimits(builder: MultipleTimeLimitsDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? MultipleTimeLimitsDiagnosticDataBuilder ?: MultipleTimeLimitsDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.NegativeTimeLimit] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun negativeTimeLimit(builder: NegativeTimeLimitDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? NegativeTimeLimitDiagnosticDataBuilder ?: NegativeTimeLimitDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.ExcessiveTimeLimit] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun excessiveTimeLimit(builder: ExcessiveTimeLimitDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? ExcessiveTimeLimitDiagnosticDataBuilder ?: ExcessiveTimeLimitDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.InvalidEventId] configured by [builder].
     *
     * @since %CURRENT_VERSION%
     */
    fun invalidEventId(builder: InvalidEventIdDiagnosticDataBuilder.() -> Unit) {
        val current = choice as? InvalidEventIdDiagnosticDataBuilder ?: InvalidEventIdDiagnosticDataBuilder()
        makeChoice(current.apply(builder))
    }

    /**
     * Selects [DiagnosticData.MissingScoreOutput].
     *
     * @since %CURRENT_VERSION%
     */
    fun missingScoreOutput() = makeChoice(object : Builder<DiagnosticData> {
        override fun build() = DiagnosticData.MissingScoreOutput
    })
}

/**
 * Builder of [DiagnosticData.UnknownElement]. Required: [tag].
 *
 * @property tag the unknown tag, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class UnknownElementDiagnosticDataBuilder : Builder<DiagnosticData> {
    var tag: String? = null
    override fun build(): DiagnosticData = DiagnosticData.UnknownElement(
        tag = requireField(tag) { ::tag },
    )
}

/**
 * Builder of [DiagnosticData.MalformedXml]. Required: [details].
 *
 * @property details the XML syntax error, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class MalformedXmlDiagnosticDataBuilder : Builder<DiagnosticData> {
    var details: String? = null
    override fun build(): DiagnosticData = DiagnosticData.MalformedXml(
        details = requireField(details) { ::details },
    )
}

/**
 * Builder of [DiagnosticData.MissingChild]. Required: [tag].
 *
 * @property tag the required child tag, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class MissingChildDiagnosticDataBuilder : Builder<DiagnosticData> {
    var tag: String? = null
    override fun build(): DiagnosticData = DiagnosticData.MissingChild(
        tag = requireField(tag) { ::tag },
    )
}

/**
 * Builder of [DiagnosticData.MissingAttribute]. Required: [attribute].
 *
 * @property attribute the required attribute, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class MissingAttributeDiagnosticDataBuilder : Builder<DiagnosticData> {
    var attribute: String? = null
    override fun build(): DiagnosticData = DiagnosticData.MissingAttribute(
        attribute = requireField(attribute) { ::attribute },
    )
}

/**
 * Builder of [DiagnosticData.InvalidAttributeValue]. Required: [attribute], [expected], [actual].
 *
 * @property attribute the attribute, or `null` if not set.
 * @property expected the required value type, or `null` if not set.
 * @property actual the supplied value, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class InvalidAttributeValueDiagnosticDataBuilder : Builder<DiagnosticData> {
    var attribute: String? = null
    var expected: String? = null
    var actual: String? = null
    override fun build(): DiagnosticData = DiagnosticData.InvalidAttributeValue(
        attribute = requireField(attribute) { ::attribute },
        expected = requireField(expected) { ::expected },
        actual = requireField(actual) { ::actual },
    )
}

/**
 * Builder of [DiagnosticData.InvalidChildCount]. Required: [expected], [actual].
 *
 * @property expected the required child count, or `null` if not set.
 * @property actual the supplied count, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class InvalidChildCountDiagnosticDataBuilder : Builder<DiagnosticData> {
    var expected: Int? = null
    var actual: Int? = null
    override fun build(): DiagnosticData = DiagnosticData.InvalidChildCount(
        expected = requireField(expected) { ::expected },
        actual = requireField(actual) { ::actual },
    )
}

/**
 * Builder of [DiagnosticData.MultipleTimeLimits]. Required: [count].
 *
 * @property count the number of limits, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class MultipleTimeLimitsDiagnosticDataBuilder : Builder<DiagnosticData> {
    var count: Int? = null
    override fun build(): DiagnosticData = DiagnosticData.MultipleTimeLimits(
        count = requireField(count) { ::count },
    )
}

/**
 * Builder of [DiagnosticData.NegativeTimeLimit]. Required: [value].
 *
 * @property value the negative limit, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class NegativeTimeLimitDiagnosticDataBuilder : Builder<DiagnosticData> {
    var value: Long? = null
    override fun build(): DiagnosticData = DiagnosticData.NegativeTimeLimit(
        value = requireField(value) { ::value },
    )
}

/**
 * Builder of [DiagnosticData.ExcessiveTimeLimit]. Required: [value], [maximum].
 *
 * @property value the limit, or `null` if not set.
 * @property maximum the configured maximum, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class ExcessiveTimeLimitDiagnosticDataBuilder : Builder<DiagnosticData> {
    var value: Long? = null
    var maximum: Long? = null
    override fun build(): DiagnosticData = DiagnosticData.ExcessiveTimeLimit(
        value = requireField(value) { ::value },
        maximum = requireField(maximum) { ::maximum },
    )
}

/**
 * Builder of [DiagnosticData.InvalidEventId]. Required: [id].
 *
 * @property id the unresolved event identifier, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class InvalidEventIdDiagnosticDataBuilder : Builder<DiagnosticData> {
    var id: String? = null
    override fun build(): DiagnosticData = DiagnosticData.InvalidEventId(
        id = requireField(id) { ::id },
    )
}
