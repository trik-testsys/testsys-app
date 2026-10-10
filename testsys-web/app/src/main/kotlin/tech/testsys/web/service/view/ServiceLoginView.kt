package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.service.security.ServiceSignIn
import tech.testsys.web.service.security.ServiceSignInResult

/**
 * Service login with one secret field, cleared after every submitted attempt.
 *
 * @since %CURRENT_VERSION%
 */
@Route("login")
@PageTitle("Служебный вход")
@AnonymousAllowed
class ServiceLoginView(texts: UiTexts, private val signIn: ServiceSignIn) : TestSysView(texts) {
    init {
        page(ServiceHeaders.header()) {
            head("Служебный вход")
            row {
                space(size = 7)
                block(size = 10) {
                    lateinit var code: ValueInput<String>
                    row { code = passwordInput("Код", labelSize = 6, size = 18) }
                    footer {
                        mainAction("Войти") {
                            clickOnEnter()
                            onClick { submit(code) }
                        }
                    }
                }
                space(size = 7)
            }
        }
    }

    private fun submit(code: ValueInput<String>) {
        val result = try {
            signIn.signIn(code.value)
        } finally {
            code.value = ""
        }

        when (result) {
            ServiceSignInResult.Success -> UI.getCurrent().navigate(ServiceHomeView::class.java)
            ServiceSignInResult.InvalidCode -> toast(FeedbackKind.Error, "Неверный код")
            ServiceSignInResult.Blocked -> toast(FeedbackKind.Error, "Вход временно заблокирован. Повторите попытку через 5 минут")
        }
    }
}
