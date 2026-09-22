package tech.testsys.infra.localization.codegen.emitter

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import tech.testsys.infra.localization.codegen.parser.MergedKey
import tech.testsys.infra.localization.codegen.parser.Placeholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.InstantPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.IntPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.NumberPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.SelectPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.StringPlaceholder

/**
 * Renders the merged signature of every key into KotlinPoet [FileSpec]s.
 *
 * For every prefix-group the emitter produces one bundle class with one method
 * per key, plus a `Localization` aggregator. Each generated method inlines its
 * per-region ICU patterns directly into a `when (region)` expression and feeds
 * them into ICU's `MessageFormat` — runtime never reads `.properties` files.
 */
internal class KotlinEmitter(
    private val packageName: String = "tech.testsys.infra.localization",
) {

    private val bundlePackage = packageName
    private val supportedRegion = ClassName("$packageName.bundle", "SupportedRegion")
    private val messageFormat = ClassName("com.ibm.icu.text", "MessageFormat")
    private val dateClass = ClassName("java.util", "Date")
    private val mapOfMember = MemberName("kotlin.collections", "mapOf")
    private val emptyMapMember = MemberName("kotlin.collections", "emptyMap")

    fun emit(merged: List<MergedKey>): List<FileSpec> {
        // toSortedMap so file emission order is deterministic — easier to diff in Git.
        val byClass = merged
            .filter { it.key.contains('.') }
            .groupBy { classNameFor(it.key) }
            .toSortedMap()

        validateMethodNameClashes(byClass)

        val bundleFiles = byClass.map { (className, keys) -> emitBundle(className, keys) }
        return bundleFiles + emitLocalization(byClass.keys)
    }

    private fun emitBundle(className: String, keys: List<MergedKey>): FileSpec {
        val cls = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc(bundleKdoc(className, keys))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("region", supportedRegion)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("region", supportedRegion)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("region")
                    .build()
            )

        val sortedKeys = keys.sortedBy { it.key }
        sortedKeys
            .flatMap { key -> selectPlaceholders(key).map { ph -> buildSelectEnum(key.key, ph) } }
            .forEach(cls::addType)
        sortedKeys
            .map { key -> buildMethod(className, key) }
            .forEach(cls::addFunction)
        return FileSpec.builder(bundlePackage, className)
            .addFileComment(GENERATED_FILE_HEADER)
            .addType(cls.build())
            .build()
    }

    private fun selectPlaceholders(key: MergedKey): List<SelectPlaceholder> =
        key.placeholders.values.filterIsInstance<SelectPlaceholder>()

    private fun buildMethod(className: String, key: MergedKey): FunSpec {
        val fn = FunSpec.builder(methodNameFor(key.key))
            .returns(String::class)
            .addKdoc(methodKdoc(key))
        addMethodParameters(fn, className, key)
        fn.addCode(methodBody(key))
        return fn.build()
    }

    private fun addMethodParameters(fn: FunSpec.Builder, className: String, key: MergedKey) {
        key.placeholders.values.forEach { ph ->
            fn.addParameter(ph.name, kotlinTypeFor(className, key.key, ph))
        }
    }

    /**
     * Renders the method body: a `when (region)` selecting the ICU pattern,
     * then a single `MessageFormat(...).format(args)` call.
     *
     * Built in a single CodeBlock builder so KotlinPoet's indent tracking stays
     * coherent — substituting a sub-CodeBlock that owns its own indent markers
     * via `%L` interacts badly with the outer builder's indent state.
     */
    private fun methodBody(key: MergedKey): CodeBlock {
        val withBranches = key.patternsByRegion.entries.fold(
            CodeBlock.builder()
                .add("val pattern = when (region) {\n")
                .indent()
        ) { b, (region, pattern) -> b.add("%T.%N -> %S\n", supportedRegion, region, pattern) }
        return withBranches
            .unindent()
            .add("}\n")
            .add("return %T(pattern, region.toULocale()).format(%L)\n", messageFormat, buildArgsMap(key))
            .build()
    }

    private fun buildArgsMap(key: MergedKey): CodeBlock {
        // Explicit type params on the empty case so MessageFormat overload resolution
        // (which has both Map and Object[] forms) picks the named-arguments overload.
        if (key.placeholders.isEmpty()) {
            return CodeBlock.of("%M<%T, %T>()", emptyMapMember, STRING, ANY)
        }
        return CodeBlock.builder()
            .add("%M(", mapOfMember)
            .add(key.placeholders.values.map(::argEntry).joinToCode(separator = ", "))
            .add(")")
            .build()
    }

    private fun argEntry(ph: Placeholder): CodeBlock = when (ph) {
        // ICU 'select' branch labels are written lowercase in the patterns
        // ("male", "female", "other"). The Kotlin enum is uppercase by convention,
        // so coerce back at the call site rather than forcing weird casing on the enum.
        is SelectPlaceholder -> CodeBlock.of("%S to %N.name.lowercase()", ph.name, ph.name)
        // ICU MessageFormat's `{x, date}` / `{x, time}` accept java.util.Date but not
        // java.time.Instant directly, so bridge at the call site.
        is InstantPlaceholder -> CodeBlock.of("%S to %T.from(%N)", ph.name, dateClass, ph.name)
        else -> CodeBlock.of("%S to %N", ph.name, ph.name)
    }

    private fun buildSelectEnum(key: String, ph: SelectPlaceholder): TypeSpec {
        // ICU requires every 'select' to define an `other` branch as the catch-all,
        // so we always emit OTHER even if no region's pattern listed it explicitly.
        val variants = (ph.selectVariants + "other")
            .map(String::uppercase)
            .toSortedSet()
        return variants.fold(
            TypeSpec.enumBuilder(enumSimpleName(key, ph.name)).addKdoc(selectEnumKdoc(key, ph))
        ) { builder, variant -> builder.addEnumConstant(variant) }.build()
    }

    private fun kotlinTypeFor(className: String, key: String, ph: Placeholder): TypeName = when (ph) {
        is StringPlaceholder -> String::class.asClassName()
        is NumberPlaceholder -> Number::class.asClassName()
        is IntPlaceholder -> Int::class.asClassName()
        is InstantPlaceholder -> ClassName("java.time", "Instant")
        is SelectPlaceholder -> ClassName(bundlePackage, className, enumSimpleName(key, ph.name))
    }

    private fun emitLocalization(bundleClasses: Set<String>): FileSpec {
        val cls = bundleClasses.fold(
            TypeSpec.classBuilder("Localization")
                .addModifiers(KModifier.PUBLIC)
                .addKdoc(localizationKdoc(bundleClasses))
                .primaryConstructor(localizationConstructor(bundleClasses))
        ) { builder, name -> builder.addProperty(bundleProperty(name)) }
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(buildForRegion(bundleClasses))
                    .build()
            )

        return FileSpec.builder(bundlePackage, "Localization")
            .addFileComment(GENERATED_FILE_HEADER)
            .addType(cls.build())
            .build()
    }

    private fun localizationConstructor(bundleClasses: Set<String>): FunSpec =
        bundleClasses.fold(FunSpec.constructorBuilder().addModifiers(KModifier.PRIVATE)) { ctor, name ->
            ctor.addParameter(bundlePropertyName(name), ClassName(bundlePackage, name))
        }.build()

    private fun bundleProperty(name: String): PropertySpec {
        val prop = bundlePropertyName(name)
        return PropertySpec.builder(prop, ClassName(bundlePackage, name))
            .initializer(prop)
            .build()
    }

    private fun buildForRegion(bundleClasses: Set<String>): FunSpec {
        val constructorArgs = bundleClasses.map { name ->
            CodeBlock.of("%N = %T(region)", bundlePropertyName(name), ClassName(bundlePackage, name))
        }
        val returnBlock = CodeBlock.builder()
            .add("return Localization(\n")
            .indent()
            .add(constructorArgs.joinToCode(separator = ",\n", suffix = ",\n"))
            .unindent()
            .add(")\n")
            .build()
        return FunSpec.builder("forRegion")
            .addKdoc(forRegionKdoc())
            .addParameter("region", supportedRegion)
            .returns(ClassName(bundlePackage, "Localization"))
            .addCode(returnBlock)
            .build()
    }

    /**
     * Catches keys that survive [classNameFor]/[methodNameFor] as the same Kotlin
     * symbol — e.g. `task.in_days` and `task.inDays` both fold to `inDays`.
     * Easier to diagnose at codegen than as a confusing duplicate-method compile error.
     */
    private fun validateMethodNameClashes(byClass: Map<String, List<MergedKey>>) {
        val errors = byClass.flatMap { (className, keys) ->
            keys
                .groupBy { methodNameFor(it.key) }
                .filterValues { it.size > 1 }
                .map { (method, group) ->
                    "Class '$className': method '$method' produced by " +
                        group.joinToString(", ") { "'${it.key}'" }
                }
        }
        if (errors.isNotEmpty()) {
            error("Localization codegen errors:\n  - " + errors.joinToString("\n  - "))
        }
    }

    private fun bundlePropertyName(className: String): String =
        className.replaceFirstChar(Char::lowercaseChar)

    private fun bundleKdoc(className: String, keys: List<MergedKey>): CodeBlock =
        CodeBlock.of(
            "%L",
            "Localized strings for the '${bundlePropertyName(className)}' bundle.\n\n" +
                "Generated from properties keys with prefix " +
                "'${keys.first().key.substringBefore('.')}.'. Do not edit by hand.",
        )

    private fun methodKdoc(key: MergedKey): CodeBlock {
        val paramsBlock = key.placeholders.values
            .joinToString(separator = "\n", transform = ::paramKdocLine)
            .takeIf { key.placeholders.isNotEmpty() }

        val text = listOfNotNull(
            "Renders the localized message for key '${key.key}'.",
            paramsBlock,
        ).joinToString(separator = "\n\n", postfix = "\n")
        return CodeBlock.of("%L", text)
    }

    private fun paramKdocLine(ph: Placeholder): String {
        val variants = (ph as? SelectPlaceholder)
            ?.selectVariants
            ?.takeIf { it.isNotEmpty() }
            ?.let { " Variants: ${it.sorted().joinToString(", ")}." }
            .orEmpty()
        return "@param ${ph.name} value bound to the ICU placeholder '{${ph.name}}'.$variants"
    }

    private fun selectEnumKdoc(key: String, ph: Placeholder): CodeBlock = CodeBlock.of(
        "%L",
        "Variants for the ICU 'select' placeholder '${ph.name}' of message '$key'.\n\n" +
            "'OTHER' is the fallback branch required by ICU.",
    )

    private fun localizationKdoc(bundleClasses: Set<String>): CodeBlock {
        val header = "Type-safe entry point for localized messages.\n\n" +
            "Construct via [Localization.forRegion]; do not instantiate directly.\n\n" +
            "Bundles:\n"
        val bundles = bundleClasses.joinToString(separator = "\n") { " - [$it]" }
        return CodeBlock.of("%L", header + bundles)
    }

    private fun forRegionKdoc(): CodeBlock = CodeBlock.of(
        "%L",
        "Builds a [Localization] instance bound to the given [SupportedRegion].\n\n" +
            "Each bundle holds the region directly and selects its ICU pattern at call time.\n",
    )

    private companion object {
        const val GENERATED_FILE_HEADER =
            "Generated by tech.testsys.infra.localization.codegen. Do not edit."
    }
}

/**
 * PascalCase bundle class name from the segment before the first dot.
 * Underscores are treated as word separators (`my_task` → `MyTask`).
 *
 * Example: `task.deadline.in_days` → `Task`.
 */
internal fun classNameFor(key: String): String =
    key.substringBefore('.')
        .split('_')
        .filter(String::isNotEmpty)
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

/**
 * camelCase method name from everything after the first dot.
 * Both `.` and `_` act as word separators so `a.b_c.d` and `a.bC.d` collapse to `bCD`.
 *
 * Example: `task.deadline.in_days` → `deadlineInDays`.
 */
internal fun methodNameFor(key: String): String {
    val parts = key.substringAfter('.').split('.', '_').filter(String::isNotEmpty)
    if (parts.isEmpty()) return key.substringAfter('.')
    return parts.first().replaceFirstChar(Char::lowercaseChar) +
        parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
}

/**
 * Nested enum name for an ICU 'select' arg, scoped to its bundle method to avoid
 * collisions when the same arg name (e.g. `gender`) appears in multiple keys.
 *
 * Example: key `task.deadline.in_days`, arg `gender` → `DeadlineInDaysGender`.
 */
internal fun enumSimpleName(key: String, argName: String): String =
    methodNameFor(key).replaceFirstChar(Char::uppercaseChar) +
        argName.replaceFirstChar(Char::uppercaseChar)
