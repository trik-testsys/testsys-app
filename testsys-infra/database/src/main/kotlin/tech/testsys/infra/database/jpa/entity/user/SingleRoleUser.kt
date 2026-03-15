package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class SupervisorRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class ParticipantRoleJpaEntity(
    userId: Long,
    val competitionId: Long
) : RoleEntity(userId)

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class ObserverRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)
