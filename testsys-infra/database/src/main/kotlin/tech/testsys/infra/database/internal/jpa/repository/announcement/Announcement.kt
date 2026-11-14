package tech.testsys.infra.database.internal.jpa.repository.announcement

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.announcement.AnnouncementJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import java.time.Instant

/**
 * Repository of the announcements of the sign-in page.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
interface AnnouncementJpaEntityRepository : SnowflakeJpaEntityRepository<AnnouncementJpaEntity> {
    /**
     * Reads every announcement, the latest start of showing first.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByOrderByShownFromDescIdDesc(): List<AnnouncementJpaEntity>

    /**
     * Reads at most [limit] announcements shown at [at], the latest start of showing first.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select a from AnnouncementJpaEntity a where a.shownFrom <= :at and a.shownUntil > :at " +
            "order by a.shownFrom desc, a.id desc",
    )
    fun findActive(@Param("at") at: Instant, limit: Limit): List<AnnouncementJpaEntity>
}
