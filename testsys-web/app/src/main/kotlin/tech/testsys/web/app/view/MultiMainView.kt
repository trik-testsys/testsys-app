package tech.testsys.web.app.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.texts.UiTexts

/**
 * Main page of the Cabinet of a user with non-fixed roles (testsys.web.page.multi.main); right after registration it
 * shows the access code once.
 *
 * @since %CURRENT_VERSION%
 */
@Route("home")
@PageTitle("Главная")
@RolesAllowed("MULTIPLE_ROLE")
class MultiMainView(texts: UiTexts, private val headers: CabinetHeaders) : TestSysView(texts) {
    init {
        show(accessToken = null)
    }

    /**
     * Rebuilds the page with [accessToken] of the user who has just registered and a request to save it.
     *
     * @since %CURRENT_VERSION%
     */
    fun showAccessToken(accessToken: String) {
        show(accessToken)
    }

    private fun show(accessToken: String?) {
        page(headers.cabinet(active = CabinetHeaders.MAIN_SECTION)) {
            if (accessToken != null) {
                row {
                    block(title = "Код-доступа") {
                        row {
                            alert(
                                kind = FeedbackKind.Warning,
                                title = "Сохраните Код-доступа",
                                text = "По нему вы входите в Систему. Код также отправлен на почту.",
                            )
                        }
                        row {
                            codeInput("Код-доступа", labelSize = 6, size = 18) {
                                value = accessToken
                                isEditable = false
                                isObscured = true
                            }
                        }
                    }
                }
            }
            row { block(title = "Главная") { emptyState("Раздел пока не реализован") } }
        }
    }
}
