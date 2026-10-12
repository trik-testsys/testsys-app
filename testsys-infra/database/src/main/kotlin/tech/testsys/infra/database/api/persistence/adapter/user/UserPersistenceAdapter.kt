package tech.testsys.infra.database.api.persistence.adapter.user

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.SupervisorUserReadPort
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.CommunityId
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
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.touchAggregateRoot
import java.time.Instant
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter searching [User] entities of every kind and recording their logins in [UserJpaEntity] rows.
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
    private val participantRepository: ParticipantRepository,
) : UserRepository, SupervisorUserReadPort {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

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
    override fun countAvailableToAdministrator(administratorId: MultipleRoleUserId, filter: UserFilter): Long =
        userJpaEntityRepository.count(availableTo(administratorId = administratorId, filter = filter))

    @Transactional(readOnly = true)
    override fun findUsers(pagination: Pagination, filter: UserFilter): Page<User<*>> {
        val orders = pagination.sort.orders.map { order ->
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }
        val stableOrders = if (orders.any { order -> order.property == "id" }) orders else orders + JpaSort.Order.asc("id")
        val page = userJpaEntityRepository.findAll(
            supervisorSelection(filter),
            PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders)),
        )
        return Page(assembleSupervisor(page.content), pagination, page.totalElements)
    }

    @Transactional(readOnly = true)
    override fun findUserById(userId: UserId): User<*>? {
        val type = typeOf(userId) ?: return null
        val idAndType = Specification<UserJpaEntity> { entity, _, builder ->
            builder.and(
                builder.equal(entity.get<Long>("id"), userId.value),
                builder.equal(entity.get<UserTypeJpaEnum>("type"), type),
            )
        }
        val row = userJpaEntityRepository.findOne(supervisorSelection(UserFilter()).and(idAndType)).orElse(null) ?: return null
        return assembleSupervisor(listOf(row)).single()
    }

    @Transactional(readOnly = true)
    override fun countUsersInCommunity(communityId: CommunityId): Long =
        userJpaEntityRepository.count(supervisorSelection(UserFilter(communityId = communityId)))

    @Transactional(readOnly = true)
    override fun findLastLogins(userIds: List<UserId>): Map<UserId, Instant> {
        val typesById = userIds.mapNotNull { userId -> typeOf(userId)?.let { type -> userId.value to (userId to type) } }.toMap()
        return userJpaEntityRepository.findAllById(typesById.keys).mapNotNull { jpaEntity ->
            val (userId, type) = typesById.getValue(jpaEntity.requireId())
            jpaEntity.lastLoginAt?.takeIf { jpaEntity.type == type }?.let { lastLoginAt -> userId to lastLoginAt }
        }.toMap()
    }

    @Transactional(readOnly = true)
    override fun existsById(userId: UserId): Boolean {
        val type = typeOf(userId) ?: return false
        return userJpaEntityRepository.existsByIdAndType(id = userId.value, type = type)
    }

    @Transactional
    override fun recordLogin(userId: UserId, loggedInAt: Instant) {
        val type = typeOf(userId) ?: return
        val row = userJpaEntityRepository.findByIdOrNull(userId.value) ?: return
        if (row.type != type) return
        entityManager.touchAggregateRoot(userJpaEntityRepository, userId.value, changesRootData = true)
        userJpaEntityRepository.updateLastLoginAt(id = userId.value, type = type, lastLoginAt = loggedInAt)
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

    /** Selects every non-supervisor user, applying all conditions before pagination and counting. */
    private fun supervisorSelection(filter: UserFilter): Specification<UserJpaEntity> = Specification { entity, query, builder ->
        val grounds = mutableListOf<Predicate>()
        val multipleRoles = filter.roles?.mapNotNull { role ->
            when (role) {
                UserFilter.Role.ADMINISTRATOR -> UserMultipleRoleJpaEnum.ADMINISTRATOR
                UserFilter.Role.DEVELOPER -> UserMultipleRoleJpaEnum.DEVELOPER
                UserFilter.Role.JUDGE -> UserMultipleRoleJpaEnum.JUDGE
                UserFilter.Role.MANAGER -> UserMultipleRoleJpaEnum.MANAGER
                UserFilter.Role.STUDENT -> UserMultipleRoleJpaEnum.STUDENT
                UserFilter.Role.OBSERVER -> null
            }
        }
        if (multipleRoles?.isNotEmpty() != false) {
            val membership = query.subquery(Long::class.java)
            val association = membership.from(MultipleRoleToUserJpaEntity::class.java)
            val associationId = association.get<Any>("id")
            val conditions = mutableListOf(builder.equal(associationId.get<Long>("userId"), entity.get<Long>("id")))
            filter.communityId?.let { conditions.add(builder.equal(associationId.get<Long>("communityId"), it.value)) }
            multipleRoles?.let { conditions.add(associationId.get<UserMultipleRoleJpaEnum>("multipleRole").`in`(it)) }
            membership.select(associationId.get("userId")).where(*conditions.toTypedArray())
            grounds.add(
                builder.and(
                    builder.equal(entity.get<UserTypeJpaEnum>("type"), UserTypeJpaEnum.MULTIPLE_ROLE),
                    builder.exists(membership),
                ),
            )
        }
        if (filter.roles?.contains(UserFilter.Role.ADMINISTRATOR) != false) {
            val owned = query.subquery(Long::class.java)
            val community = owned.from(CommunityJpaEntity::class.java)
            val conditions = mutableListOf(builder.equal(community.get<Long>("ownerId"), entity.get<Long>("id")))
            filter.communityId?.let { conditions.add(builder.equal(community.get<Long>("id"), it.value)) }
            owned.select(community.get("id")).where(*conditions.toTypedArray())
            grounds.add(
                builder.and(
                    builder.equal(entity.get<UserTypeJpaEnum>("type"), UserTypeJpaEnum.MULTIPLE_ROLE),
                    builder.exists(owned),
                ),
            )
        }
        if (filter.roles?.contains(UserFilter.Role.OBSERVER) != false) {
            val observers = query.subquery(Long::class.java)
            val observer = observers.from(ObserverDataJpaEntity::class.java)
            val conditions = mutableListOf(builder.equal(observer.get<Long>("userId"), entity.get<Long>("id")))
            filter.communityId?.let { conditions.add(builder.equal(observer.get<Long>("communityId"), it.value)) }
            observers.select(observer.get("userId")).where(*conditions.toTypedArray())
            grounds.add(builder.exists(observers))
        }
        if (filter.roles == null && filter.communityId == null) {
            val participants = query.subquery(Long::class.java)
            val participant = participants.from(ParticipantDataJpaEntity::class.java)
            participants.select(participant.get("userId"))
                .where(builder.equal(participant.get<Long>("userId"), entity.get<Long>("id")))
            grounds.add(builder.exists(participants))
        }
        val predicates = mutableListOf(builder.or(*grounds.toTypedArray()))
        filter.name?.let { name -> predicates.add(builder.gt(builder.locate(builder.lower(entity.get("name")), name.lowercase()), 0)) }
        builder.and(*predicates.toTypedArray())
    }

    /** Assembles users selected by the supervisor scope in one fetch per concrete kind. */
    private fun assembleSupervisor(rows: List<UserJpaEntity>): List<User<*>> {
        val multipleIds = rows.filter { it.type == UserTypeJpaEnum.MULTIPLE_ROLE }.map { MultipleRoleUserId(it.requireId()) }
        val singleIds = rows.filter { it.type == UserTypeJpaEnum.SINGLE_ROLE }.map { SingleRoleUserId(it.requireId()) }
        val found = buildList<User<*>> {
            if (multipleIds.isNotEmpty()) addAll(multipleRoleUserRepository.findByIds(multipleIds))
            if (singleIds.isNotEmpty()) {
                addAll(observerRepository.findByIds(singleIds))
                addAll(participantRepository.findByIds(singleIds))
            }
        }.associateBy { it.id.value }
        return rows.map { row -> checkNotNull(found[row.requireId()]) { "Selected user id=${row.requireId()} was not assembled" } }
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
