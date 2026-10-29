package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.model.task.TrikSupportedLanguage

/**
 * DSL chooser of a [TrikSupportedLanguage].
 *
 * @since 1.0.0-beta.1
 */
class LanguageChooser : Chooser<TrikSupportedLanguage>() {

    /**
     * Selects [TrikSupportedLanguage.Python].
     *
     * @since 1.0.0-beta.1
     */
    fun python() = makeChoice(object : Builder<TrikSupportedLanguage> {
        override fun build() = TrikSupportedLanguage.Python
    })

    /**
     * Selects [TrikSupportedLanguage.JavaScript].
     *
     * @since 1.0.0-beta.1
     */
    fun javaScript() = makeChoice(object : Builder<TrikSupportedLanguage> {
        override fun build() = TrikSupportedLanguage.JavaScript
    })

    /**
     * Selects [TrikSupportedLanguage.VisualLanguage].
     *
     * @since 1.0.0-beta.1
     */
    fun visualLanguage() = makeChoice(object : Builder<TrikSupportedLanguage> {
        override fun build() = TrikSupportedLanguage.VisualLanguage
    })
}
