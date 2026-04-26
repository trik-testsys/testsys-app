package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * Composite primary key for [CompetitionToObserverJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class CompetitionToObserverId(
    val competitionId: Long,
    val observerId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity associating an observer with a competition they are allowed to watch.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class CompetitionToObserverJpaEntity(
    id: CompetitionToObserverId,
) : CompositeJpaEntity<CompetitionToObserverId>(id) {

    constructor(competitionId: Long, observerId: Long) : this(CompetitionToObserverId(competitionId, observerId))
}

/**
 * JPA entity representing the observer role data attached to a user.
 *
 * Observers are scoped to a single community via [communityId].
 *
 * @see tech.testsys.domain.model.user.ObserverData
 * @since %CURRENT_VERSION%
 */
@Entity
class ObserverDataJpaEntity(
    val userId: Long,
    val communityId: Long,
) : SequenceJpaEntity()
