package tech.testsys.infra.database.internal.jpa.repository.group

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassInviteJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import java.time.Instant

/**
 * Spring Data repository for [ClassInviteJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface ClassInviteJpaEntityRepository : SnowflakeJpaEntityRepository<ClassInviteJpaEntity> {

    /**
     * Finds the invite with the stored [code] produced by [codeHashAlgorithm].
     *
     * @since 1.0.0-beta.1
     */
    fun findByCodeAndCodeHashAlgorithm(code: String, codeHashAlgorithm: HashAlgorithmJpaEnum): ClassInviteJpaEntity?

    /**
     * Finds the invites expiring at or before [now], ordered by id.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByExpiresAtLessThanEqualOrderByIdAsc(now: Instant): List<ClassInviteJpaEntity>
}
