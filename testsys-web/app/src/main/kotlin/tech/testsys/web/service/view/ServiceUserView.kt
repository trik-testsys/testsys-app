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
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.infra.database.api.service.ServiceSupervisorRole
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.infra.database.api.service.SupervisorRoleChange
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Service card of a user of any kind with its data read on opening; the access code is not shown. A user with
 * non-fixed roles other than the owner of the public community gets the role of a supervisor granted or revoked
 * after a confirmation (testsys.dev.service.supervisorRole).
 *
 * @since 1.0.0-beta.1
 */
@Route("users/:$SERVICE_USER_ID_PARAMETER")
@PageTitle("Пользователь")
@RolesAllowed("SERVICE")
class ServiceUserView(
    private val headers: ServiceHeaders,
    private val search: ServiceUserSearch,
    private val supervisorRole: ServiceSupervisorRole,
    private val runtime: RuntimeSettings,
) : TestSysView(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = event.routeParameters.get(SERVICE_USER_ID_PARAMETER).orElse(null)
            ?.toLongOrNull()
            ?.let { id -> search.findById(ServiceUserId(id)) }
        if (user == null) {
            event.rerouteToError(NotFoundException::class.java)
            return
        }

        show(user)
    }

    private fun show(user: User<*>) {
        page(headers.header(active = ServiceHeaders.USERS_SECTION)) {
            head("Пользователь «${dataOf(user).name}»") { crumb("Пользователи", ServiceUsersView::class.java) }
            detailsBlock(user)
        }
    }

    private fun PageScope.detailsBlock(user: User<*>) {
        row {
            block(title = "Сведения") {
                if (user is MultipleRoleUser && user.data.email != PublicCommunityOwner.EMAIL) {
                    actions { supervisorRoleAction(user) }
                }
                row {
                    codeInput("ID", labelSize = 3, size = 9) { value = user.id.value.toString() }
                    textInput("Псевдоним", labelSize = 3, size = 9) { value = dataOf(user).name }
                }
                row { field("Роли", labelSize = 3, size = 21) { roleLabelsOf(user).forEach { role -> tag(role) } } }
                row {
                    dateTimeInput("Создан", labelSize = 3, size = 9) { value = user.createdAt.toServerDateTime() }
                    emailOf(user)?.let { email -> textInput("Почта", labelSize = 3, size = 9) { value = email } }
                }

                when (user) {
                    is MultipleRoleUser -> Unit
                    is Participant -> row {
                        codeInput("Соревнование (ID)", labelSize = 3, size = 9) { value = user.data.competition.id.value.toString() }
                    }
                    is Observer -> row {
                        codeInput("Сообщество (ID)", labelSize = 3, size = 9) { value = user.data.community.id.value.toString() }
                        codeInput("Туры (ID)", labelSize = 3, size = 9) {
                            value = user.data.contests.ids.joinToString(", ") { contestId -> contestId.value.toString() }
                        }
                    }
                }
            }.isEditable = false
        }
    }

    /** Adds the action that grants the supervisor role to [user] or revokes it, after a confirmation. */
    private fun ContentScope.supervisorRoleAction(user: MultipleRoleUser) {
        val isSupervisor = user.data.roles.any { role -> role is Supervisor }
        val id = ServiceUserId(user.id.value)
        val publicCommunityId = runtime.publicCommunityId
        if (isSupervisor) {
            action("Снять Роль Супервайзера") {
                onClick {
                    confirm(
                        title = "Снять Роль Супервайзера с Пользователя «${user.data.name}»?",
                        text = "Пользователь потеряет Роль Супервайзера и членство в Публичном Сообществе в этой Роли.",
                        action = "Снять",
                        isDanger = true,
                    ) { report(supervisorRole.revoke(id, publicCommunityId), "Роль Супервайзера снята") }
                }
            }
        } else {
            action("Выдать Роль Супервайзера") {
                onClick {
                    confirm(
                        title = "Выдать Роль Супервайзера Пользователю «${user.data.name}»?",
                        text = "Пользователь станет Супервайзером и членом Публичного Сообщества в этой Роли.",
                        action = "Выдать",
                    ) { report(supervisorRole.grant(id, publicCommunityId), "Роль Супервайзера выдана") }
                }
            }
        }
    }

    private fun report(change: SupervisorRoleChange, success: String) {
        when (change) {
            is SupervisorRoleChange.Changed -> {
                toast(FeedbackKind.Success, success)
                show(change.user)
            }
            SupervisorRoleChange.NotFound -> toast(FeedbackKind.Error, "Пользователь не найден")
            SupervisorRoleChange.NotAllowed -> toast(FeedbackKind.Error, "Этому Пользователю нельзя выдать Роль Супервайзера")
        }
    }
}

/** Returns this moment in the time zone of the server. */
private fun Instant.toServerDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())
