package tech.testsys.infra.localization.codegen

/**
 * Catalog of every problem text the codegen reports, grouped by area. Function and option names come in as
 * parameters, so no text names a function or an option itself.
 */
internal object Problems {
    // The pinned ICU version whose behaviour the texts describe; an ICU upgrade updates it here.
    private const val ICU4J = "ICU4J 78.1"

    /** Problems of the resource directory and of one `.properties` file. */
    object Files {
        const val REGIONS_FILE_MISSING = "the file is missing; it declares the supported regions"
        const val BOM = "the file starts with a UTF-8 BOM; save it as UTF-8 without BOM"
        const val LAYOUT = "a message file must be '<language tag of a region>/<bundle>.properties'"
        const val BUNDLE_NAME = "the file name must be '<bundle>.properties' with a bundle matching [a-z][a-z0-9_]*"
        const val COMMENTS_START_WITH_HASH = "Comments must start with #"

        fun undeclaredRegion(directory: String, regionsFile: String): String =
            "the directory '$directory' is not the language tag of a region declared in $regionsFile"

        fun regionWithoutBundles(region: String, directory: String): String = "region '$region' has no bundle files in $directory"

        fun missingBundle(bundle: String, regions: String): String = "the file is missing; the bundle '$bundle' is defined in $regions"

        fun foreignKey(key: String, bundle: String): String =
            "key '$key' does not belong to the bundle '$bundle' of this file; move it to the file of its bundle"

        fun notUtf8(error: String): String = "the file is not valid UTF-8 ($error)"

        fun malformedEscape(error: String?): String =
            "malformed \\uXXXX escape ($error); write the character itself, exactly four hex digits after \\u or \\\\ for a backslash"

        fun duplicateKey(key: String, firstLine: Int): String = "duplicate key '$key' (first defined at line $firstLine)"

        fun emptyValue(key: String): String = "key '$key' has an empty value"

        fun trailingWhitespace(key: String): String = "the value of '$key' ends with whitespace"
    }

    /** Problems of `regions.properties`. */
    object Regions {
        const val NONE_DECLARED = "no regions are declared"
        const val ENTRY_SHAPE = "expected '<REGION>=<BCP 47 language tag>'"

        fun regionId(id: String): String = "region id '$id' must match [A-Z]{2}"

        fun declaredTwice(id: String): String = "region '$id' is declared twice"

        fun languageTag(tag: String, id: String): String = "'$tag' of region '$id' is not a valid BCP 47 language tag"
    }

    /** Problems of the MF2 syntax and of constructs the API cannot render. */
    object Syntax {
        fun syntaxError(explanation: String): String = "syntax error: $explanation"

        fun markup(name: String): String = "markup '$name' is not supported: the API returns plain text and ICU drops markup silently"
    }

    /** Problems of the function of an expression. */
    object Functions {
        fun duplicate(function: String, replacement: String): String =
            "function ':$function' is not supported: it duplicates ':$replacement'; use ':$replacement'"

        fun namespaced(function: String): String =
            "function ':$function' is not supported: namespaced functions other than the supported ones are rejected"

        fun unknown(function: String): String = "unknown function ':$function'"

        fun offsetOfOffset(offset: String): String = "an ':$offset' of an ':$offset' is not supported: $ICU4J keeps only the last shift"

        fun overOffset(function: String, offset: String): String =
            "':$function' cannot annotate an ':$offset' value: $ICU4J formats the unshifted operand"

        fun declaredWith(function: String, declared: String): String =
            "':$function' cannot annotate a value declared with ':$declared': $ICU4J would apply it to the " +
                "declaration's operand and options"

        fun offsetDropsOption(offset: String, declared: String, option: String): String =
            "an ':$offset' of a ':$declared' value drops the option '$option': $ICU4J applies it only to ':$declared'"
    }

    /** Problems of the options of an expression and of their combinations. */
    object Options {
        fun unsupportedNamespace(option: String, namespace: String): String =
            "option '$option' is not supported: $namespace options have no effect on plain-text output with bidi " +
                "isolation NONE"

        fun unknown(option: String, function: String): String = "unknown option '$option' of ':$function'"

        fun notHonoured(option: String, function: String, condition: String? = null): String =
            "option '$option' of ':$function' is not honoured by $ICU4J" + condition?.let { " $it" }.orEmpty()

        fun without(option: String): String = "without '$option'"

        fun withoutValue(option: String, value: String): String = "without $option=$value"

        fun togetherWith(option: String): String = "together with '$option'"

        fun mustBeLiteral(option: String, function: String): String = "option '$option' of ':$function' must be a literal"

        fun mustTakeArgument(option: String, function: String, variable: String): String =
            "option '$option' of ':$function' must take a message argument; '\$$variable' is declared in the message"

        fun exactlyOne(function: String, first: String, second: String): String =
            "':$function' takes exactly one of the options '$first' and '$second'"

        fun required(function: String, option: String): String = "':$function' needs the option '$option'"

        fun noEffectWithout(option: String, function: String, required: String, value: String): String =
            "option '$option' of ':$function' has no effect without $required=$value"

        fun needsFractionAndSignificant(option: String, function: String): String =
            "option '$option' of ':$function' needs both fraction and significant digit options"

        fun needsMaximumFraction(option: String, function: String, maximumFraction: String, priority: String): String =
            "option '$option' of ':$function' needs '$maximumFraction' and no significant digits or '$priority'"

        fun needsLiteral(option: String, function: String, other: String): String =
            "option '$option' of ':$function' needs a literal '$other'"
    }

    /** Problems of a literal option value; [invalid] wraps the reasons. */
    object Values {
        const val NUMBERING_SYSTEM = "not a decimal ICU numbering system"
        const val DEFAULT_SELECT = "plural is the default; remove the option"
        const val ZONE = "expected 'input', 'UTC' or an IANA zone id such as |Europe/Moscow|"
        const val CALENDAR = "expected a BCP 47 calendar type such as gregory or buddhist"
        const val CURRENCY_CODE = "expected an ISO 4217 code"
        const val TERM_NAME = "expected a term name matching [a-z][a-z0-9_]*"
        const val CASE_KEY = "expected a case key matching [a-z][a-z0-9_]*"
        const val UNIT = "not an ICU unit identifier"
        const val USAGE = "not a CLDR unit usage"

        fun invalid(value: String, option: String, function: String, reason: String): String =
            "invalid value '$value' of option '$option' of ':$function': $reason"

        fun expectedOneOf(values: List<String>): String = "expected one of: ${values.joinToString()}"

        fun digitRange(minimum: Int, maximum: Int): String = "expected an integer in $minimum..$maximum"

        fun numberSkeleton(error: String?): String = "ICU rejects the number skeleton: $error"

        fun ruleSet(locale: String): String = "ICU has no such rule set for $locale"

        fun notDateField(letter: Char): String = "'$letter' is not a date pattern field"

        fun fieldWidth(letter: Char, width: Int, allowed: List<Int>): String =
            "the field '$letter' cannot be $width letters long (allowed: ${allowed.joinToString()})"

        fun repeatedField(letter: Char): String = "the field '$letter' appears more than once"

        fun standardSkeleton(options: String, function: String): String =
            "it equals the standard options '$options' of ':$function'; use them instead"

        fun usageCategory(usage: String, category: String, known: List<String>): String =
            "usage '$usage' is not defined for the unit category '$category' (known: ${known.joinToString().ifEmpty { "none" }})"
    }

    /** Problems of the operand of an expression. */
    object Operands {
        fun missing(function: String): String = "':$function' needs an operand"

        fun notNumber(value: String, function: String): String = "the literal operand '|$value|' of ':$function' is not a number"

        fun notInteger(value: String, function: String): String = "the literal operand '|$value|' of ':$function' is not an integer"

        fun notIsoDate(value: String, function: String): String = "the literal operand '|$value|' of ':$function' is not an ISO 8601 date"

        fun literalNeedsOption(function: String, option: String): String = "a literal operand of ':$function' needs the option '$option'"

        fun needsVariable(option: String, value: String): String = "$option=$value needs a variable operand"

        fun termLiteral(function: String): String = "':$function' takes an integer variable or no operand, not a literal"

        fun termWithoutNumber(function: String, option: String, singular: String, plural: String): String =
            "':$function' without an operand needs the option '$option' ($singular or $plural)"

        fun termWithNumber(function: String, option: String): String =
            "':$function' with an operand takes its form from the number; remove the option '$option'"
    }

    /** Problems of the variables of a message. */
    object Variables {
        fun usedBeforeDeclaration(name: String): String = "'\$$name' is used before its declaration"

        fun unusedInput(name: String): String = "the declared input '\$$name' is never used"

        fun name(name: String): String = "the variable name '\$$name' must match [a-z][A-Za-z0-9]* and not be a Kotlin keyword"

        fun leakingArgument(name: String): String =
            "the argument '\$$name' is named like a number option: $ICU4J passes every argument to number " +
                "functions as an option; rename it"
    }

    /** Problems of the `.match` selectors and their keys. */
    object Selectors {
        fun noAnnotation(name: String): String = "the selector '\$$name' has no annotation; declare it with '.input {\$$name :<function>}'"

        fun notSelectable(function: String, selectable: List<String>): String =
            "':$function' cannot be used as a selector: only ${selectable.joinToString { ":$it" }} select reliably in $ICU4J"

        fun otherKey(key: String, selector: String): String =
            "the key '$key' of '\$$selector' collides with the generated OTHER constant and the '*' variant; use '*'"

        fun stringKey(key: String, selector: String): String = "the key '$key' of '\$$selector' must match [a-z][a-z0-9_]*"

        fun exactWithDigits(key: String, selector: String): String =
            "the exact key '$key' of '\$$selector' is not allowed with digit options: exact matching is " +
                "implementation-defined there"

        fun exactNotInteger(key: String, selector: String): String =
            "the exact key '$key' of '\$$selector' must be an integer: 0 or -?[1-9][0-9]*"

        fun exactOnly(key: String, selector: String, option: String, value: String): String =
            "the key '$key' of '\$$selector' is not a number; $option=$value allows only exact keys"

        fun notCategory(key: String, selector: String, kind: String, locale: String, categories: Set<String>): String =
            "the key '$key' of '\$$selector' is not a $kind category of $locale (expected one of: ${categories.sorted().joinToString()})"
    }

    /** Problems and value descriptions of the strict plural completeness check. */
    object Completeness {
        fun tooManyCombinations(maximum: Int): String = "the selectors have more than $maximum value combinations to check completeness"

        fun fallback(category: String, selector: String, others: List<String>): String {
            val context = if (others.isEmpty()) "" else " when ${others.joinToString(" and ")}"
            return "category '$category' of '\$$selector' has no explicit variant$context (falls back to '*')"
        }

        fun otherNeverMatched(values: List<String>, icuKeys: String, keys: String): String =
            "$ICU4J never matches the key 'other': when ${values.joinToString(" and ")} it renders the variant " +
                "'$icuKeys' instead of '$keys'; give both the same pattern"

        fun anyOtherValue(selector: String): String = "\$$selector is any other value"

        fun equalTo(selector: String, key: String): String = "\$$selector = $key"

        fun inCategory(selector: String, category: String): String = "\$$selector in category '$category'"
    }

    /** Problems of glossary terms and of the references to them. */
    object Terms {
        const val GLOSSARY_KEY = "a glossary key must be 'glossary.<name>' with a name matching [a-z][a-z0-9_]*"

        fun shape(case: String, form: String, function: String): String =
            "a term must declare exactly '.input {\$$case :$function}' and '.input {\$$form :$function}' and select " +
                "with '.match \$$case \$$form'"

        fun caseKey(key: String): String = "case key '$key' must match [a-z][a-z0-9_]*"

        fun formKey(key: String, forms: List<String>): String = "form key '$key' is not one of ${forms.joinToString()}"

        fun plainText(keys: String): String = "the variant '$keys' must be plain text: a term has no placeholders or markup"

        fun unknownTerm(function: String, term: String): String = "':$function' refers to the unknown term '$term'"

        fun unknownCase(function: String, option: String, case: String, term: String, known: List<String>): String =
            "':$function' $option '$case' is not a case of the term '$term' (known: ${known.joinToString()})"

        fun missingVariant(term: String, case: String, form: String): String =
            "the term '$term' has no explicit variant '$case $form' needed by this reference"

        fun unused(term: String): String = "the term '$term' is never used"
    }

    /** Problems of the Kotlin names produced by the keys. */
    object Names {
        const val KEY_SHAPE = "a key must be '<bundle>.<segment>…' with segments matching [a-z][a-z0-9_]*"

        fun reservedClass(bundle: String, className: String): String =
            "the bundle '$bundle' produces the class '$className', which clashes with a generated or runtime type"

        fun sameClass(bundles: List<String>, className: String): String =
            "the bundles ${quoted(bundles)} all produce the class '$className'"

        fun classesDifferInCase(bundles: List<String>, classNames: List<String>): String =
            "the bundles ${quoted(bundles)} produce the classes ${quoted(classNames)}, which differ only in case and " +
                "share one file on a case-insensitive file system"

        fun sameMethod(keys: List<String>, className: String, method: String): String =
            "the keys ${quoted(keys)} all produce the method '$className.$method'"

        fun anyMember(method: String): String = "the method name '$method' clashes with a member of Any"

        fun sameEnum(keys: List<String>, className: String, enum: String): String =
            "the selectors of ${quoted(keys)} all produce the nested enum '$className.$enum'"

        fun enumsDifferInCase(keys: List<String>, className: String, enums: List<String>): String =
            "the selectors of ${quoted(keys)} produce the nested enums ${quoted(enums.map { "$className.$it" })}, which " +
                "differ only in case and share one class file on a case-insensitive file system"

        fun enumNamedLikeClass(enum: String): String = "the nested enum '$enum' has the name of its bundle class"

        private fun quoted(names: List<String>): String = names.joinToString { "'$it'" }
    }

    /** Problems of the argument types of a key across uses and regions. */
    object Signatures {
        fun conflictingTypes(name: String, first: String, second: String): String = "conflicting types for '\$$name': $first vs $second"

        fun inRegion(problem: String, region: String): String = "$problem (in $region)"

        fun missingKey(regions: String): String = "the key is missing in this region (it is defined in $regions)"
    }

    /** Problems and descriptions of the time zones of date arguments. */
    object Zones {
        const val CONTEXT = "the context zone"

        fun differentZones(argument: String, zones: List<String>): String =
            "the date argument '\$$argument' is formatted in different time zones (${zones.joinToString()}); use one " +
                "zone per argument"

        fun literal(option: String, value: String): String = "$option=$value"

        fun fixed(option: String, zone: String): String = "$option=|$zone|"

        fun argument(option: String, argument: String): String = "$option=\$$argument"
    }
}
