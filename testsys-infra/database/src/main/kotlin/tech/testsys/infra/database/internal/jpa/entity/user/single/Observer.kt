package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * Composite key of [ContestToObserverJpaEntity].
 *
 * @property contestId id of the contest.
 * @property observerId id of the observer.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class ContestToObserverId(
    val contestId: Long,
    val observerId: Long,
) : CompositeId

/**
 * Join row: an observer may watch a contest.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class ContestToObserverJpaEntity(id: ContestToObserverId) : CompositeJpaEntity<ContestToObserverId>(id)

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
