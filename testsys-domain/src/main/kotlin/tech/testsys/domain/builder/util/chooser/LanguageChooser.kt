package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.model.task.TrikSupportedLanguage

/**
 * DSL chooser for selecting a [TrikSupportedLanguage].
 *
 * @since %CURRENT_VERSION%
 */
class LanguageChooser : Chooser<TrikSupportedLanguage>() {

    /**
     * Selects [TrikSupportedLanguage.Python].
     *
     * @since %CURRENT_VERSION%
     */
    fun python() = makeChoice(TrikSupportedLanguage.Python)

    /**
     * Selects [TrikSupportedLanguage.JavaScript].
     *
     * @since %CURRENT_VERSION%
     */
    fun javaScript() = makeChoice(TrikSupportedLanguage.JavaScript)

    /**
     * Selects [TrikSupportedLanguage.VisualLanguage].
     *
     * @since %CURRENT_VERSION%
     */
    fun visualLanguage() = makeChoice(TrikSupportedLanguage.VisualLanguage)

}
