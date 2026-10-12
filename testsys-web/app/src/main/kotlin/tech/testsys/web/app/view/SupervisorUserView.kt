package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.service.AdminUserVo
import tech.testsys.web.app.service.AdministratorRoleVo
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.DeveloperRoleVo
import tech.testsys.web.app.service.JudgeRoleVo
import tech.testsys.web.app.service.ManagerRoleVo
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.ObserverVo
import tech.testsys.web.app.service.ParticipantVo
import tech.testsys.web.app.service.supervisor.SupervisorService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts
import java.time.Instant

private const val REMOVAL_WARNING = "Пользователь потеряет доступ, который имел только через это сообщество в этой роли."

/**
 * Details of any non-supervisor user, with membership actions only for users with non-fixed roles.
 *
 * @since %CURRENT_VERSION%
 */
@Route("supervisor/users/:userId([0-9]+)/:section?(developer|manager|judge)")
@PageTitle("Пользователь")
@RolesAllowed("SUPERVISOR")
class SupervisorUserView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val service: SupervisorService,
    private val communityConfig: CommunityConfig,
) : TestSysView(texts), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val id = event.routeParameters.getLong(USER_ID_PARAMETER).orElseThrow()
        val section = event.routeParameters.get(ADMIN_USER_SECTION_PARAMETER).orElse(null)
            ?.let { value -> AdminUserSection.entries.first { it.value == value } }
        if (!show(id, section)) {
            event.forwardTo(SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, id.toString()))
        }
    }

    /** Shows a section; returns false if the selected user does not hold the requested role. */
    private fun show(id: Long, section: AdminUserSection? = null): Boolean {
        val (user, login) = service.viewUserByIdValue(id)
        val communities = service.viewCommunities().map { it.first }
        val sections = if (user is MultipleRoleUserVo) {
            AdminUserSection.entries.filter { entry ->
                user.roles.any { role ->
                    when (entry) {
                        AdminUserSection.Developer -> role is DeveloperRoleVo
                        AdminUserSection.Manager -> role is ManagerRoleVo
                        AdminUserSection.Judge -> role is JudgeRoleVo
                    }
                }
            }
        } else {
            emptyList()
        }
        if (section != null && section !in sections) {
            return false
        }
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("${kindOf(user)} «${user.name}»") {
                crumb("Главная", SupervisorView::class.java)
                crumb("Кабинет Супервайзера", SupervisorView::class.java)
                crumb("Пользователи", SupervisorView::class.java, RouteParameters(ADMIN_SECTION_PARAMETER, "users"))
                if (sections.isNotEmpty()) {
                    tabs(matchRouteParameters = true) {
                        tab("Сведения", SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, id.toString()))
                        sections.forEach { entry ->
                            tab(
                                entry.label,
                                SupervisorUserView::class.java,
                                RouteParameters(
                                    mapOf(USER_ID_PARAMETER to id.toString(), ADMIN_USER_SECTION_PARAMETER to entry.value),
                                ),
                            )
                        }
                    }
                }
            }
            when (section) {
                AdminUserSection.Developer -> {
                    tasksBlock(service, user.id)
                    contestsBlock(service, user.id)
                }
                AdminUserSection.Manager -> {
                    classesBlock(service, user.id)
                    competitionsBlock(service, user.id)
                }
                AdminUserSection.Judge -> judgmentsBlock(service, user.id)
                null -> {
                    detailsBlock(user, login)
                    if (user is ObserverVo) {
                        assignedContestsBlock(service, user.id)
                    }
                }
            }
            if (section == null && user is MultipleRoleUserVo) {
                row {
                    block(title = "Роли в сообществах") {
                        val memberships = communities.mapNotNull { community ->
                            user.roles.filter { community.id in it.memberOf }.takeIf { it.isNotEmpty() }
                                ?.let { roles -> community to roles }
                        }
                        table(
                            key = { (community, _) -> community.id },
                            fetch = { request -> pageOf(memberships, request) },
                        ) {
                            textColumn("Сообщество", size = 12) { (community, _) -> community.name }
                            column("Роли") { (_, roles) -> roles.forEach { role -> tag(labelOf(role)) } }
                            menuColumn(
                                ariaLabel = { (community, _) -> "Роли в сообществе «${community.name}»" },
                            ) { (community, roles) ->
                                val removable = roles.mapNotNull { role -> communityRoleOf(role)?.let { it to labelOf(role) } }
                                if (community.id == communityConfig.publicCommunityId || removable.isEmpty()) {
                                    item("Нет ролей для исключения", isEnabled = false) {}
                                } else {
                                    removable.forEach { (role, label) ->
                                        destructiveItem("Исключить в роли $label") {
                                            confirm(
                                                title = "Исключить «${user.name}» из сообщества «${community.name}» в роли $label?",
                                                text = REMOVAL_WARNING,
                                                action = "Исключить",
                                                isDanger = true,
                                            ) {
                                                service.removeFromCommunity(user.id, community.id, role)
                                                toast(
                                                    FeedbackKind.Success,
                                                    "Пользователь исключён из сообщества",
                                                    "Роль: $label.",
                                                )
                                                show(id)
                                            }
                                        }
                                    }
                                }
                            }
                            empty("Ролей в сообществах нет")
                            onRowClick(isNavigation = true) { (community, _) -> openSupervisorCommunity(community.id) }
                        }
                        val add = addDialog(user, communities) { show(id) }
                        actions {
                            action("Включить в сообщество") { onClick { add() } }
                            if (user.roles.none { it is AdministratorRoleVo }) {
                                action("Назначить администратором") {
                                    onClick {
                                        confirm(
                                            title = "Назначить «${user.name}» администратором?",
                                            text = "Система создаст сообщество администратора. Снять роль администратора нельзя.",
                                            action = "Назначить",
                                        ) {
                                            service.grantAdministrator(user.id)
                                            toast(FeedbackKind.Success, "Пользователь назначен администратором")
                                            show(id)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (section == null && user is ObserverVo) {
                row {
                    block(title = "Сообщество") {
                        val community = communities.firstOrNull { it.id == user.community }
                        row {
                            textInput("Название", labelSize = 6, size = 18) {
                                value = community?.name.orEmpty()
                                isEditable = false
                            }
                        }
                    }
                }
            } else if (section == null && user is ParticipantVo) {
                row {
                    block(title = "Соревнование") {
                        row {
                            codeInput("ID", labelSize = 6, size = 18) {
                                value = user.competition.value.toString()
                                isEditable = false
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    @RawAccessTokenDependency(reason = "Shows the original observer or participant access code while Identity hashing is in use.")
    private fun PageScope.detailsBlock(user: AdminUserVo, login: Instant?) {
        row {
            block(title = "Сведения") {
                row {
                    codeInput("ID", labelSize = 3, size = 6) {
                        value = user.id.value.toString()
                        isEditable = false
                    }
                    textInput("Псевдоним", labelSize = 3, size = 8) {
                        value = user.name
                        isEditable = false
                    }
                }
                row {
                    dateTimeInput("Последний вход", labelSize = 3, size = 6) {
                        value = login?.toServerDateTime()
                        isEditable = false
                    }
                }
                if (user is ObserverVo || user is ParticipantVo) {
                    row {
                        codeInput("Код-доступа", labelSize = 3, size = 6) {
                            value = user.accessTokenHash.value
                            isObscured = true
                            isEditable = false
                        }
                    }
                }
            }
        }
    }

    private fun addDialog(user: MultipleRoleUserVo, communities: List<CommunityVo>, onAdded: () -> Unit): () -> Unit {
        val draft = Binder<MembershipDraft>()
        val labels = mapOf(
            CommunityRole.Administrator to "Администратор",
            CommunityRole.Developer to "Разработчик",
            CommunityRole.Manager to "Организатор",
            CommunityRole.Judge to "Судья",
            CommunityRole.Student to "Ученик",
        )
        val dialog = dialog(title = "Включение в сообщество") {
            row {
                select("Сообщество", labelSize = 6, size = 18, items = communities, itemLabel = CommunityVo::name) {
                    draft.forField(this).asRequired("Выберите сообщество")
                        .bind({ it.community }, { bean, selected -> bean.community = selected })
                }
            }
            row {
                select("Роль", labelSize = 6, size = 18, items = CommunityRole.entries, itemLabel = labels::getValue) {
                    draft.forField(this).asRequired("Выберите роль")
                        .bind({ it.role }, { bean, selected -> bean.role = selected })
                }
            }
            footer { window ->
                action("Отменить") { onClick { window.close() } }
                mainAction("Включить") {
                    onClick {
                        val values = MembershipDraft()
                        if (draft.writeBeanIfValid(values)) {
                            service.addToCommunity(user.id, checkNotNull(values.community).id, checkNotNull(values.role))
                            toast(FeedbackKind.Success, "Пользователь включён в сообщество")
                            window.close()
                            onAdded()
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(MembershipDraft())
            dialog.open()
        }
    }

    private fun kindOf(user: AdminUserVo): String = when (user) {
        is MultipleRoleUserVo -> "Пользователь"
        is ObserverVo -> "Наблюдатель"
        is ParticipantVo -> "Участник"
    }

    private class MembershipDraft {
        var community: CommunityVo? = null
        var role: CommunityRole? = null
    }
}
