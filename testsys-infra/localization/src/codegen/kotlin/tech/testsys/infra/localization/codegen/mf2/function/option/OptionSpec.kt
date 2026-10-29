package tech.testsys.infra.localization.codegen.mf2.function.option

import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.mf2.function.ArgumentType

/**
 * An MF2 option of a function.
 *
 * @property variableType the Kotlin type of a message argument given as the value (`opt=$v`), or `null` if the
 *   value must be a literal.
 */
internal class OptionSpec(
    val name: String,
    val variableType: ArgumentType? = null,
    private val check: ValueCheck = ValueCheck { _, _ -> null },
) {
    /** Returns why the literal [value] is invalid for [locale], or `null` if it is valid. */
    fun literalProblem(value: String, locale: ULocale): String? = check.findProblem(value, locale)
}
