package tech.testsys.infra.database.api.persistence.adapter.user

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.UserMultipleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.single.ObserverDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.utils.requireId
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter searching [User] entities of every kind backed by [UserJpaEntity] rows.
 * Found users are assembled by [MultipleRoleUserRepository] and [ObserverRepository].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class UserPersistenceAdapter(
    private val userJpaEntityRepository: UserJpaEntityRepository,
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val observerRepository: ObserverRepository,
) : UserRepository {

    @Transactional(readOnly = true)
    override fun findAvailableToAdministrator(
        administratorId: MultipleRoleUserId,
        pagination: Pagination,
        filter: UserFilter,
    ): Page<User<*>> {
        val specification = availableTo(administratorId = administratorId, filter = filter)
        val orders = pagination.sort.orders.map { order ->
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }
        val stableOrders = if (orders.any { order -> order.property == "id" }) orders else orders + JpaSort.Order.asc("id")
        val pageable = PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders))
        val page = userJpaEntityRepository.findAll(specification, pageable)
        return Page(
            content = assemble(administratorId, page.content),
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun findAvailableToAdministratorById(administratorId: MultipleRoleUserId, userId: UserId): User<*>? {
        val type = typeOf(userId) ?: return null
        val specification = availableTo(administratorId = administratorId, filter = UserFilter()).and(
            Specification { entity, _, builder ->
                builder.and(
                    builder.equal(entity.get<Long>("id"), userId.value),
                    builder.equal(entity.get<UserTypeJpaEnum>("type"), type),
                )
            },
        )
        val jpaEntity = userJpaEntityRepository.findOne(specification).orElse(null) ?: return null
        return assemble(administratorId, listOf(jpaEntity)).single()
    }

    @Transactional(readOnly = true)
    override fun existsById(userId: UserId): Boolean {
        val type = typeOf(userId) ?: return false
        return userJpaEntityRepository.existsByIdAndType(id = userId.value, type = type)
    }

    /**
     * Selects users available to [administratorId] that match the role, community and name conditions of [filter].
     */
    private fun availableTo(administratorId: MultipleRoleUserId, filter: UserFilter): Specification<UserJpaEntity> =
        Specification<UserJpaEntity> { entity, query, builder ->
            val scope = AdministratorScope(administratorId = administratorId.value, filter = filter, builder = builder)
            val grounds = listOfNotNull(
                scope.multipleRoleMembership(entity, query.subquery(Long::class.java)),
                scope.observerMembership(entity, query.subquery(Long::class.java)),
                scope.administratorThemself(entity, query.subquery(Long::class.java)),
            )
            val predicates = mutableListOf(builder.or(*grounds.toTypedArray()))
            filter.name?.let { name ->
                predicates.add(builder.gt(builder.locate(builder.lower(entity.get("name")), name.lowercase()), 0))
            }
            builder.and(*predicates.toTypedArray())
        }

    /**
     * Returns the row kind holding users with ids of the kind of [userId], or `null` for an unknown id kind.
     */
    private fun typeOf(userId: UserId): UserTypeJpaEnum? = when (userId) {
        is MultipleRoleUserId -> UserTypeJpaEnum.MULTIPLE_ROLE
        is SingleRoleUserId -> UserTypeJpaEnum.SINGLE_ROLE
        else -> null
    }

    private fun assemble(administratorId: MultipleRoleUserId, jpaEntities: List<UserJpaEntity>): List<User<*>> {
        val (multipleRoleEntities, singleRoleEntities) =
            jpaEntities.partition { jpaEntity -> jpaEntity.type == UserTypeJpaEnum.MULTIPLE_ROLE }
        val multipleRoleUsers = multipleRoleEntities
            .map { jpaEntity -> MultipleRoleUserId(jpaEntity.requireId()) }
            .takeIf { ids -> ids.isNotEmpty() }
            ?.let { ids -> multipleRoleUserRepository.findByIds(ids) }
            .orEmpty()
        val observers = singleRoleEntities
            .map { jpaEntity -> SingleRoleUserId(jpaEntity.requireId()) }
            .takeIf { ids -> ids.isNotEmpty() }
            ?.let { ids -> observerRepository.findByIds(ids) }
            .orEmpty()
        val usersById: Map<Long, User<*>> = (multipleRoleUsers + observers).associateBy { user -> user.id.value }
        return jpaEntities.map { jpaEntity ->
            val userId = jpaEntity.requireId()
            checkNotNull(usersById[userId]) {
                "User id=$userId of type ${jpaEntity.type} selected for administrator id=${administratorId.value} was not assembled"
            }
        }
    }

    /**
     * Builds the access grounds of one administrator: membership in a community created by [administratorId]
     * that also passes the role and community conditions of [filter].
     */
    private class AdministratorScope(
        private val administratorId: Long,
        private val filter: UserFilter,
        private val builder: CriteriaBuilder,
    ) {

        private val multipleRoles: List<UserMultipleRoleJpaEnum>? = filter.roles?.mapNotNull { role -> multipleRoleOf(role) }

        fun multipleRoleMembership(entity: Root<UserJpaEntity>, membership: Subquery<Long>): Predicate? {
            if (multipleRoles?.isEmpty() == true) return null
            val association = membership.from(MultipleRoleToUserJpaEntity::class.java)
            val associationId = association.get<Any>("id")
            val community = membership.from(CommunityJpaEntity::class.java)
            val conditions = mutableListOf(
                builder.equal(associationId.get<Long>("userId"), entity.get<Long>("id")),
                builder.equal(community.get<Long>("id"), associationId.get<Long>("communityId")),
            )
            conditions.addAll(communityConditions(community))
            multipleRoles?.let { roles -> conditions.add(associationId.get<UserMultipleRoleJpaEnum>("multipleRole").`in`(roles)) }
            membership.select(associationId.get("userId")).where(*conditions.toTypedArray())
            return builder.and(
                builder.equal(entity.get<UserTypeJpaEnum>("type"), UserTypeJpaEnum.MULTIPLE_ROLE),
                builder.exists(membership),
            )
        }

        fun observerMembership(entity: Root<UserJpaEntity>, membership: Subquery<Long>): Predicate? {
            if (!selects(UserFilter.Role.OBSERVER)) return null
            val observerData = membership.from(ObserverDataJpaEntity::class.java)
            val community = membership.from(CommunityJpaEntity::class.java)
            val conditions = mutableListOf(
                builder.equal(observerData.get<Long>("userId"), entity.get<Long>("id")),
                builder.equal(community.get<Long>("id"), observerData.get<Long>("communityId")),
            )
            conditions.addAll(communityConditions(community))
            membership.select(observerData.get("userId")).where(*conditions.toTypedArray())
            return builder.exists(membership)
        }

        fun administratorThemself(entity: Root<UserJpaEntity>, created: Subquery<Long>): Predicate? {
            if (!selects(UserFilter.Role.ADMINISTRATOR)) return null
            val community = created.from(CommunityJpaEntity::class.java)
            created.select(community.get("id")).where(*communityConditions(community).toTypedArray())
            return builder.and(builder.equal(entity.get<Long>("id"), administratorId), builder.exists(created))
        }

        private fun selects(role: UserFilter.Role): Boolean = filter.roles?.contains(role) != false

        private fun communityConditions(community: Path<CommunityJpaEntity>): List<Predicate> = listOfNotNull(
            builder.equal(community.get<Long>("ownerId"), administratorId),
            filter.communityId?.let { communityId -> builder.equal(community.get<Long>("id"), communityId.value) },
        )

        private fun multipleRoleOf(role: UserFilter.Role): UserMultipleRoleJpaEnum? = when (role) {
            UserFilter.Role.ADMINISTRATOR -> UserMultipleRoleJpaEnum.ADMINISTRATOR
            UserFilter.Role.DEVELOPER -> UserMultipleRoleJpaEnum.DEVELOPER
            UserFilter.Role.JUDGE -> UserMultipleRoleJpaEnum.JUDGE
            UserFilter.Role.MANAGER -> UserMultipleRoleJpaEnum.MANAGER
            UserFilter.Role.STUDENT -> UserMultipleRoleJpaEnum.STUDENT
            UserFilter.Role.OBSERVER -> null
        }
    }
}
