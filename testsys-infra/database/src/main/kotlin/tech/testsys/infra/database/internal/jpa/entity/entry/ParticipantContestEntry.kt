package tech.testsys.infra.database.internal.jpa.entity.entry

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.time.Instant

/**
 * JPA entity of [tech.testsys.domain.model.entry.ParticipantContestEntry].
 *
 * @property participantId the context identifier.
 * @property competitionId the context identifier.
 * @property contestId the context identifier.
 * @property enteredAt the first entry moment.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ParticipantContestEntryJpaEntity(
    val participantId: Long,
    val competitionId: Long,
    val contestId: Long,
    val enteredAt: Instant,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
