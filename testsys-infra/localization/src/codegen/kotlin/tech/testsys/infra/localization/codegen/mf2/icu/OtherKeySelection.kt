// ICU4J 78.1 never matches the literal key `other` of a numeric selector and picks the next variant instead, usually
// `*`. Pinned by OptionHonouredTests ("should never match the literal key other of a number selector (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

/** How ICU4J 78.1 matches the literal key `other`; the completeness check compares its choice with the MF2 one. */
internal object OtherKeySelection {
    private const val OTHER = "other"

    /** Returns whether ICU4J 78.1 skips the literal [key] for a value of the plural [category] of a numeric selector. */
    fun isSkipped(key: String, category: String?, isNumeric: Boolean): Boolean = isNumeric && key == OTHER && category == OTHER
}
