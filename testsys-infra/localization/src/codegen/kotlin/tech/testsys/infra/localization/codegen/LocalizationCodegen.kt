package tech.testsys.infra.localization.codegen

import com.squareup.kotlinpoet.FileSpec
import tech.testsys.infra.localization.codegen.emitter.GeneratedTypes
import tech.testsys.infra.localization.codegen.emitter.KotlinEmitter
import tech.testsys.infra.localization.codegen.emitter.NameValidator
import tech.testsys.infra.localization.codegen.mf2.validator.RegionAnalysis
import tech.testsys.infra.localization.codegen.mf2.validator.RegionAnalyzer
import tech.testsys.infra.localization.codegen.signature.SignatureMerger
import tech.testsys.infra.localization.codegen.source.RegionDefinition
import tech.testsys.infra.localization.codegen.source.RegionFiles

/**
 * The localization codegen pipeline: regions → files → parse and analyze each region → merge regions → check
 * names → emit. Every problem of a run is thrown together as a [LocalizationCodegenException].
 */
internal object LocalizationCodegen {
    // It works on file contents only, so tests run it without a file system.

    /**
     * Generates the API in [packageName] from [files] (path relative to one localization resource directory →
     * content).
     */
    fun generate(files: Map<String, ByteArray>, packageName: String = GeneratedTypes.PRODUCT_PACKAGE): List<FileSpec> {
        val errors = mutableListOf<String>()
        val regions = RegionFiles.loadRegions(files, errors)
        errors += RegionFiles.fileSetProblems(regions, files)
        val analyses = analyzeRegions(regions, files, errors)
        val loaded = regions.filter { it.id in analyses }.map { it.id }
        val missingBundles = RegionFiles.missingBundles(regions, files)
        val merged = SignatureMerger().merge(loaded, analyses.mapValues { it.value.messages }, errors, missingBundles)
        errors += NameValidator.findProblems(merged) { key -> loaded.first { region -> key in analyses.getValue(region).messages } }
        if (errors.isNotEmpty()) throw LocalizationCodegenException(errors)
        return KotlinEmitter.emit(GeneratedTypes(packageName), regions, merged, analyses.mapValues { it.value.terms })
    }

    // One region at a time, so the errors of a region stay together.
    private fun analyzeRegions(
        regions: List<RegionDefinition>,
        files: Map<String, ByteArray>,
        errors: MutableList<String>,
    ): Map<String, RegionAnalysis> {
        val analyses = linkedMapOf<String, RegionAnalysis>()
        regions.forEach { region ->
            val entries = RegionFiles.loadEntries(region, files, errors) ?: return@forEach
            analyses[region.id] = RegionAnalyzer.analyze(region, entries, errors)
        }
        return analyses
    }
}
