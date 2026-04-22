package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@Embeddable
data class CompetitionToParticipantId(
    val competitionId: Long,
    val participantId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class CompetitionToParticipantJpaEntity(
    id: CompetitionToParticipantId,
) : CompositeJpaEntity<CompetitionToParticipantId>(id)

@Entity
class ParticipantDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
