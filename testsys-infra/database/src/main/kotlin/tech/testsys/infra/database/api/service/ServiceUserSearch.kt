package tech.testsys.infra.database.api.service

import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.User

/**
 * Service search of users of every kind for the service cabinet; it bypasses the access rules of the cabinets of users.
 *
 * @since 1.0.0-beta.1
 */
interface ServiceUserSearch {

    /**
     * Finds one page of users matching [filter], each user once, ordered by [Pagination.sort] and then by id.
     *
     * @param filter the conditions combined with AND.
     * @param pagination the requested page; only the `id` and `name` fields can be sorted.
     * @return the users of the page as entities of their concrete kinds, with [pagination] and the total number of matches.
     * @throws IllegalArgumentException if [pagination] sorts by another field.
     * @since 1.0.0-beta.1
     */
    fun find(filter: ServiceUserFilter, pagination: Pagination): Page<User<*>>

    /**
     * Finds the user with [id] regardless of the user kind.
     *
     * @param id the id of the user.
     * @return the user as an entity of its concrete kind, or `null` if there is no user with [id].
     * @since 1.0.0-beta.1
     */
    fun findById(id: ServiceUserId): User<*>?
}

/**
 * Optional conditions of [ServiceUserSearch.find] combined with AND; a `null` condition selects every user.
 * Throws [IllegalArgumentException] if [roles] is empty.
 *
 * @property id the exact id of the user.
 * @property name a literal case-insensitive substring of the name of the user.
 * @property email a literal case-insensitive substring of the e-mail; only users with non-fixed roles have one.
 * @property roles the roles of which the user holds at least one; with [communityId], in that community.
 * @property communityId the community the user is a member of in any role, observes or owns; with [roles], the
 * community the user holds one of them in.
 * @property classId the class the user studies in or owns.
 * @property competitionId the competition the user takes part in, owns or observes through an assigned contest of it.
 * @since 1.0.0-beta.1
 */
data class ServiceUserFilter(
    val id: ServiceUserId? = null,
    val name: String? = null,
    val email: String? = null,
    val roles: Set<ServiceUserRole>? = null,
    val communityId: CommunityId? = null,
    val classId: ClassId? = null,
    val competitionId: CompetitionId? = null,
) {

    init {
        require(roles == null || roles.isNotEmpty()) { "Service user role filter must be null or non-empty, but was $roles" }
    }
}

/**
 * Role of a user selected by [ServiceUserFilter.roles]: a non-fixed role or the fixed role of a single-role user.
 *
 * @since 1.0.0-beta.1
 */
enum class ServiceUserRole {
    ADMINISTRATOR,
    DEVELOPER,
    JUDGE,
    MANAGER,
    STUDENT,
    PARTICIPANT,
    OBSERVER,
    SUPERVISOR,
}

/**
 * Identifier of a user of any kind in [ServiceUserSearch].
 *
 * @property value the raw id of the user.
 * @since 1.0.0-beta.1
 */
@JvmInline
value class ServiceUserId(val value: Long)
