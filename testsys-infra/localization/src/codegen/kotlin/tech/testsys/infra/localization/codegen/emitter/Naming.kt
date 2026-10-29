package tech.testsys.infra.localization.codegen.emitter

/** Kotlin names of the generated code, derived from the localization keys. */
internal object Naming {

    /**
     * PascalCase bundle class name from the segment before the first dot.
     * Underscores are treated as word separators (`my_task` → `MyTask`).
     *
     * Example: `task.deadline.in_days` → `Task`.
     */
    fun classNameFor(key: String): String = key.substringBefore('.')
        .split('_')
        .filter(String::isNotEmpty)
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

    /**
     * camelCase method name from everything after the first dot.
     * Both `.` and `_` act as word separators so `a.b_c.d` and `a.bC.d` collapse to `bCD`.
     *
     * Example: `task.deadline.in_days` → `deadlineInDays`.
     */
    fun methodNameFor(key: String): String {
        val parts = key.substringAfter('.').split('.', '_').filter(String::isNotEmpty)
        if (parts.isEmpty()) return key.substringAfter('.')
        return parts.first().replaceFirstChar(Char::lowercaseChar) +
            parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    /**
     * Nested enum name for a `:string` selector, scoped to its bundle method to avoid collisions when the same
     * argument name (e.g. `gender`) appears in multiple keys.
     *
     * Example: key `user.solved`, argument `gender` → `SolvedGender`.
     */
    fun enumSimpleName(key: String, argName: String): String = methodNameFor(key).replaceFirstChar(Char::uppercaseChar) +
        argName.replaceFirstChar(Char::uppercaseChar)

    /** The property of `Localization` that holds the bundle [className]. */
    fun bundlePropertyName(className: String): String = className.replaceFirstChar(Char::lowercaseChar)
}
