package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class SupervisorRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ParticipantRoleJpaEntity(
    userId: Long,
    val competitionId: Long
) : RoleEntity(userId)

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ObserverRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)
