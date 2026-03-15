package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity

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
 * Composite primary key for [ManagerToCompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ManagerToCompetitionId(
    val managerId: Long,
    val competitionId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a manager to competition association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ManagerToCompetitionJpaEntity(
    id: ManagerToCompetitionId,
) : JpaCompositeEntity<ManagerToCompetitionId>(id)

/**
 * Composite primary key for [TaskToCompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class TaskToCompetitionId(
    val taskId: Long,
    val competitionId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a task to competition association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class TaskToCompetitionJpaEntity(
    id: TaskToCompetitionId,
) : JpaCompositeEntity<TaskToCompetitionId>(id)

/**
 * JPA entity representing a competition domain entity.
 *
 * @see tech.testsys.domain.model.group.Competition
 * @see tech.testsys.domain.model.group.CompetitionData
 * @since %CURRENT_VERSION%
 */
@Entity
class CompetitionJpaEntity(
    val ownerId: Long,
) : JpaEntity()
