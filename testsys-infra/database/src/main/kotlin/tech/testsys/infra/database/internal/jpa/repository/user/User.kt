package tech.testsys.infra.database.internal.jpa.repository.user

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
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
     * Finds the user row whose stored access code equals [accessToken] and was produced by [accessTokenHashAlgorithm].
     *
     * @since %CURRENT_VERSION%
     */
    fun findByAccessTokenAndAccessTokenHashAlgorithm(accessToken: String, accessTokenHashAlgorithm: HashAlgorithmJpaEnum): UserJpaEntity?

    /**
     * Finds the user row whose e-mail address equals [email].
     *
     * @since %CURRENT_VERSION%
     */
    fun findByEmail(email: String): UserJpaEntity?

    /**
     * Whether a user row with [id] of the [type] kind exists, checked without loading the row.
     *
     * @since %CURRENT_VERSION%
     */
    fun existsByIdAndType(id: Long, type: UserTypeJpaEnum): Boolean

    /**
     * Increments the versions of the user rows [ids] in one statement and returns the number of updated rows; the
     * caller synchronizes the managed rows through `touchRoots`.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying
    @Query("update UserJpaEntity u set u.version = u.version + 1 where u.id in :ids")
    fun incrementVersions(@Param("ids") ids: Collection<Long>): Int
}
