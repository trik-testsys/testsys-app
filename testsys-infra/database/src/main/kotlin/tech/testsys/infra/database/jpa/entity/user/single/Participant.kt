package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * Composite primary key for [CompetitionToParticipantJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class CompetitionToParticipantId(
    val competitionId: Long,
    val participantId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity associating a participant with a competition they take part in.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class CompetitionToParticipantJpaEntity(
    id: CompetitionToParticipantId,
) : CompositeJpaEntity<CompetitionToParticipantId>(id) {

    constructor(competitionId: Long, participantId: Long): this(CompetitionToParticipantId(competitionId, participantId))
}

/**
 * JPA entity representing the participant role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.ParticipantData
 * @since %CURRENT_VERSION%
 */
@Entity
class ParticipantDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
