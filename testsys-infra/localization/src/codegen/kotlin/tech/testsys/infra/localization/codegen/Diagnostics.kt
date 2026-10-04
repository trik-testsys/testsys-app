package tech.testsys.infra.localization.codegen

/**
 * Failure of a codegen run; [errors] holds every problem found, so one Gradle run reports all of them at once.
 */
internal class LocalizationCodegenException(val errors: List<String>) : IllegalStateException(
    "Localization codegen errors:\n  - " + errors.joinToString("\n  - "),
)

/** Formats a problem of the message [key] in [region] as `<REGION> / <key>: <problem>`. */
internal fun messageError(region: String, key: String, problem: String): String = "$region / $key: $problem"

/** Formats a problem of a resource file as `<file>:<line>: <problem>`. */
internal fun fileError(file: String, line: Int, problem: String): String = "$file:$line: $problem"
