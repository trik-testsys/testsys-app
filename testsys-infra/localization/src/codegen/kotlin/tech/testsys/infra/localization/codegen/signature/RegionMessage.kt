package tech.testsys.infra.localization.codegen.signature

/**
 * The analysis of one key in one region.
 *
 * @property pattern the MF2 message as written in the region file.
 * @property placeholders the typed arguments in first-use order.
 * @property dateZones the zone of every date argument that does not use the context zone.
 */
internal data class RegionMessage(
    val pattern: String,
    val placeholders: Map<String, Placeholder>,
    val dateZones: Map<String, DateZoneSpec> = emptyMap(),
)

/**
 * A localization key whose signature has been merged across all regions.
 *
 * @property key the dotted key, e.g. `task.deadline.in_days`.
 * @property placeholders union of the arguments of all regions, keyed by name, in first-seen order.
 * @property patternsByRegion the MF2 message of every region.
 * @property dateZonesByRegion the non-context date zones of every region that has any.
 */
internal data class MergedKey(
    val key: String,
    val placeholders: Map<String, Placeholder>,
    val patternsByRegion: Map<String, String> = emptyMap(),
    val dateZonesByRegion: Map<String, Map<String, DateZoneSpec>> = emptyMap(),
)
