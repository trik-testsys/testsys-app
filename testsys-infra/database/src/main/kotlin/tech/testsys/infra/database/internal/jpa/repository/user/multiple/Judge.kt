package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.JudgeDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [JudgeDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface JudgeDataJpaEntityRepository : SequenceJpaEntityRepository<JudgeDataJpaEntity> {

    /**
     * Finds the judge data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): JudgeDataJpaEntity?
}
