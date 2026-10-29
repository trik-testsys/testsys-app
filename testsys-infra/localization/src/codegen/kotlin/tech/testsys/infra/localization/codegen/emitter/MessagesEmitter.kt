package tech.testsys.infra.localization.codegen.emitter

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import tech.testsys.infra.localization.codegen.emitter.GeneratedTypes.Companion.GENERATED_FILE_HEADER
import tech.testsys.infra.localization.codegen.mf2.validator.term.Glossary
import tech.testsys.infra.localization.codegen.signature.DateZoneSpec
import tech.testsys.infra.localization.codegen.signature.MergedKey
import tech.testsys.infra.localization.codegen.source.RegionDefinition

/**
 * Emits the internal object with the MF2 messages, terms and date zones of every region and one lazily created
 * runtime per region; the runtime never reads `.properties` files.
 */
internal object MessagesEmitter {

    /**
     * Emits the messages object of the [types] package with the messages of [regions] from [merged] and the terms of
     * [termsByRegion] (region → name → message).
     */
    fun emit(
        types: GeneratedTypes,
        regions: List<RegionDefinition>,
        merged: List<MergedKey>,
        termsByRegion: Map<String, Map<String, String>>,
    ): FileSpec {
        val cls = TypeSpec.objectBuilder(types.messages)
            .addAnnotation(GeneratedTypes.internalApiAnnotation)
            .addModifiers(KModifier.INTERNAL)
            .addKdoc("%L", GeneratedKdoc.MESSAGES)
        val messages = merged.filterNot { it.key.startsWith(Glossary.GLOSSARY_PREFIX) }
        regions.forEach { region ->
            val initializer = runtimeInitializer(types, region, messages, termsByRegion[region.id].orEmpty())
            cls.addProperty(
                PropertySpec.builder(runtimeProperty(region), GeneratedTypes.runtime, KModifier.PRIVATE).delegate(initializer).build(),
            )
        }
        cls.addFunction(runtimeOf(types, regions))
        return FileSpec.builder(types.messages).addFileComment(GENERATED_FILE_HEADER).addType(cls.build()).build()
    }

    private fun runtimeInitializer(
        types: GeneratedTypes,
        region: RegionDefinition,
        messages: List<MergedKey>,
        terms: Map<String, String>,
    ): CodeBlock {
        val patterns = messages.mapNotNull { key -> key.patternsByRegion[region.id]?.let { pattern -> key.key to pattern } }
        return CodeBlock.builder()
            .add("lazy {\n").indent()
            .add("%T(\n", GeneratedTypes.runtime).indent()
            .add("regionId = %S,\n", region.id)
            .add("locale = %T.%N.toULocale(),\n", types.supportedRegion, region.id)
            .add("patterns = %L,\n", stringMap(patterns))
            .add("terms = %L,\n", stringMap(terms.toSortedMap().toList()))
            .add("dateZones = %L,\n", dateZones(region.id, messages))
            .unindent().add(")\n")
            .unindent().add("}")
            .build()
    }

    private fun runtimeOf(types: GeneratedTypes, regions: List<RegionDefinition>): FunSpec {
        val branches = CodeBlock.builder().add("return·when·(region)·{\n").indent()
        regions.forEach { branches.add("%T.%N -> %N\n", types.supportedRegion, it.id, runtimeProperty(it)) }
        return FunSpec.builder("runtime")
            .addModifiers(KModifier.INTERNAL)
            .addKdoc("%L", GeneratedKdoc.RUNTIME_OF_REGION)
            .addParameter("region", types.supportedRegion)
            .returns(GeneratedTypes.runtime)
            .addCode(branches.unindent().add("}\n").build())
            .build()
    }

    private fun stringMap(entries: List<Pair<String, String>>): CodeBlock {
        if (entries.isEmpty()) return CodeBlock.of("emptyMap()")
        val builder = CodeBlock.builder().add("%M(\n", GeneratedTypes.mapOf).indent()
        entries.forEach { (key, value) -> builder.add("%S·to·%S,\n", key, value) }
        return builder.unindent().add(")").build()
    }

    private fun dateZones(region: String, keys: List<MergedKey>): CodeBlock {
        val entries = keys.mapNotNull { key -> key.dateZonesByRegion[region]?.let { zones -> key.key to zones } }
        if (entries.isEmpty()) return CodeBlock.of("emptyMap()")
        val builder = CodeBlock.builder().add("%M(\n", GeneratedTypes.mapOf).indent()
        entries.forEach { (key, zones) ->
            val zoneEntries = zones.entries.map { (argument, spec) -> dateZone(argument, spec) }
            builder.add("%S·to·%M(%L),\n", key, GeneratedTypes.mapOf, zoneEntries.joinToCode(",·"))
        }
        return builder.unindent().add(")").build()
    }

    private fun dateZone(argument: String, spec: DateZoneSpec): CodeBlock = when (spec) {
        is DateZoneSpec.Fixed ->
            CodeBlock.of("%S·to·%T.Fixed(%T.of(%S))", argument, GeneratedTypes.dateZone, GeneratedTypes.zoneId, spec.zoneId)
        is DateZoneSpec.Argument -> CodeBlock.of("%S·to·%T.Argument(%S)", argument, GeneratedTypes.dateZone, spec.argument)
    }

    private fun runtimeProperty(region: RegionDefinition): String = region.id.lowercase() + "Runtime"
}
