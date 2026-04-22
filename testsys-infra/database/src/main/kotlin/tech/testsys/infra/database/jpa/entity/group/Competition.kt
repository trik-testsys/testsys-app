package tech.testsys.infra.database.jpa.entity.group

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.DescribableJpaEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaSequenceEntity

@Embeddable
data class ContestToCompetitionId(
    val contestId: Long,
    val competitionId: Long,
) : JpaCompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class ContestToCompetitionJpaEntity(
    id: ContestToCompetitionId
) : JpaCompositeEntity<ContestToCompetitionId>(id)

@Embeddable
data class ParticipantToCompetitionId(
    val participantId: Long,
    val competitionId: Long,
) : JpaCompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class ParticipantToCompetitionJpaEntity(
    id: ParticipantToCompetitionId
) : JpaCompositeEntity<ParticipantToCompetitionId>(id)

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
