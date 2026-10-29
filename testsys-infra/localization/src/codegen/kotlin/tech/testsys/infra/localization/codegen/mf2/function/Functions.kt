package tech.testsys.infra.localization.codegen.mf2.function

/**
 * Registry of the supported functions: [all] in a fixed order, lookup [byName] and the groups derived from the
 * properties of the functions. [ICU_GENDER] is the ICU function the codegen rejects by design: it duplicates
 * `:string`, and one canonical way is kept.
 */
internal object Functions {
    const val ICU_GENDER = "icu:gender"

    val all: List<Mf2Function> = listOf(
        StringFunction,
        IntegerFunction,
        NumberFunction,
        PercentFunction,
        CurrencyFunction,
        OffsetFunction,
        DateFunction,
        TimeFunction,
        DateTimeFunction,
        TermFunction,
        SpelloutFunction,
        OrdinalFunction,
        UnitFunction,
    )
    val byName: Map<String, Mf2Function> = all.associateBy { it.name }
    val selectors: List<Mf2Function> = all.filter { it.selection != null }
    val builtIns: List<Mf2Function> = all.filterNot { it.isCustom }
    val custom: List<Mf2Function> = all.filter { it.isCustom }
    val numericBuiltIns: Set<Mf2Function> = builtIns.filter { it.isNumeric }.toSet()
}
