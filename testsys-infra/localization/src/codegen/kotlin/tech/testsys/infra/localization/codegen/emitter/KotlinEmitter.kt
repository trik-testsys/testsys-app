package tech.testsys.infra.localization.codegen.emitter

import com.squareup.kotlinpoet.FileSpec
import tech.testsys.infra.localization.codegen.mf2.validator.term.Glossary
import tech.testsys.infra.localization.codegen.signature.MergedKey
import tech.testsys.infra.localization.codegen.source.RegionDefinition

/**
 * Renders the merged keys into KotlinPoet [FileSpec]s: one bundle class per key prefix, the `Localization` entry
 * point, an internal object with the MF2 messages and terms of every region and `SupportedRegion`.
 */
internal object KotlinEmitter {
    /**
     * Emits the API of the [types] package for [regions] (in declaration order), the non-glossary keys of [merged]
     * and the terms of [termsByRegion] (region → term name → MF2 message).
     */
    fun emit(
        types: GeneratedTypes,
        regions: List<RegionDefinition>,
        merged: List<MergedKey>,
        termsByRegion: Map<String, Map<String, String>>,
    ): List<FileSpec> {
        val byClass = merged
            .filterNot { it.key.startsWith(Glossary.GLOSSARY_PREFIX) }
            .groupBy { Naming.classNameFor(it.key) }
            .toSortedMap()
        return byClass.map { (className, keys) -> BundleEmitter.emit(types, className, keys) } +
            LocalizationEmitter.emit(types, byClass.keys) +
            MessagesEmitter.emit(types, regions, merged, termsByRegion) +
            SupportedRegionEmitter.emit(types, regions)
    }
}
