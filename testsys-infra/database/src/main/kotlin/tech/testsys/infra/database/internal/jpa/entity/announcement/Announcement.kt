package tech.testsys.infra.database.internal.jpa.entity.announcement

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.time.Instant

/**
 * Kind of an [AnnouncementJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class AnnouncementKindJpaEnum {

    INFO,
    WARNING,
}

/**
 * Technical record of an announcement of the sign-in page, independent of domain aggregates.
 *
 * @property text the one-line plain text, trimmed.
 * @property kind the kind of the announcement.
 * @property shownFrom the moment the announcement starts being shown, inclusive.
 * @property shownUntil the moment the announcement stops being shown, exclusive; later than [shownFrom].
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class AnnouncementJpaEntity(
    var text: String,
    @Enumerated(EnumType.STRING)
    var kind: AnnouncementKindJpaEnum,
    var shownFrom: Instant,
    var shownUntil: Instant,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
