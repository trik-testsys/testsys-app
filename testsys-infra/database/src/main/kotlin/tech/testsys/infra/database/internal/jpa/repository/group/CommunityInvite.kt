package tech.testsys.infra.database.internal.jpa.repository.group

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityInviteJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityInviteRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import java.time.Instant

/**
 * Spring Data repository for [CommunityInviteJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface CommunityInviteJpaEntityRepository : SnowflakeJpaEntityRepository<CommunityInviteJpaEntity> {

    /**
     * Finds the invite of [role] with the stored [code] produced by [codeHashAlgorithm].
     *
     * @since %CURRENT_VERSION%
     */
    fun findByRoleAndCodeAndCodeHashAlgorithm(
        role: CommunityInviteRoleJpaEnum,
        code: String,
        codeHashAlgorithm: HashAlgorithmJpaEnum,
    ): CommunityInviteJpaEntity?

    /**
     * Finds the invites of [role] expiring at or before [now], ordered by id.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByRoleAndExpiresAtLessThanEqualOrderByIdAsc(role: CommunityInviteRoleJpaEnum, now: Instant): List<CommunityInviteJpaEntity>
}
