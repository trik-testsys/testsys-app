package tech.testsys.infra.localization.codegen.parser

/**
 * Categorized placeholder extracted from one or more ICU patterns for the same key.
 *
 * Subtype encodes the Kotlin parameter type the emitter should pick — one ICU
 * argument shape per subclass:
 *  - [StringPlaceholder] — bare `{name}` or unrecognized SIMPLE subtype.
 *  - [NumberPlaceholder] — `{name, number, …}`.
 *  - [IntPlaceholder] — `plural`, `selectordinal`, `spellout`, `ordinal`.
 *  - [InstantPlaceholder] — `date` / `time`, bound to [java.time.Instant].
 *  - [SelectPlaceholder] — `select` (carries the union of branch labels seen).
 */
internal sealed class Placeholder(val name: String) {
    class StringPlaceholder(name: String) : Placeholder(name)
    class NumberPlaceholder(name: String) : Placeholder(name)
    class IntPlaceholder(name: String) : Placeholder(name)
    class InstantPlaceholder(name: String) : Placeholder(name)

    /**
     * @property selectVariants ICU `select` branch labels seen across regions,
     *   excluding the implicit `other` branch (the emitter adds it unconditionally
     *   since ICU requires it as a fallback).
     */
    class SelectPlaceholder(name: String, val selectVariants: Set<String>) : Placeholder(name)
}

/**
 * A localization key whose placeholder signature has been merged across all regions.
 *
 * @property key the original dotted key, e.g. `task.deadline.in_days`.
 * @property placeholders union of placeholders seen across regions, keyed by name.
 *   Iteration order follows first-seen order, which the emitter uses for the
 *   generated parameter list.
 * @property patternsByRegion raw ICU patterns per region — kept around so the
 *   emitter can render them into the generated method's KDoc and the runtime
 *   patterns table.
 */
internal data class MergedKey(
    val key: String,
    val placeholders: Map<String, Placeholder>,
    val patternsByRegion: Map<String, String> = emptyMap(),
)
