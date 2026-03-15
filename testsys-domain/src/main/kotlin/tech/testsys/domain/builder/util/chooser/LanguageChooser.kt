package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.model.task.TrikSupportedLanguage

class LanguageChooser : Chooser<TrikSupportedLanguage>() {

    fun python() = makeChoice(TrikSupportedLanguage.Python)

    fun javaScript() = makeChoice(TrikSupportedLanguage.JavaScript)

    fun visualLanguage() = makeChoice(TrikSupportedLanguage.VisualLanguage)

}
