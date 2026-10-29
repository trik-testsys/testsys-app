package tech.testsys.infra.database.internal.jpa.repository.session

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.session.CabinetSignInJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import java.time.Instant

/**
 * Repository of the sign-in records; touches and deletions by token or by own user are single statements without a
 * read of the row.
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
interface CabinetSignInJpaEntityRepository : SnowflakeJpaEntityRepository<CabinetSignInJpaEntity> {
    /**
     * Reads the record of [tokenHash], or `null` if there is none.
     *
     * @since 1.0.0-beta.1
     */
    fun findByTokenHash(tokenHash: String): CabinetSignInJpaEntity?

    /**
     * Sets the last request of the record of [tokenHash] to [lastSeenAt] and returns the number of updated rows.
     *
     * @since 1.0.0-beta.1
     */
    @Modifying
    @Query("update CabinetSignInJpaEntity s set s.lastSeenAt = :lastSeenAt where s.tokenHash = :tokenHash")
    fun touch(@Param("tokenHash") tokenHash: String, @Param("lastSeenAt") lastSeenAt: Instant): Int

    /**
     * Deletes the record of [tokenHash] and returns the number of deleted rows.
     *
     * @since 1.0.0-beta.1
     */
    @Modifying
    @Query("delete from CabinetSignInJpaEntity s where s.tokenHash = :tokenHash")
    fun deleteByToken(@Param("tokenHash") tokenHash: String): Int

    /**
     * Deletes the records in which the user [userId] signed in themselves, except the record of [exceptTokenHash],
     * and returns the number of deleted rows.
     *
     * @since 1.0.0-beta.1
     */
    @Modifying
    @Query(
        "delete from CabinetSignInJpaEntity s " +
            "where s.userId = :userId and s.impersonatedBy is null and s.tokenHash <> :exceptTokenHash",
    )
    fun deleteOwnExcept(@Param("userId") userId: Long, @Param("exceptTokenHash") exceptTokenHash: String): Int

    /**
     * Reads the records of the user [userId] together with those in which the supervisor [impersonatedBy] signed in as
     * another user.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByUserIdOrImpersonatedBy(userId: Long, impersonatedBy: Long): List<CabinetSignInJpaEntity>

    /**
     * Deletes the records in which the user [userId] signed in themselves and returns the number of deleted rows.
     *
     * @since 1.0.0-beta.1
     */
    @Modifying
    @Query("delete from CabinetSignInJpaEntity s where s.userId = :userId and s.impersonatedBy is null")
    fun deleteOwn(@Param("userId") userId: Long): Int

    /**
     * Reads the records whose last request was before [before].
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByLastSeenAtBefore(before: Instant): List<CabinetSignInJpaEntity>
}
