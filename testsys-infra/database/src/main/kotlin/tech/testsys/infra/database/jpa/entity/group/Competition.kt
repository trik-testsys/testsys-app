package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@Embeddable
data class ContestToCompetitionId(
    val contestId: Long,
    val competitionId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class ContestToCompetitionJpaEntity(
    id: ContestToCompetitionId
) : CompositeJpaEntity<ContestToCompetitionId>(id) {

    constructor(contestId: Long, competitionId: Long): this(ContestToCompetitionId(contestId, competitionId))
}

@Embeddable
data class ParticipantToCompetitionId(
    val participantId: Long,
    val competitionId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class ParticipantToCompetitionJpaEntity(
    id: ParticipantToCompetitionId
) : CompositeJpaEntity<ParticipantToCompetitionId>(id) {

    constructor(participantId: Long, competitionId: Long): this(ParticipantToCompetitionId(participantId, competitionId))
}

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
    val description: String,
    val ownerId: Long,
) : SequenceJpaEntity()
