// ICU4J 78.1 resolves a `usage` of a unit by falling back from `a-b-c` to `a-b`, `a` and finally `default`, so a
// usage the unit category does not define is silently replaced by `default`. The categories and their usages are
// read through the internal ICU API `com.ibm.icu.impl`. Pinned by OptionHonouredTests ("should fall back to the
// default usage if the unit category does not define the usage (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

import com.ibm.icu.impl.ICUData
import com.ibm.icu.impl.units.MeasureUnitImpl
import com.ibm.icu.impl.units.UnitsData
import com.ibm.icu.util.UResourceBundle
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.UnitFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.UNIT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.USAGE
import tech.testsys.infra.localization.codegen.mf2.function.option.UnitIdentifierCheck
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects a literal `usage` of `:unit` that the category of its literal unit does not define. */
internal object UnitUsageRule : FunctionRule() {
    private const val UNITS_BUNDLE = "units"
    private const val PREFERENCES = "unitPreferenceData"

    private val usagesByCategory: Map<String, Set<String>> by lazy {
        val preferences = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, UNITS_BUNDLE).get(PREFERENCES)
        (0 until preferences.size).associate { index ->
            val category = preferences.get(index)
            category.key to (0 until category.size).map { usage -> category.get(usage).key }.toSet()
        }
    }

    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        if (function != UnitFunction) return emptyList()
        val options = expression.value.options
        val usage = (options[USAGE] as? LiteralOperand)?.value ?: return emptyList()
        val unit = (options[UNIT] as? LiteralOperand)?.value?.takeIf(UnitIdentifierCheck::isUnit) ?: return emptyList()
        return listOfNotNull(usageProblem(unit, usage))
    }

    private fun usageProblem(unit: String, usage: String): String? {
        val category = UnitsData().getCategory(MeasureUnitImpl.forIdentifier(unit))
        val usages = usagesByCategory[category].orEmpty()
        val candidates = generateSequence(usage) { it.substringBeforeLast('-', "").ifEmpty { null } }
        return if (candidates.any { it in usages }) null else Problems.Values.usageCategory(usage, category, usages.sorted())
    }
}
