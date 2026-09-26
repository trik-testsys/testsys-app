package tech.testsys.web.ui.display

/**
 * Meaning of a badge state; the colour of the badge follows it.
 *
 * @since %CURRENT_VERSION%
 */
enum class Tone(internal val modifier: String) {
    Neutral("neutral"),
    Info("info"),
    Success("success"),
    Warning("warning"),
    Danger("danger"),
}
