package tech.testsys.infra.localization.bundle

import com.ibm.icu.util.ULocale

/**
 * Closed set of regions for which the application ships first-class
 * localization support.
 *
 * Each entry pairs a country/language pair the product is shipped to with the
 * ICU [ULocale] used to drive every locale-sensitive behavior — number and
 * date formatting, collation, segmentation, and calendar arithmetic — through
 * [LocaleData.forRegion]. Treating the region set as an enum (rather
 * than free-form locale strings) makes it a compile-time error to reference a
 * region the product has not been translated and reviewed for: any new market
 * must be added here explicitly, alongside its translations and any
 * region-specific business rules.
 *
 * Use [toULocale] whenever an ICU API needs a [ULocale] directly; prefer
 * passing the [SupportedRegion] itself across module boundaries so callers
 * cannot smuggle in unsupported locales.
 *
 * @see LocaleData.forRegion
 */
enum class SupportedRegion {
    /** Russia — Russian language, Russian Federation conventions (`ru_RU`). */
    RU,
    ;

    /**
     * Maps this region to the ICU [ULocale] used by all locale-sensitive
     * facilities (formatters, collator, break iterator, calendar).
     */
    fun toULocale(): ULocale = when (this) {
        RU -> ULocale("ru_RU")
    }
}
