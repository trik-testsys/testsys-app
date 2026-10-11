package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.RouteParameters
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.web.app.service.AdminUserVo
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.ObserverVo
import tech.testsys.web.app.service.ParticipantVo
import tech.testsys.web.app.service.UserListingService
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.multiSelect
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.texts.currentLocalization
import tech.testsys.web.components.texts.currentTimeZone
import java.time.Instant
import java.time.LocalDateTime
import tech.testsys.domain.contract.persistence.Sort as DomainSort

private const val MAX_NAME_CODE_POINTS = 255

/** Route parameter of [AdminView] with the open tab. */
internal const val ADMIN_SECTION_PARAMETER = "section"

/** Route parameter of [AdminCommunityView] with the community id. */
internal const val COMMUNITY_ID_PARAMETER = "communityId"

/** Route parameter of [AdminUserView] with the id of a user with non-fixed roles. */
internal const val USER_ID_PARAMETER = "userId"

/** Route parameter of [AdminUserView] with the open tab of the sections of a role. */
internal const val ADMIN_USER_SECTION_PARAMETER = "section"

/** Route parameter of the observer alias of [AdminUserView] with the observer id. */
internal const val OBSERVER_ID_PARAMETER = "observerId"

/** Message variants naming the community roles a user can be filtered by. */
private val ROLE_NAMES: Map<UserFilter.Role, RoleNameRole> = mapOf(
    UserFilter.Role.ADMINISTRATOR to RoleNameRole.ADMINISTRATOR,
    UserFilter.Role.DEVELOPER to RoleNameRole.DEVELOPER,
    UserFilter.Role.JUDGE to RoleNameRole.JUDGE,
    UserFilter.Role.MANAGER to RoleNameRole.MANAGER,
    UserFilter.Role.STUDENT to RoleNameRole.STUDENT,
    UserFilter.Role.OBSERVER to RoleNameRole.OBSERVER,
)

/** Returns the route parameters of the page of [communityId]. */
internal fun communityParameters(communityId: CommunityId): RouteParameters =
    RouteParameters(COMMUNITY_ID_PARAMETER, communityId.value.toString())

/** Opens the page of the community [communityId]. */
internal fun openAdminCommunity(communityId: CommunityId) {
    UI.getCurrent().navigate(AdminCommunityView::class.java, communityParameters(communityId))
}

/** Returns the route parameters of the page of the user [userId] with non-fixed roles, on [section] if given. */
internal fun userParameters(userId: UserId, section: AdminUserSection? = null): RouteParameters = if (section == null) {
    RouteParameters(USER_ID_PARAMETER, userId.value.toString())
} else {
    RouteParameters(mapOf(USER_ID_PARAMETER to userId.value.toString(), ADMIN_USER_SECTION_PARAMETER to section.value))
}

/** Opens the page of [user]: observers have their own address. */
internal fun openAdminUser(user: AdminUserVo) {
    val parameters = when (user) {
        is MultipleRoleUserVo -> RouteParameters(USER_ID_PARAMETER, user.id.value.toString())
        is ObserverVo -> RouteParameters(OBSERVER_ID_PARAMETER, user.id.value.toString())
        is ParticipantVo -> error("Participants have no administrator route")
    }
    UI.getCurrent().navigate(AdminUserView::class.java, parameters)
}

/** Returns the labels of the roles [user] holds in [communityId]. */
private fun rolesIn(user: AdminUserVo, communityId: CommunityId): List<String> = when (user) {
    is MultipleRoleUserVo -> user.roles.filter { role -> communityId in role.memberOf }.map(::labelOf)
    is ObserverVo -> listOfNotNull(
        currentLocalization().pageCabinet.roleName(role = RoleNameRole.OBSERVER).takeIf { user.community == communityId },
    )
    is ParticipantVo -> emptyList()
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

/** Returns this moment in the time zone of the user of the current UI. */
internal fun Instant.toUserDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, currentTimeZone())

/** Requires a nonblank name of at most 255 Unicode code points, reporting [missing] for an empty or blank one. */
internal fun <T> Binder.BindingBuilder<T, String>.nameRules(missing: String): Binder.BindingBuilder<T, String> = asRequired(missing)
    .withValidator({ name -> name.isNotBlank() }, missing)
    .withValidator(
        { name -> name.codePointCount(0, name.length) <= MAX_NAME_CODE_POINTS },
        currentLocalization().pageCabinet.nameTooLong(max = MAX_NAME_CODE_POINTS),
    )

/** Values of the community creation and editing forms. */
internal class CommunityDraft(var name: String = "", var description: String = "")

/**
 * Fills a cabinet block with users selected by [service], paged and filtered by nickname, roles and, unless [community]
 * fixes it, by one of [communities]. [openUser] opens the role-specific user page when a row is selected.
 */
internal fun BlockScope.adminUsersTable(
    service: UserListingService,
    communities: List<CommunityVo>,
    community: CommunityId?,
    openUser: (AdminUserVo) -> Unit = ::openAdminUser,
) {
    val localization = currentLocalization()
    val texts = localization.pageAdministrator
    val defaults = UserFilter(communityId = community)
    var applied = defaults
    val rows = table(
        key = { (user, _): Pair<AdminUserVo, Instant?> -> user.id },
        fetch = { request ->
            val users = service.viewUsers(request.toPagination(), applied)
            Page(users.content, users.totalElements.toInt())
        },
    ) {
        codeColumn(texts.id(), sortKey = "id", size = if (community == null) 6 else 4) { (user, _) -> user.id.value.toString() }
        textColumn(texts.nickname(), sortKey = "name", size = if (community == null) 10 else 7) { (user, _) -> user.name }
        community?.let { communityId ->
            column(texts.roles(), size = 7) { (user, _) -> rolesIn(user, communityId).forEach { role -> tag(role) } }
        }
        dateTimeColumn(texts.lastLogin(), sortKey = "lastLoginAt") { (_, lastLogin) -> lastLogin?.toUserDateTime() }
        empty(texts.usersEmpty(), texts.usersEmptyHint())
        onRowClick(isNavigation = true) { (user, _) -> openUser(user) }
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
            name = textInput(texts.nickname(), labelSize = 4, size = 8, hint = texts.usersNicknameHint())
            roles = multiSelect(
                texts.roles(),
                labelSize = 4,
                size = 8,
                items = UserFilter.Role.entries,
                itemLabel = { role -> localization.pageCabinet.roleName(role = ROLE_NAMES.getValue(role)) },
            )
        }
        row {
            communityChoice = select(texts.usersCommunity(), labelSize = 4, size = 8, items = communities, itemLabel = CommunityVo::name) {
                isVisible = community == null
            }
        }
    }
}
