package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [ParticipantDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ParticipantDataJpaEntityRepository : SequenceJpaEntityRepository<ParticipantDataJpaEntity> {

    /**
     * Finds the participant data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): ParticipantDataJpaEntity?
}
