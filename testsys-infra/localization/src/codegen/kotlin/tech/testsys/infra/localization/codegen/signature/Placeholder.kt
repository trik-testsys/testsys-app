package tech.testsys.infra.localization.codegen.signature

/**
 * An argument of a message; the subtype is the Kotlin parameter type of the generated method, derived from how the
 * MF2 message uses the argument (see [TypeInference]).
 *
 * @property typeName the human-readable type name used in error messages.
 */
internal sealed class Placeholder(val name: String) {
    abstract val typeName: String

    class StringPlaceholder(name: String) : Placeholder(name) {
        override val typeName: String = "String"
    }

    class NumberPlaceholder(name: String) : Placeholder(name) {
        override val typeName: String = "Number"
    }

    class IntPlaceholder(name: String) : Placeholder(name) {
        override val typeName: String = "Int"
    }

    class InstantPlaceholder(name: String) : Placeholder(name) {
        override val typeName: String = "Instant"
    }

    class ZonedDateTimePlaceholder(name: String) : Placeholder(name) {
        override val typeName: String = "ZonedDateTime"
    }

    class ZoneIdPlaceholder(name: String) : Placeholder(name) {
        override val typeName: String = "ZoneId"
    }

    class CurrencyAmountPlaceholder(name: String) : Placeholder(name) {
        override val typeName: String = "CurrencyAmount"
    }

    /**
     * @property selectVariants literal keys of the `:string` selector seen across regions; the emitter adds `OTHER`
     *   for the `*` variant.
     */
    class SelectPlaceholder(name: String, val selectVariants: Set<String>) : Placeholder(name) {
        override val typeName: String = "enum"
    }
}

/** Where a date argument of one region's message takes its time zone from, when it is not the context zone. */
internal sealed interface DateZoneSpec {
    /** A literal `timeZone` option, including `UTC`. */
    data class Fixed(val zoneId: String) : DateZoneSpec

    /** `timeZone=$argument`: the `ZoneId` argument [argument]. */
    data class Argument(val argument: String) : DateZoneSpec
}

/**
 * Reconciles two [Placeholder]s with the same name, from different regions or different uses in one message;
 * returns `null` if they conflict.
 *
 * Compatibility rules:
 *  - identical subtypes collapse (SELECT unions its variants);
 *  - NUMBER widens to INT when one side requires integer input — `Int` is assignable to `Number`, not vice versa;
 *  - all other pairings conflict.
 */
internal fun mergePlaceholders(a: Placeholder, b: Placeholder): Placeholder? {
    require(a.name == b.name) { "Cannot merge placeholders '${a.name}' and '${b.name}' with different names" }
    return when {
        a is Placeholder.SelectPlaceholder && b is Placeholder.SelectPlaceholder ->
            Placeholder.SelectPlaceholder(a.name, a.selectVariants + b.selectVariants)
        a::class == b::class -> a
        a is Placeholder.NumberPlaceholder && b is Placeholder.IntPlaceholder -> b
        a is Placeholder.IntPlaceholder && b is Placeholder.NumberPlaceholder -> a
        else -> null
    }
}
