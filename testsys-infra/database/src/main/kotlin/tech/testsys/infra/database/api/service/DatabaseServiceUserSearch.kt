package tech.testsys.infra.database.api.service

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
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
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.CompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToCompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.StudentToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.AdministratorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.DeveloperDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.JudgeDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.ManagerDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.StudentDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.UserMultipleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.single.ContestToObserverJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ObserverDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.SupervisorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.utils.requireId
import org.springframework.data.domain.Sort as JpaSort

/**
 * Service search of [User] entities of every kind over [UserJpaEntity] rows: one specification selects and counts
 * the rows, and the persistence adapters of the user kinds assemble the found users.
 *
 * @since 1.0.0-beta.1
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DatabaseServiceUserSearch(
    private val userJpaEntityRepository: UserJpaEntityRepository,
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val participantRepository: ParticipantRepository,
    private val observerRepository: ObserverRepository,
    private val supervisorRepository: SupervisorRepository,
) : ServiceUserSearch {

    @Transactional(readOnly = true)
    override fun find(filter: ServiceUserFilter, pagination: Pagination): Page<User<*>> {
        val orders = pagination.sort.orders.map { order ->
            require(order.field in SORTABLE_FIELDS) {
                "Service user search cannot sort by field '${order.field}', expected one of $SORTABLE_FIELDS"
            }
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }

        val stableOrders = if (orders.any { order -> order.property == ID }) orders else orders + JpaSort.Order.asc(ID)
        val pageable = PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders))

        val page = userJpaEntityRepository.findAll(matching(filter), pageable)
        return Page(content = assemble(page.content), pagination = pagination, totalElements = page.totalElements)
    }

    @Transactional(readOnly = true)
    override fun findById(id: ServiceUserId): User<*>? =
        userJpaEntityRepository.findByIdOrNull(id.value)?.let { row -> assemble(listOf(row)).single() }

    /**
     * Selects the user rows matching every condition of [filter], requiring the roles in the community if both are
     * set; conditions are predicates on the user row or `EXISTS` subqueries, so a row is selected once.
     */
    private fun matching(filter: ServiceUserFilter): Specification<UserJpaEntity> = Specification { user, query, builder ->
        val conditions = ServiceUserConditions(user = user, query = query, builder = builder)
        val roles = filter.roles
        val communityId = filter.communityId
        val rolesAndCommunity = if (roles != null && communityId != null) {
            listOf(conditions.holdsAnyIn(roles, communityId))
        } else {
            listOfNotNull(
                roles?.let { requested -> conditions.holdsAny(requested) },
                communityId?.let { community -> conditions.relatedTo(community) },
            )
        }
        val predicates = listOfNotNull(
            filter.id?.let { id -> builder.equal(user.get<Long>(ID), id.value) },
            filter.name?.let { name -> conditions.nameContains(name) },
            filter.email?.let { email -> conditions.emailContains(email) },
            filter.classId?.let { classId -> conditions.relatedTo(classId) },
            filter.competitionId?.let { competitionId -> conditions.relatedTo(competitionId) },
        ) + rolesAndCommunity

        builder.and(*predicates.toTypedArray())
    }

    /**
     * Assembles the users of [rows] in the order of [rows] with the adapters of their kinds, reading each kind at once.
     */
    private fun assemble(rows: List<UserJpaEntity>): List<User<*>> {
        val (multipleRoleRows, singleRoleRows) = rows.partition { row -> row.type == UserTypeJpaEnum.MULTIPLE_ROLE }
        val multipleRoleIds = multipleRoleRows.map { row -> MultipleRoleUserId(row.requireId()) }
        val singleRoleIds = singleRoleRows.map { row -> SingleRoleUserId(row.requireId()) }
        val users: List<User<*>> = findUnlessEmpty(multipleRoleIds, multipleRoleUserRepository::findByIds) +
            findUnlessEmpty(singleRoleIds, participantRepository::findByIds) +
            findUnlessEmpty(singleRoleIds, observerRepository::findByIds) +
            findUnlessEmpty(singleRoleIds, supervisorRepository::findByIds)
        val usersById = users.associateBy { user -> user.id.value }

        return rows.map { row ->
            val userId = row.requireId()
            checkNotNull(usersById[userId]) { "User id=$userId of type ${row.type} selected by the service search was not assembled" }
        }
    }

    private fun <Id, Entity> findUnlessEmpty(ids: List<Id>, find: (List<Id>) -> List<Entity>): List<Entity> =
        if (ids.isEmpty()) emptyList() else find(ids)

    /**
     * Builds the conditions of one search query on the [user] row of [query].
     */
    private class ServiceUserConditions(
        private val user: Root<UserJpaEntity>,
        private val query: CriteriaQuery<*>,
        private val builder: CriteriaBuilder,
    ) {

        private val userId: Path<Long> = user.get(ID)

        fun nameContains(name: String): Predicate = contains(user.get("name"), name)

        fun emailContains(email: String): Predicate {
            val path = user.get<String>("email")
            return builder.and(builder.isNotNull(path), contains(path, email))
        }

        fun holdsAny(roles: Set<ServiceUserRole>): Predicate = builder.or(*roles.map { role -> holds(role) }.toTypedArray())

        /**
         * Users holding any of [roles] in [communityId]; the fixed roles of participants and supervisors are not held
         * in a community, so they select nobody.
         */
        fun holdsAnyIn(roles: Set<ServiceUserRole>, communityId: CommunityId): Predicate {
            val grounds = roles.mapNotNull { role -> holdsIn(role, communityId) }
            return if (grounds.isEmpty()) builder.disjunction() else builder.or(*grounds.toTypedArray())
        }

        /**
         * Members of [communityId] in any non-fixed role, its observers and its owner.
         */
        fun relatedTo(communityId: CommunityId): Predicate = builder.or(
            membershipIn(communityId),
            observes(communityId),
            owns(CommunityJpaEntity::class.java, communityId.value),
        )

        /**
         * Students of [classId] and its owner.
         */
        fun relatedTo(classId: ClassId): Predicate = builder.or(
            exists(StudentToClassJpaEntity::class.java, userIdOf = { row -> row.get<Any>(ID).get("studentId") }) { row, _ ->
                listOf(builder.equal(row.get<Any>(ID).get<Long>("classId"), classId.value))
            },
            owns(ClassJpaEntity::class.java, classId.value),
        )

        /**
         * Participants of [competitionId], its owner and observers assigned at least one contest of it; judgment
         * orders do not relate a judge to a competition.
         */
        fun relatedTo(competitionId: CompetitionId): Predicate = builder.or(
            exists(ParticipantDataJpaEntity::class.java, userIdOf = { row -> row.get("userId") }) { row, _ ->
                listOf(builder.equal(row.get<Long>("competitionId"), competitionId.value))
            },
            owns(CompetitionJpaEntity::class.java, competitionId.value),
            exists(ContestToObserverJpaEntity::class.java, userIdOf = { row -> row.get<Any>(ID).get("observerId") }) { row, subquery ->
                val contest = subquery.from(ContestToCompetitionJpaEntity::class.java).get<Any>(ID)
                listOf(
                    builder.equal(contest.get<Long>("contestId"), row.get<Any>(ID).get<Long>("contestId")),
                    builder.equal(contest.get<Long>("competitionId"), competitionId.value),
                )
            },
        )

        private fun holds(role: ServiceUserRole): Predicate = when (role) {
            ServiceUserRole.ADMINISTRATOR -> multipleRole(AdministratorDataJpaEntity::class.java)
            ServiceUserRole.DEVELOPER -> multipleRole(DeveloperDataJpaEntity::class.java)
            ServiceUserRole.JUDGE -> multipleRole(JudgeDataJpaEntity::class.java)
            ServiceUserRole.MANAGER -> multipleRole(ManagerDataJpaEntity::class.java)
            ServiceUserRole.STUDENT -> multipleRole(StudentDataJpaEntity::class.java)
            ServiceUserRole.PARTICIPANT -> exists(ParticipantDataJpaEntity::class.java, userIdOf = { row -> row.get("userId") })
            ServiceUserRole.OBSERVER -> exists(ObserverDataJpaEntity::class.java, userIdOf = { row -> row.get("userId") })
            ServiceUserRole.SUPERVISOR -> exists(SupervisorDataJpaEntity::class.java, userIdOf = { row -> row.get("userId") })
        }

        /**
         * Whether the user holds [role] in [communityId], or `null` for a fixed role that is not held in a community.
         */
        private fun holdsIn(role: ServiceUserRole, communityId: CommunityId): Predicate? = when (role) {
            ServiceUserRole.ADMINISTRATOR -> builder.or(
                membershipIn(communityId, UserMultipleRoleJpaEnum.ADMINISTRATOR),
                owns(CommunityJpaEntity::class.java, communityId.value),
            )
            ServiceUserRole.DEVELOPER -> membershipIn(communityId, UserMultipleRoleJpaEnum.DEVELOPER)
            ServiceUserRole.JUDGE -> membershipIn(communityId, UserMultipleRoleJpaEnum.JUDGE)
            ServiceUserRole.MANAGER -> membershipIn(communityId, UserMultipleRoleJpaEnum.MANAGER)
            ServiceUserRole.STUDENT -> membershipIn(communityId, UserMultipleRoleJpaEnum.STUDENT)
            ServiceUserRole.OBSERVER -> observes(communityId)
            ServiceUserRole.PARTICIPANT, ServiceUserRole.SUPERVISOR -> null
        }

        /**
         * Whether the user is a member of [communityId], in [role] if it is not `null`.
         */
        private fun membershipIn(communityId: CommunityId, role: UserMultipleRoleJpaEnum? = null): Predicate = builder.and(
            isMultipleRole(),
            exists(MultipleRoleToUserJpaEntity::class.java, userIdOf = { row -> row.get<Any>(ID).get("userId") }) { row, _ ->
                val membership = row.get<Any>(ID)
                listOfNotNull(
                    builder.equal(membership.get<Long>("communityId"), communityId.value),
                    role?.let { requested -> builder.equal(membership.get<UserMultipleRoleJpaEnum>("multipleRole"), requested) },
                )
            },
        )

        private fun observes(communityId: CommunityId): Predicate =
            exists(ObserverDataJpaEntity::class.java, userIdOf = { row -> row.get("userId") }) { row, _ ->
                listOf(builder.equal(row.get<Long>("communityId"), communityId.value))
            }

        private fun <Data : Any> multipleRole(data: Class<Data>): Predicate =
            builder.and(isMultipleRole(), exists(data, userIdOf = { row -> row.get("userId") }))

        private fun isMultipleRole(): Predicate = builder.equal(user.get<UserTypeJpaEnum>("type"), UserTypeJpaEnum.MULTIPLE_ROLE)

        private fun <Group : Any> owns(group: Class<Group>, groupId: Long): Predicate =
            exists(group, userIdOf = { row -> row.get("ownerId") }) { row, _ -> listOf(builder.equal(row.get<Long>(ID), groupId)) }

        private fun contains(path: Path<String>, substring: String): Predicate =
            builder.gt(builder.locate(builder.lower(path), substring.lowercase()), 0)

        /**
         * Whether a row of [entity] refers to the user through [userIdOf] and satisfies [conditions], which may join
         * further roots of the subquery.
         */
        private fun <Row : Any> exists(
            entity: Class<Row>,
            userIdOf: (Root<Row>) -> Path<Long>,
            conditions: (Root<Row>, Subquery<Long>) -> List<Predicate> = { _, _ -> emptyList() },
        ): Predicate {
            val subquery = query.subquery(Long::class.java)
            val row = subquery.from(entity)
            val rowUserId = userIdOf(row)
            val predicates = listOf(builder.equal(rowUserId, userId)) + conditions(row, subquery)
            subquery.select(rowUserId).where(*predicates.toTypedArray())
            return builder.exists(subquery)
        }
    }

    private companion object {

        const val ID = "id"

        val SORTABLE_FIELDS = setOf(ID, "name")
    }
}
