package tech.testsys.infra.localization.codegen.emitter

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import tech.testsys.infra.localization.codegen.emitter.GeneratedTypes.Companion.GENERATED_FILE_HEADER

/** Emits `Localization`, the entry point with one property per bundle and `forRegion`. */
internal object LocalizationEmitter {

    /** Emits `Localization` of the [types] package with the [bundleClasses]. */
    fun emit(types: GeneratedTypes, bundleClasses: Set<String>): FileSpec {
        val constructor = FunSpec.constructorBuilder().addModifiers(KModifier.PRIVATE)
        val cls = TypeSpec.classBuilder(types.localization).addKdoc("%L", GeneratedKdoc.localization(bundleClasses))
        bundleClasses.forEach { name ->
            val type = types.bundle(name)
            val property = Naming.bundlePropertyName(name)
            constructor.addParameter(property, type)
            cls.addProperty(PropertySpec.builder(property, type).initializer(property).build())
        }
        cls.primaryConstructor(constructor.build())
            .addType(TypeSpec.companionObjectBuilder().addFunction(forRegion(types, bundleClasses)).build())
        return FileSpec.builder(types.localization).addFileComment(GENERATED_FILE_HEADER).addType(cls.build()).build()
    }

    private fun forRegion(types: GeneratedTypes, bundleClasses: Set<String>): FunSpec {
        val body = CodeBlock.builder()
        if (bundleClasses.isNotEmpty()) body.addStatement("val runtime = %T.runtime(region)", types.messages)
        body.add("return·%T(\n", types.localization).indent()
        bundleClasses.forEach { name ->
            body.add("%N = %T(runtime, timeZone),\n", Naming.bundlePropertyName(name), types.bundle(name))
        }
        body.unindent().add(")\n")
        return FunSpec.builder("forRegion")
            .addKdoc("%L", GeneratedKdoc.FOR_REGION)
            .addAnnotation(GeneratedTypes.optInInternalApi)
            .addParameter("region", types.supportedRegion)
            .addParameter("timeZone", GeneratedTypes.zoneId)
            .returns(types.localization)
            .addCode(body.build())
            .build()
    }
}
