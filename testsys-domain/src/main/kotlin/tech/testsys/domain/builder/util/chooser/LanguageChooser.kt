package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
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
    fun python() = makeChoice(object : Builder<TrikSupportedLanguage> {
        override fun build() = TrikSupportedLanguage.Python
    })

    /**
     * Selects [TrikSupportedLanguage.JavaScript].
     *
     * @since %CURRENT_VERSION%
     */
    fun javaScript() = makeChoice(object : Builder<TrikSupportedLanguage> {
        override fun build() = TrikSupportedLanguage.JavaScript
    })

    /**
     * Selects [TrikSupportedLanguage.VisualLanguage].
     *
     * @since %CURRENT_VERSION%
     */
    fun visualLanguage() = makeChoice(object : Builder<TrikSupportedLanguage> {
        override fun build() = TrikSupportedLanguage.VisualLanguage
    })
}
