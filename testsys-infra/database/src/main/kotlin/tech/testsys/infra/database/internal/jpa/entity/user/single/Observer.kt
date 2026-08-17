package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * Composite primary key for [CompetitionToObserverJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class CompetitionToObserverId(
    val competitionId: Long,
    val observerId: Long,
) : CompositeId

/**
 * JPA entity associating an observer with a competition they are allowed to watch.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class CompetitionToObserverJpaEntity(id: CompetitionToObserverId) : CompositeJpaEntity<CompetitionToObserverId>(id)

/**
 * JPA entity representing the observer role data attached to a user.
 *
 * Observers are scoped to a single community via [communityId].
 *
 * @see tech.testsys.domain.model.user.ObserverData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ObserverDataJpaEntity(
    val userId: Long,
    val communityId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
