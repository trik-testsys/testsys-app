package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.StudentDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [StudentDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface StudentDataJpaEntityRepository : SequenceJpaEntityRepository<StudentDataJpaEntity> {

    /**
     * Finds the student data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): StudentDataJpaEntity?
}
