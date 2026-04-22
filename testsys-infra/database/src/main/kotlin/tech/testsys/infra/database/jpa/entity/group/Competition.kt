package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.DescribableJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId

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
) : CompositeJpaEntity<ContestToCompetitionId>(id)

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
) : CompositeJpaEntity<ParticipantToCompetitionId>(id)

/**
 * JPA entity representing a competition domain entity.
 *
 * @see tech.testsys.domain.model.group.Competition
 * @see tech.testsys.domain.model.group.CompetitionData
 * @since %CURRENT_VERSION%
 */
@Entity
class CompetitionJpaEntity(
    name: String,
    description: String,
    val ownerId: Long,
) : DescribableJpaEntity(name, description)
