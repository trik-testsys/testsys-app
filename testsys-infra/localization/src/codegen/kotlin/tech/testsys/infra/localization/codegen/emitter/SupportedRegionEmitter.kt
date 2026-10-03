package tech.testsys.infra.localization.codegen.emitter

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import tech.testsys.infra.localization.codegen.emitter.GeneratedTypes.Companion.GENERATED_FILE_HEADER
import tech.testsys.infra.localization.codegen.source.RegionDefinition

/** Emits the enum `SupportedRegion` with one constant per region and `toULocale`. */
internal object SupportedRegionEmitter {

    /** Emits `SupportedRegion` of the [types] package with [regions] in declaration order. */
    fun emit(types: GeneratedTypes, regions: List<RegionDefinition>): FileSpec {
        val enum = TypeSpec.enumBuilder(types.supportedRegion)
            .addKdoc("%L", GeneratedKdoc.SUPPORTED_REGION)
            .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "VERBOSE_DOC").build())
        regions.forEach { region ->
            enum.addEnumConstant(region.id, TypeSpec.anonymousClassBuilder().addKdoc("%L", GeneratedKdoc.region(region)).build())
        }
        enum.addFunction(toULocale(regions))
        return FileSpec.builder(types.supportedRegion).addFileComment(GENERATED_FILE_HEADER).addType(enum.build()).build()
    }

    private fun toULocale(regions: List<RegionDefinition>): FunSpec {
        val branches = CodeBlock.builder().add("return·when·(this)·{\n").indent()
        regions.forEach { branches.add("%N -> %T.forLanguageTag(%S)\n", it.id, GeneratedTypes.uLocale, it.languageTag) }
        return FunSpec.builder("toULocale")
            .addKdoc("%L", GeneratedKdoc.TO_ULOCALE)
            .returns(GeneratedTypes.uLocale)
            .addCode(branches.unindent().add("}\n").build())
            .build()
    }
}
