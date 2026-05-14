package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the participant role data attached to a user.
 *
 * A participant takes part in exactly one competition, referenced by [competitionId].
 *
 * @see tech.testsys.domain.model.user.ParticipantData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ParticipantDataJpaEntity(
    val userId: Long,
    val competitionId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
