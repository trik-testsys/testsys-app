package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.RouteParameters
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.web.app.service.administrator.AdminUserVo
import tech.testsys.web.app.service.administrator.AdministratorService
import tech.testsys.web.app.service.administrator.CommunityVo
import tech.testsys.web.app.service.administrator.MultipleRoleUserVo
import tech.testsys.web.app.service.administrator.ObserverVo
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.multiSelect
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import tech.testsys.domain.contract.persistence.Sort as DomainSort

private const val MAX_NAME_CODE_POINTS = 255

/** Route parameter of [AdminView] with the open tab. */
internal const val ADMIN_SECTION_PARAMETER = "section"

/** Route parameter of [AdminCommunityView] with the community id. */
internal const val COMMUNITY_ID_PARAMETER = "communityId"

/** Route parameter of [AdminUserView] with the id of a user with non-fixed roles. */
internal const val USER_ID_PARAMETER = "userId"

/** Route parameter of the observer alias of [AdminUserView] with the observer id. */
internal const val OBSERVER_ID_PARAMETER = "observerId"

/** Labels of the community roles a user can be filtered by. */
private val ROLE_LABELS: Map<UserFilter.Role, String> = mapOf(
    UserFilter.Role.ADMINISTRATOR to "Администратор",
    UserFilter.Role.DEVELOPER to "Разработчик",
    UserFilter.Role.JUDGE to "Судья",
    UserFilter.Role.MANAGER to "Организатор",
    UserFilter.Role.STUDENT to "Ученик",
    UserFilter.Role.OBSERVER to "Наблюдатель",
)

/** Returns the route parameters of the page of [communityId]. */
internal fun communityParameters(communityId: CommunityId): RouteParameters =
    RouteParameters(COMMUNITY_ID_PARAMETER, communityId.value.toString())

/** Opens the page of the community [communityId]. */
internal fun openAdminCommunity(communityId: CommunityId) {
    UI.getCurrent().navigate(AdminCommunityView::class.java, communityParameters(communityId))
}

/** Opens the page of [user]: observers have their own address. */
internal fun openAdminUser(user: AdminUserVo) {
    val parameters = when (user) {
        is MultipleRoleUserVo -> RouteParameters(USER_ID_PARAMETER, user.id.value.toString())
        is ObserverVo -> RouteParameters(OBSERVER_ID_PARAMETER, user.id.value.toString())
    }
    UI.getCurrent().navigate(AdminUserView::class.java, parameters)
}

/** Converts a table page request into a pagination with the column sort keys as storage fields. */
internal fun PageRequest.toPagination(): Pagination {
    val orders = listOfNotNull(
        sort?.let { order ->
            DomainSort.Order(field = order.key, direction = if (order.isDescending) DomainSort.Direction.DESC else DomainSort.Direction.ASC)
        },
    )
    return Pagination(page = offset / limit, size = limit, sort = DomainSort(orders))
}

/** Returns the page of [rows] asked by [request] in their order. */
internal fun <T> pageOf(rows: List<T>, request: PageRequest): Page<T> = Page(rows.drop(request.offset).take(request.limit), rows.size)

/** Returns this moment in the time zone of the server. */
internal fun Instant.toServerDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())

/** Requires a nonblank name of at most 255 Unicode code points, reporting [missing] for an empty or blank one. */
internal fun <T> Binder.BindingBuilder<T, String>.nameRules(missing: String): Binder.BindingBuilder<T, String> = asRequired(missing)
    .withValidator({ name -> name.isNotBlank() }, missing)
    .withValidator({ name -> name.codePointCount(0, name.length) <= MAX_NAME_CODE_POINTS }, "Не длиннее 255 символов")

/** Values of the community creation and editing forms. */
internal class CommunityDraft(var name: String = "", var description: String = "")

/**
 * Fills the block with the users of the communities of the administrator (testsys.user.multi.admin.viewUsers), paged
 * and filtered by nickname, roles and, unless [community] fixes it, by one of [communities]; a row opens the user page.
 */
internal fun BlockScope.adminUsersTable(service: AdministratorService, communities: List<CommunityVo>, community: CommunityId?) {
    val defaults = UserFilter(communityId = community)
    var applied = defaults
    val rows = table(
        key = { (user, _): Pair<AdminUserVo, Instant?> -> user.id },
        fetch = { request ->
            val users = service.viewUsers(request.toPagination(), applied)
            Page(users.content, users.totalElements.toInt())
        },
    ) {
        codeColumn("ID", sortKey = "id", size = 6) { (user, _) -> user.id.value.toString() }
        textColumn("Псевдоним", sortKey = "name", size = 10) { (user, _) -> user.name }
        dateTimeColumn("Последний вход", sortKey = "lastLoginAt") { (_, lastLogin) -> lastLogin?.toServerDateTime() }
        empty("Пользователей нет", "Пользователи появятся, когда присоединятся к сообществу.")
        onRowClick { (user, _) -> openAdminUser(user) }
    }

    lateinit var name: ValueInput<String>
    lateinit var roles: ValueInput<Set<UserFilter.Role>>
    lateinit var communityChoice: ValueInput<CommunityVo?>
    filters(
        onApply = {
            applied = UserFilter(
                name = name.value.ifEmpty { null },
                roles = roles.value.ifEmpty { null },
                communityId = community ?: communityChoice.value?.id,
            )
            true
        },
        onReset = {
            name.value = ""
            roles.value = emptySet()
            communityChoice.value = null
            applied = defaults
        },
        onRefresh = { rows.refresh(toFirstPage = true) },
    ) {
        row {
            name = textInput("Псевдоним", labelSize = 4, size = 8, hint = "Часть псевдонима, без учёта регистра")
            roles = multiSelect("Роли", labelSize = 4, size = 8, items = UserFilter.Role.entries, itemLabel = ROLE_LABELS::getValue)
        }
        row {
            communityChoice = select("Сообщество", labelSize = 4, size = 8, items = communities, itemLabel = CommunityVo::name) {
                isVisible = community == null
            }
        }
    }
}
