package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId

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
 * Composite primary key for [ObserverToCompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ObserverToCompetitionId(
    val observerId: Long,
    val competitionId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing an observer to competition association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ObserverToCompetitionJpaEntity(
    id: ObserverToCompetitionId,
) : JpaCompositeEntity<ObserverToCompetitionId>(id)

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
