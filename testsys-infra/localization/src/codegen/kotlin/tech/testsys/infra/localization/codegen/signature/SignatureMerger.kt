package tech.testsys.infra.localization.codegen.signature

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.messageError

/**
 * Builds one [MergedKey] per localization key from the per-region analyses.
 *
 * Cross-region invariants enforced here:
 *  - every key is present in every region (no silent fallbacks);
 *  - argument types are compatible across regions (see [mergePlaceholders]); `:string` selector keys form the
 *    union of the generated enum.
 *
 * Errors are accumulated rather than thrown so a single run reports every problem at once.
 */
internal class SignatureMerger {

    /**
     * Merges [perRegion] (region → key → analysis, or `null` for a key whose analysis failed) in the order of
     * [regions], reporting problems into [errors]. The keys of the [missingBundles] of a region (region → bundles
     * whose file the region lacks) are not reported as missing: the missing file is reported once instead.
     */
    fun merge(
        regions: List<String>,
        perRegion: Map<String, Map<String, RegionMessage?>>,
        errors: MutableList<String>,
        missingBundles: Map<String, Set<String>> = emptyMap(),
    ): List<MergedKey> {
        val allKeys = perRegion.values.flatMapTo(sortedSetOf()) { it.keys }
        regions.forEach { region ->
            val missing = allKeys - perRegion[region].orEmpty().keys
            missing.filter { it.substringBefore('.') !in missingBundles[region].orEmpty() }.forEach { key ->
                val definedIn = regions.filter { key in perRegion[it].orEmpty() }.joinToString()
                errors += messageError(region, key, Problems.Signatures.missingKey(definedIn))
            }
        }
        return allKeys.map { key -> mergeOne(key, regions, perRegion, errors) }
    }

    private fun mergeOne(
        key: String,
        regions: List<String>,
        perRegion: Map<String, Map<String, RegionMessage?>>,
        errors: MutableList<String>,
    ): MergedKey {
        val patterns = linkedMapOf<String, String>()
        val zones = linkedMapOf<String, Map<String, DateZoneSpec>>()
        val union = linkedMapOf<String, Placeholder>()
        val firstSeen = mutableMapOf<String, String>()
        regions.forEach { region ->
            val message = perRegion[region]?.get(key) ?: return@forEach
            patterns[region] = message.pattern
            if (message.dateZones.isNotEmpty()) zones[region] = message.dateZones
            message.placeholders.forEach { (name, placeholder) ->
                val existing = union[name]
                if (existing == null) {
                    union[name] = placeholder
                    firstSeen[name] = region
                } else {
                    // Keep the previous placeholder on conflict so later regions merge against a coherent signature.
                    when (val merged = mergePlaceholders(existing, placeholder)) {
                        null -> errors += conflict(region, key, existing, placeholder, firstSeen.getValue(name))
                        else -> union[name] = merged
                    }
                }
            }
        }
        return MergedKey(key, union, patterns, zones)
    }

    private fun conflict(region: String, key: String, existing: Placeholder, placeholder: Placeholder, firstSeen: String): String {
        val problem = Problems.Signatures.conflictingTypes(existing.name, existing.typeName, placeholder.typeName)
        return messageError(region, key, Problems.Signatures.inRegion(problem, firstSeen))
    }
}
