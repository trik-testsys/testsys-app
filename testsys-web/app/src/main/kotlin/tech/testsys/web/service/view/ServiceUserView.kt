package tech.testsys.web.service.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.issuedAccessToken
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.copyAction
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Service card of a user of any kind: its data and its access code from the user read on opening, hidden until hover
 * or focus and copied from its field. The access code is never part of the address.
 *
 * @since %CURRENT_VERSION%
 */
@Route("users/:$SERVICE_USER_ID_PARAMETER")
@PageTitle("Пользователь")
@RolesAllowed("SERVICE")
class ServiceUserView(private val search: ServiceUserSearch) : TestSysView(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = event.routeParameters.get(SERVICE_USER_ID_PARAMETER).orElse(null)
            ?.toLongOrNull()
            ?.let { id -> search.findById(ServiceUserId(id)) }
        if (user == null) {
            event.rerouteToError(NotFoundException::class.java)
            return
        }

        page(ServiceHeaders.header()) {
            head("Пользователь «${dataOf(user).name}»") { crumb("Пользователи", ServiceUsersView::class.java) }
            detailsBlock(user)
            accessTokenBlock(user)
        }
    }

    private fun PageScope.detailsBlock(user: User<*>) {
        row {
            block(title = "Сведения") {
                row {
                    codeInput("ID", labelSize = 4, size = 8) { value = user.id.value.toString() }
                    textInput("Псевдоним", labelSize = 4, size = 8) { value = dataOf(user).name }
                }
                row { field("Роли", labelSize = 4, size = 20) { roleLabelsOf(user).forEach { role -> tag(role) } } }
                row {
                    dateTimeInput("Создан", labelSize = 4, size = 8) { value = user.createdAt.toServerDateTime() }
                    emailOf(user)?.let { email -> textInput("Почта", labelSize = 4, size = 8) { value = email } }
                }

                when (user) {
                    is MultipleRoleUser, is Supervisor -> Unit
                    is Participant -> row {
                        codeInput("Соревнование (ID)", labelSize = 4, size = 8) { value = user.data.competition.id.value.toString() }
                    }
                    is Observer -> row {
                        codeInput("Сообщество (ID)", labelSize = 4, size = 8) { value = user.data.community.id.value.toString() }
                        codeInput("Туры (ID)", labelSize = 4, size = 8) {
                            value = user.data.contests.ids.joinToString(", ") { contestId -> contestId.value.toString() }
                        }
                    }
                }
            }.isEditable = false
        }
    }

    /** Adds the access code of [user] in a hidden field and an action copying the value of that field. */
    @RawAccessTokenDependency(reason = "Shows and copies the stored access code of the user as the issued one.")
    private fun PageScope.accessTokenBlock(user: User<*>) {
        row {
            block(title = "Код-доступа") {
                lateinit var code: ValueInput<String>
                row {
                    code = codeInput("Код-доступа", labelSize = 4, size = 12) {
                        value = dataOf(user).accessTokenHash.issuedAccessToken()
                        isObscured = true
                    }
                }

                actions { copyAction("Копировать", value = { code.value }) }
            }.isEditable = false
        }
    }
}

/** Returns this moment in the time zone of the server. */
private fun Instant.toServerDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())
