package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * Composite primary key for [ContestToCompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ContestToCompetitionId(
    val contestId: Long,
    val competitionId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity representing a contest to competition association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
class ContestToCompetitionJpaEntity(id: ContestToCompetitionId) : CompositeJpaEntity<ContestToCompetitionId>(id)

/**
 * Composite primary key for [ParticipantToCompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class ParticipantToCompetitionId(
    val participantId: Long,
    val competitionId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity representing a participant to competition association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
class ParticipantToCompetitionJpaEntity(id: ParticipantToCompetitionId) : CompositeJpaEntity<ParticipantToCompetitionId>(id)

/**
 * JPA entity representing a competition domain entity.
 *
 * @see tech.testsys.domain.model.group.Competition
 * @see tech.testsys.domain.model.group.CompetitionData
 * @since %CURRENT_VERSION%
 */
@Entity
class CompetitionJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val ownerId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
