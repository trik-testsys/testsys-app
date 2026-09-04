package tech.testsys.infra.database.internal.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * Composite key of [ContestToCompetitionJpaEntity].
 *
 * @property contestId id of the contest.
 * @property competitionId id of the competition.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class ContestToCompetitionId(
    val contestId: Long,
    val competitionId: Long,
) : CompositeId

/**
 * Join row: a contest is assigned to a competition.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class ContestToCompetitionJpaEntity(id: ContestToCompetitionId) : CompositeJpaEntity<ContestToCompetitionId>(id)

/**
 * Composite key of [ParticipantToCompetitionJpaEntity].
 *
 * @property participantId id of the participant.
 * @property competitionId id of the competition.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class ParticipantToCompetitionId(
    val participantId: Long,
    val competitionId: Long,
) : CompositeId

/**
 * Join row: a participant takes part in a competition.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class ParticipantToCompetitionJpaEntity(id: ParticipantToCompetitionId) : CompositeJpaEntity<ParticipantToCompetitionId>(id)

/**
 * JPA entity of [tech.testsys.domain.model.group.Competition].
 *
 * @property name the name of the competition.
 * @property description the description of the competition.
 * @property ownerId id of the manager owning the competition.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class CompetitionJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val ownerId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
