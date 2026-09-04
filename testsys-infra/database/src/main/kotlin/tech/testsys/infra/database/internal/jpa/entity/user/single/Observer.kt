package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * Composite key of [CompetitionToObserverJpaEntity].
 *
 * @property competitionId id of the competition.
 * @property observerId id of the observer.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class CompetitionToObserverId(
    val competitionId: Long,
    val observerId: Long,
) : CompositeId

/**
 * Join row: an observer may watch a competition.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class CompetitionToObserverJpaEntity(id: CompetitionToObserverId) : CompositeJpaEntity<CompetitionToObserverId>(id)

/**
 * JPA entity of [tech.testsys.domain.model.user.ObserverData].
 *
 * @property userId id of the user holding the role.
 * @property communityId id of the community the observer belongs to.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ObserverDataJpaEntity(
    val userId: Long,
    val communityId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
