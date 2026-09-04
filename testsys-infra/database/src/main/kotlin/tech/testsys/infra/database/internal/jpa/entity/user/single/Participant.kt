package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.user.ParticipantData].
 *
 * @property userId id of the user holding the role.
 * @property competitionId id of the competition the participant takes part in.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ParticipantDataJpaEntity(
    val userId: Long,
    val competitionId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
