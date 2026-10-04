package tech.testsys.infra.localization.codegen.emitter

import tech.testsys.infra.localization.codegen.mf2.function.StringFunction
import tech.testsys.infra.localization.codegen.signature.MergedKey
import tech.testsys.infra.localization.codegen.signature.Placeholder.SelectPlaceholder
import tech.testsys.infra.localization.codegen.source.RegionDefinition

/** The KDoc texts of the generated code. */
internal object GeneratedKdoc {
    const val FOR_REGION = "Builds a [Localization] for [region], formatting dates in [timeZone] unless a message fixes its own zone.\n" +
        "The required zone prevents dates from silently using the server's default zone."
    const val MESSAGES = "MF2 messages and glossary terms of every region, formatted by one cached runtime per region."
    const val RUNTIME_OF_REGION = "Returns the runtime of [region]."
    const val TO_ULOCALE = "Maps this region to the ICU [ULocale] used by all locale-sensitive facilities (formatters, " +
        "collator, break iterator, calendar)."

    val SUPPORTED_REGION = """
        Closed set of regions for which the application ships first-class localization support.

        Each entry pairs a country/language pair the product is shipped to with the ICU [ULocale] used to drive
        every locale-sensitive behavior — number and date formatting, collation, segmentation, and calendar
        arithmetic — through [LocaleData.forRegion]. Treating the region set as an enum (rather than free-form
        locale strings) makes it a compile-time error to reference a region the product has not been translated
        and reviewed for: any new market must be declared in `regions.properties` explicitly, alongside its
        translations and any region-specific business rules.

        Use [toULocale] whenever an ICU API needs a [ULocale] directly; prefer passing the [SupportedRegion]
        itself across module boundaries so callers cannot smuggle in unsupported locales.

        Generated from `regions.properties`; do not edit by hand.

    """.trimIndent()

    /** The KDoc of the bundle class [className] generated from [keys]. */
    fun bundle(className: String, keys: List<MergedKey>): String =
        "Localized strings for the '${Naming.bundlePropertyName(className)}' bundle. " +
            "Generated from the keys with prefix '${keys.first().key.substringBefore('.')}.'. Do not edit by hand."

    /** The KDoc of the method of [key]. */
    fun method(key: MergedKey): String {
        val params = key.placeholders.values.joinToString("\n") { placeholder ->
            val variants = (placeholder as? SelectPlaceholder)?.selectVariants?.takeIf { keys -> keys.isNotEmpty() }
                ?.let { keys -> " Variants: ${keys.sorted().joinToString(", ")}." }
                .orEmpty()
            "@param ${placeholder.name} value of the MF2 variable '\$${placeholder.name}'.$variants"
        }
        return listOf("Renders the localized message for key '${key.key}'.", params)
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n\n", postfix = "\n")
    }

    /** The KDoc of the enum of the `:string` selector [placeholder] of the message [key]. */
    fun selectEnum(key: String, placeholder: SelectPlaceholder): String =
        "Variants of the ':${StringFunction.name}' selector '\$${placeholder.name}' of message '$key'. " +
            "'OTHER' selects the '*' variant."

    /** The KDoc of `Localization` with the [bundleClasses]. */
    fun localization(bundleClasses: Set<String>): String =
        "Type-safe entry point for localized messages, constructed via [Localization.forRegion].\n\n" +
            bundleClasses.joinToString("\n") { "@property ${Naming.bundlePropertyName(it)} localized messages of the [$it] bundle." }

    /** The KDoc of the `SupportedRegion` constant of [region]: its comment in `regions.properties` or a default. */
    fun region(region: RegionDefinition): String = region.comment ?: "Region '${region.id}' (`${region.languageTag}`)."
}
