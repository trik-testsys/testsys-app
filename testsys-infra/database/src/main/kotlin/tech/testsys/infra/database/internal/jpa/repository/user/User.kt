package tech.testsys.infra.database.internal.jpa.repository.user

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [UserJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface UserJpaEntityRepository : SnowflakeJpaEntityRepository<UserJpaEntity> {

    /**
     * Locks the user row of [id] for context entry creation.
     *
     * @since %CURRENT_VERSION%
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserJpaEntity u where u.id = :id")
    fun lockById(@Param("id") id: Long): UserJpaEntity?

    /**
     * Finds the user row whose stored access code equals [accessToken] and was produced by [accessTokenHashAlgorithm].
     *
     * @since %CURRENT_VERSION%
     */
    fun findByAccessTokenAndAccessTokenHashAlgorithm(accessToken: String, accessTokenHashAlgorithm: HashAlgorithmJpaEnum): UserJpaEntity?
}
