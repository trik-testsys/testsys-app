package tech.testsys.infra.database.api.announcement

import org.springframework.data.domain.Limit
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.announcement.AnnouncementJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.announcement.AnnouncementKindJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.announcement.AnnouncementJpaEntityRepository
import java.time.Instant

/**
 * Storage of the announcements of the sign-in page over [AnnouncementJpaEntity] rows; a concurrent change of one
 * announcement keeps the last one.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DatabaseAnnouncements(private val announcementJpaEntityRepository: AnnouncementJpaEntityRepository) : Announcements {

    @Transactional(readOnly = true)
    override fun findAll(): List<Announcement> =
        announcementJpaEntityRepository.findAllByOrderByShownFromDescIdDesc().map { entity -> entity.toAnnouncement() }

    @Transactional(readOnly = true)
    override fun findActive(at: Instant): List<Announcement> =
        announcementJpaEntityRepository.findActive(at, Limit.of(MAX_ACTIVE)).map { entity -> entity.toAnnouncement() }

    @Transactional
    override fun create(text: String, kind: AnnouncementKind, shownFrom: Instant, shownUntil: Instant): AnnouncementSave {
        val trimmed = text.trim()
        rejectionOf(text = trimmed, shownFrom = shownFrom, shownUntil = shownUntil)?.let { reason ->
            return AnnouncementSave.Rejected(reason)
        }

        val saved = announcementJpaEntityRepository.save(
            AnnouncementJpaEntity(text = trimmed, kind = kind.toJpa(), shownFrom = shownFrom, shownUntil = shownUntil),
        )
        return AnnouncementSave.Saved(saved.toAnnouncement())
    }

    @Transactional
    override fun update(
        id: AnnouncementId,
        text: String,
        kind: AnnouncementKind,
        shownFrom: Instant,
        shownUntil: Instant,
    ): AnnouncementSave {
        val entity = announcementJpaEntityRepository.findById(id.value).orElse(null) ?: return AnnouncementSave.NotFound
        val trimmed = text.trim()
        rejectionOf(text = trimmed, shownFrom = shownFrom, shownUntil = shownUntil)?.let { reason ->
            return AnnouncementSave.Rejected(reason)
        }

        entity.text = trimmed
        entity.kind = kind.toJpa()
        entity.shownFrom = shownFrom
        entity.shownUntil = shownUntil
        return AnnouncementSave.Saved(entity.toAnnouncement())
    }

    @Transactional
    override fun delete(id: AnnouncementId): Boolean {
        val entity = announcementJpaEntityRepository.findById(id.value).orElse(null) ?: return false
        announcementJpaEntityRepository.delete(entity)
        return true
    }

    private fun rejectionOf(text: String, shownFrom: Instant, shownUntil: Instant): AnnouncementRejection? = when {
        text.isEmpty() -> AnnouncementRejection.BlankText
        text.codePointCount(0, text.length) > MAX_TEXT_LENGTH -> AnnouncementRejection.TextTooLong
        !shownUntil.isAfter(shownFrom) -> AnnouncementRejection.EmptyPeriod
        else -> null
    }

    private fun AnnouncementJpaEntity.toAnnouncement() = Announcement(
        id = AnnouncementId(checkNotNull(id) { "Stored announcement has no id" }),
        text = text,
        kind = when (kind) {
            AnnouncementKindJpaEnum.INFO -> AnnouncementKind.Info
            AnnouncementKindJpaEnum.WARNING -> AnnouncementKind.Warning
        },
        shownFrom = shownFrom,
        shownUntil = shownUntil,
    )

    private fun AnnouncementKind.toJpa() = when (this) {
        AnnouncementKind.Info -> AnnouncementKindJpaEnum.INFO
        AnnouncementKind.Warning -> AnnouncementKindJpaEnum.WARNING
    }

    private companion object {

        /** Length of the `text` column. */
        const val MAX_TEXT_LENGTH = 300

        /** Number of active announcements the sign-in page shows. */
        const val MAX_ACTIVE = 3
    }
}
