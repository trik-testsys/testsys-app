package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserData
import tech.testsys.infra.database.api.service.ServiceUserFilter
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserRole
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.multiSelect
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.domain.contract.persistence.Sort as DomainSort

/** Route parameter of [ServiceUserView] with the id of a user of any kind. */
internal const val SERVICE_USER_ID_PARAMETER = "userId"

/** Labels of the roles a user can be searched by, in the order of the filter. */
private val ROLE_LABELS: Map<ServiceUserRole, String> = mapOf(
    ServiceUserRole.ADMINISTRATOR to "Администратор",
    ServiceUserRole.DEVELOPER to "Разработчик",
    ServiceUserRole.JUDGE to "Судья",
    ServiceUserRole.MANAGER to "Организатор",
    ServiceUserRole.STUDENT to "Ученик",
    ServiceUserRole.PARTICIPANT to "Участник",
    ServiceUserRole.OBSERVER to "Наблюдатель",
    ServiceUserRole.SUPERVISOR to "Супервайзер",
)

/**
 * Service search of users of every kind by id, name, e-mail, roles and related community, class or competition;
 * a row opens the card of the user.
 *
 * @since %CURRENT_VERSION%
 */
@Route("users")
@PageTitle("Пользователи")
@RolesAllowed("SERVICE")
class ServiceUsersView(private val headers: ServiceHeaders, private val search: ServiceUserSearch) : TestSysView() {
    private var applied = ServiceUserFilter()

    init {
        page(headers.header(active = ServiceHeaders.USERS_SECTION)) {
            head("Пользователи")
            row {
                block(title = "Пользователи") {
                    val rows = table(
                        key = { user: User<*> -> user.id.value },
                        fetch = { request ->
                            val users = search.find(applied, request.toPagination())
                            Page(users.content, users.totalElements.toInt())
                        },
                    ) {
                        codeColumn("ID", sortKey = "id", size = 5) { user -> user.id.value.toString() }
                        textColumn("Псевдоним", sortKey = "name", size = 7) { user -> dataOf(user).name }
                        textColumn("Почта", size = 6) { user -> emailOf(user) }
                        column("Роли") { user -> roleLabelsOf(user).forEach { role -> tag(role) } }
                        empty("Пользователи не найдены", "Измените условия поиска.")
                        onRowClick(isNavigation = true) { user -> openUser(user) }
                    }

                    searchFilters(onRefresh = { rows.refresh(toFirstPage = true) })
                }
            }
        }
    }

    /** Adds the search conditions; applying them publishes a new filter only if every identifier is a number. */
    private fun BlockScope.searchFilters(onRefresh: () -> Unit) {
        lateinit var id: ValueInput<String>
        lateinit var name: ValueInput<String>
        lateinit var email: ValueInput<String>
        lateinit var roles: ValueInput<Set<ServiceUserRole>>
        lateinit var community: ValueInput<String>
        lateinit var studyClass: ValueInput<String>
        lateinit var competition: ValueInput<String>
        filters(
            onApply = {
                val isValid = listOf(id, community, studyClass, competition).map { field -> field.validateId() }.all { valid -> valid }

                if (isValid) {
                    applied = ServiceUserFilter(
                        id = id.idValue()?.let(::ServiceUserId),
                        name = name.value.ifEmpty { null },
                        email = email.value.ifEmpty { null },
                        roles = roles.value.ifEmpty { null },
                        communityId = community.idValue()?.let(::CommunityId),
                        classId = studyClass.idValue()?.let(::ClassId),
                        competitionId = competition.idValue()?.let(::CompetitionId),
                    )
                }

                isValid
            },
            onReset = {
                listOf(id, name, email, community, studyClass, competition).forEach { field -> field.value = "" }
                listOf(id, community, studyClass, competition).forEach { field -> field.isInvalid = false }
                roles.value = emptySet()
                applied = ServiceUserFilter()
            },
            onRefresh = onRefresh,
        ) {
            row {
                id = textInput("ID", labelSize = 5, size = 7, hint = "Точное совпадение")
                name = textInput("Псевдоним", labelSize = 5, size = 7, hint = "Часть псевдонима, без учёта регистра")
            }
            row {
                email = textInput("Почта", labelSize = 5, size = 7, hint = "Часть почты, без учёта регистра")
                roles = multiSelect("Роли", labelSize = 5, size = 7, items = ServiceUserRole.entries, itemLabel = ROLE_LABELS::getValue)
            }
            row {
                community = textInput("Сообщество (ID)", labelSize = 5, size = 7)
                studyClass = textInput("Класс (ID)", labelSize = 5, size = 7)
            }
            row { competition = textInput("Соревнование (ID)", labelSize = 5, size = 7) }
        }
    }
}

/** Returns the data common to every kind of [user]. */
internal fun dataOf(user: User<*>): UserData = when (user) {
    is MultipleRoleUser -> user.data
    is Participant -> user.data
    is Observer -> user.data
    is Supervisor -> user.data
}

/** Returns the e-mail of [user], or `null` for a user with a fixed role, who has none. */
internal fun emailOf(user: User<*>): String? = when (user) {
    is MultipleRoleUser -> user.data.email
    is SingleRoleUser -> null
}

/** Returns the labels of the roles of [user], one per role. */
internal fun roleLabelsOf(user: User<*>): List<String> = when (user) {
    is MultipleRoleUser -> user.data.roles.map { role ->
        when (role) {
            is Administrator -> ROLE_LABELS.getValue(ServiceUserRole.ADMINISTRATOR)
            is Developer -> ROLE_LABELS.getValue(ServiceUserRole.DEVELOPER)
            is Judge -> ROLE_LABELS.getValue(ServiceUserRole.JUDGE)
            is Manager -> ROLE_LABELS.getValue(ServiceUserRole.MANAGER)
            is Student -> ROLE_LABELS.getValue(ServiceUserRole.STUDENT)
        }
    }
    is Participant -> listOf(ROLE_LABELS.getValue(ServiceUserRole.PARTICIPANT))
    is Observer -> listOf(ROLE_LABELS.getValue(ServiceUserRole.OBSERVER))
    is Supervisor -> listOf(ROLE_LABELS.getValue(ServiceUserRole.SUPERVISOR))
}

/** Opens the card of [user]. */
private fun openUser(user: User<*>) {
    UI.getCurrent().navigate(ServiceUserView::class.java, RouteParameters(SERVICE_USER_ID_PARAMETER, user.id.value.toString()))
}

/** Marks this identifier field invalid unless it is empty or a number after trimming, and returns whether it is valid. */
private fun ValueInput<String>.validateId(): Boolean {
    val isValid = value.isBlank() || idValue() != null
    errorMessage = if (isValid) null else "Введите число"
    isInvalid = !isValid
    return isValid
}

/** Returns the number in this identifier field after trimming, or `null` if it holds no number. */
private fun ValueInput<String>.idValue(): Long? = value.trim().toLongOrNull()

/** Converts a table page request into a pagination with the column sort keys as storage fields. */
private fun PageRequest.toPagination(): Pagination {
    val orders = listOfNotNull(
        sort?.let { order ->
            DomainSort.Order(field = order.key, direction = if (order.isDescending) DomainSort.Direction.DESC else DomainSort.Direction.ASC)
        },
    )
    return Pagination(page = offset / limit, size = limit, sort = DomainSort(orders))
}
