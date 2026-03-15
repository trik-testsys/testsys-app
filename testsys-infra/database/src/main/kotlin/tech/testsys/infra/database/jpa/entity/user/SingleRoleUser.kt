package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity

/**
 * JPA entity representing a participant role domain entity.
 *
 * @see tech.testsys.domain.model.user.Participant
 * @see tech.testsys.domain.model.user.ParticipantData
 * @since %CURRENT_VERSION%
 */
@Entity
class ParticipantRoleJpaEntity(
    userId: Long,
    val competitionId: Long
) : RoleEntity(userId)

/**
 * JPA entity representing an observer role domain entity.
 *
 * @see tech.testsys.domain.model.user.Observer
 * @see tech.testsys.domain.model.user.ObserverData
 * @since %CURRENT_VERSION%
 */
@Entity
class ObserverRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * JPA entity representing a supervisor role domain entity.
 *
 * @see tech.testsys.domain.model.user.Supervisor
 * @since %CURRENT_VERSION%
 */
@Entity
class SupervisorRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)
