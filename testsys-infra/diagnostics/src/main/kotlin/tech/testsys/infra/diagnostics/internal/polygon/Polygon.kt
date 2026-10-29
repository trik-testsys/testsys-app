package tech.testsys.infra.diagnostics.internal.polygon

import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi

/**
 * Region identifiers from the [world XML example](https://help.trikset.com/studio/2d-model/restrictions#primer-dobavleniya-ogranichenii).
 */
@InternalDiagnosticsApi
internal data class PolygonWorld(val regions: List<Located<String>>)

/**
 * The [constraint container](https://help.trikset.com/studio/2d-model/restrictions#struktura-napisaniya-ogranichenii).
 */
@InternalDiagnosticsApi
internal data class PolygonConstraints(val elements: List<Located<PolygonElement>>)

/**
 * Supported constructs of [TRIK Studio world XML](https://help.trikset.com/studio/2d-model/restrictions).
 */
@InternalDiagnosticsApi
internal data class Polygon(val world: PolygonWorld, val constraints: PolygonConstraints)

/**
 * A supported [constraint, condition or trigger](https://help.trikset.com/studio/2d-model/restrictions).
 */
@InternalDiagnosticsApi
internal sealed interface PolygonElement {
    /**
     * A [time limit](https://help.trikset.com/studio/2d-model/restrictions#less-than-timelimit-greater-than).
     */
    @InternalDiagnosticsApi
    data class TimeLimit(val milliseconds: Long) : PolygonElement

    /**
     * An [event](https://help.trikset.com/studio/2d-model/restrictions#event) with an optional identifier.
     */
    @InternalDiagnosticsApi
    data class Event(val id: String?, val body: List<Located<PolygonElement>>) : PolygonElement

    /**
     * An event reference in [atomic conditions](https://help.trikset.com/studio/2d-model/restrictions#atomarnye-usloviya)
     * or [setUp/drop triggers](https://help.trikset.com/studio/2d-model/restrictions#setup).
     */
    @InternalDiagnosticsApi
    data class EventReference(val id: String) : PolygonElement

    /**
     * A console [message](https://help.trikset.com/studio/2d-model/restrictions#less-than-message-greater-than).
     */
    @InternalDiagnosticsApi
    data class Message(val text: String, val replacements: List<Located<PolygonElement>>) : PolygonElement

    /**
     * Supported [conditions](https://help.trikset.com/studio/2d-model/restrictions#usloviya),
     * [triggers](https://help.trikset.com/studio/2d-model/restrictions#triggery) and expressions.
     */
    @InternalDiagnosticsApi
    data class Construct(
        val tag: String,
        val attributes: Map<String, String>,
        val children: List<Located<PolygonElement>>,
    ) : PolygonElement
}
