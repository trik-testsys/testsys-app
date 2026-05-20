package tech.testsys.infra.localization.codegen.parser

/**
 * Builds one [MergedKey] per localization key from per-region pattern maps.
 *
 * Cross-region invariants enforced here:
 *  - every key contains at least one dot (the dot separates bundle class from method name);
 *  - every key is present in every region (no silent fallbacks);
 *  - argument kinds are compatible across regions (see [mergePlaceholders]).
 *
 * Errors are accumulated rather than thrown eagerly so a single run reports every
 * problem at once — important for codegen, where each Gradle invocation is expensive
 * and fixing one error at a time would be painful.
 */
internal class SignatureMerger(
    private val analyzer: MessagePatternAnalyzer = MessagePatternAnalyzer(),
) {

    fun merge(perRegion: Map<String, Map<String, String>>): List<MergedKey> {
        if (perRegion.isEmpty()) return emptyList()

        // Sorted union of every key seen in any region — drives both validation and merge order.
        val allKeys = perRegion.values.flatMapTo(sortedSetOf()) { it.keys }
        val errors = mutableListOf<String>()
        errors += keyShapeErrors(allKeys)
        errors += missingKeyErrors(perRegion, allKeys)

        val merged = allKeys.map { key -> mergeOne(key, perRegion, errors) }

        if (errors.isNotEmpty()) {
            error("Localization codegen errors:\n  - " + errors.joinToString("\n  - "))
        }
        return merged
    }

    private fun keyShapeErrors(allKeys: Set<String>): List<String> =
        allKeys
            .filterNot { it.contains('.') }
            .map { "Key '$it' must contain at least one dot." }

    private fun missingKeyErrors(
        perRegion: Map<String, Map<String, String>>,
        allKeys: Set<String>,
    ): List<String> = perRegion.mapNotNull { (region, msgs) ->
        val missing = allKeys - msgs.keys
        if (missing.isEmpty()) null
        else "Region '$region' is missing keys: ${missing.joinToString(", ")}"
    }

    /**
     * Folds across regions for one key, accumulating both the per-region pattern
     * map and the union of placeholders. Errors flow into the shared [errors] sink
     * so the whole codegen run still surfaces every problem at once.
     */
    private fun mergeOne(
        key: String,
        perRegion: Map<String, Map<String, String>>,
        errors: MutableList<String>,
    ): MergedKey {
        val patterns = sortedMapOf<String, String>()
        val union = perRegion.entries.fold(linkedMapOf<String, Placeholder>()) { acc, (region, msgs) ->
            // A missing pattern is already reported in `missingKeyErrors`; skip silently
            // here so we keep collecting other diagnostics instead of cascading NPEs.
            val pattern = msgs[key] ?: return@fold acc
            patterns[region] = pattern
            val parsed = runCatching { analyzer.analyze(pattern) }
                .onFailure { errors += "Failed to parse '$region' / '$key': ${it.message}" }
                .getOrNull()
                ?: return@fold acc
            mergeInto(acc, parsed, key, region, errors)
        }
        return MergedKey(key, union, patterns)
    }

    private fun mergeInto(
        acc: LinkedHashMap<String, Placeholder>,
        parsed: Map<String, Placeholder>,
        key: String,
        region: String,
        errors: MutableList<String>,
    ): LinkedHashMap<String, Placeholder> = acc.apply {
        parsed.forEach { (name, ph) ->
            val existing = get(name)
            put(name, if (existing == null) ph else widenOrKeep(existing, ph, key, name, region, errors))
        }
    }

    private fun widenOrKeep(
        existing: Placeholder,
        incoming: Placeholder,
        key: String,
        name: String,
        region: String,
        errors: MutableList<String>,
    ): Placeholder = runCatching { mergePlaceholders(existing, incoming) }
        .onFailure { errors += "Key '$key' arg '$name' (in region '$region'): ${it.message}" }
        // Keep the previous placeholder on conflict so later regions still merge against
        // a coherent signature instead of producing duplicate errors.
        .getOrDefault(existing)
}
