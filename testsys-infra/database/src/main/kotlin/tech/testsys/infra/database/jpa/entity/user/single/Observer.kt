package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@Embeddable
data class CompetitionToObserverId(
    val competitionId: Long,
    val observerId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class CompetitionToObserverJpaEntity(
    id: CompetitionToObserverId,
) : CompositeJpaEntity<CompetitionToObserverId>(id)

@Entity
class ObserverDataJpaEntity(
    val userId: Long,
    val communityId: Long,
) : SequenceJpaEntity()

