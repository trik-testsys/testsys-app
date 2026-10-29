package tech.testsys.infra.localization.codegen.emitter

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import tech.testsys.infra.localization.codegen.emitter.GeneratedTypes.Companion.GENERATED_FILE_HEADER
import tech.testsys.infra.localization.codegen.signature.MergedKey
import tech.testsys.infra.localization.codegen.signature.Placeholder
import tech.testsys.infra.localization.codegen.signature.Placeholder.SelectPlaceholder

/** Emits one bundle class: a method per key and a nested enum per `:string` selector. */
internal object BundleEmitter {
    private const val RUNTIME = "runtime"
    private const val TIME_ZONE = "timeZone"

    // The constant of the `*` variant.
    private const val OTHER = "other"

    // Generated methods pass plain Kotlin and java.time values to the runtime; they never touch ICU's MF2 API.

    /** Emits the bundle [className] of the [types] package with the methods of [keys]. */
    fun emit(types: GeneratedTypes, className: String, keys: List<MergedKey>): FileSpec {
        // The class opts in for its private runtime property; the constructor takes the runtime and is internal API.
        val cls = TypeSpec.classBuilder(className)
            .addKdoc("%L", GeneratedKdoc.bundle(className, keys))
            .addAnnotation(GeneratedTypes.optInInternalApi)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addAnnotation(GeneratedTypes.internalApiAnnotation)
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter(RUNTIME, GeneratedTypes.runtime)
                    .addParameter(TIME_ZONE, GeneratedTypes.zoneId)
                    .build(),
            )
            .addProperty(PropertySpec.builder(RUNTIME, GeneratedTypes.runtime, KModifier.PRIVATE).initializer(RUNTIME).build())
            .addProperty(PropertySpec.builder(TIME_ZONE, GeneratedTypes.zoneId, KModifier.PRIVATE).initializer(TIME_ZONE).build())
        val sortedKeys = keys.sortedBy { it.key }
        sortedKeys
            .flatMap { key ->
                key.placeholders.values.filterIsInstance<SelectPlaceholder>().map { placeholder -> selectEnum(key.key, placeholder) }
            }
            .forEach(cls::addType)
        sortedKeys.map { method(types.bundle(className), it) }.forEach(cls::addFunction)
        return FileSpec.builder(types.packageName, className)
            .addFileComment(GENERATED_FILE_HEADER)
            .addType(cls.build())
            .build()
    }

    // `this.` keeps the members apart from message parameters named `runtime` or `timeZone`.
    private fun method(bundle: ClassName, key: MergedKey): FunSpec {
        val fn = FunSpec.builder(Naming.methodNameFor(key.key))
            .returns(STRING)
            .addKdoc("%L", GeneratedKdoc.method(key))
        key.placeholders.values.forEach { fn.addParameter(it.name, kotlinType(bundle, key.key, it)) }
        val arguments = key.placeholders.values.map(::argumentPair)
        val block = CodeBlock.builder().add("return·this.runtime.format(%S,·this.timeZone", key.key)
        arguments.forEach { block.add(",·%L", it) }
        return fn.addCode(block.add(")\n").build()).build()
    }

    // `:string` selector keys are lowercase in the messages; the Kotlin enum is uppercase by convention.
    private fun argumentPair(placeholder: Placeholder): CodeBlock = when (placeholder) {
        is SelectPlaceholder -> CodeBlock.of("%S·to·%N.name.lowercase()", placeholder.name, placeholder.name)
        is Placeholder.StringPlaceholder,
        is Placeholder.NumberPlaceholder,
        is Placeholder.IntPlaceholder,
        is Placeholder.InstantPlaceholder,
        is Placeholder.ZonedDateTimePlaceholder,
        is Placeholder.ZoneIdPlaceholder,
        is Placeholder.CurrencyAmountPlaceholder,
        -> CodeBlock.of("%S·to·%N", placeholder.name, placeholder.name)
    }

    private fun selectEnum(key: String, placeholder: SelectPlaceholder): TypeSpec {
        val constants = (placeholder.selectVariants + OTHER).map(String::uppercase).toSortedSet()
        return constants.fold(
            TypeSpec.enumBuilder(Naming.enumSimpleName(key, placeholder.name)).addKdoc("%L", GeneratedKdoc.selectEnum(key, placeholder)),
        ) { builder, constant -> builder.addEnumConstant(constant) }.build()
    }

    private fun kotlinType(bundle: ClassName, key: String, placeholder: Placeholder): TypeName = when (placeholder) {
        is Placeholder.StringPlaceholder -> STRING
        is Placeholder.NumberPlaceholder -> Number::class.asClassName()
        is Placeholder.IntPlaceholder -> Int::class.asClassName()
        is Placeholder.InstantPlaceholder -> GeneratedTypes.instant
        is Placeholder.ZonedDateTimePlaceholder -> GeneratedTypes.zonedDateTime
        is Placeholder.ZoneIdPlaceholder -> GeneratedTypes.zoneId
        is Placeholder.CurrencyAmountPlaceholder -> GeneratedTypes.currencyAmount
        is SelectPlaceholder -> bundle.nestedClass(Naming.enumSimpleName(key, placeholder.name))
    }
}
