package tech.testsys.infra.database.api.announcement

import java.time.Instant

/**
 * Storage of the announcements of the sign-in page: listing, the active ones, creation, change and closing.
 *
 * @since %CURRENT_VERSION%
 */
interface Announcements {

    /**
     * Finds every announcement, including future, closed and finished ones.
     *
     * @return the announcements, the latest start of showing first and the larger id first on a tie.
     * @since %CURRENT_VERSION%
     */
    fun findAll(): List<Announcement>

    /**
     * Finds at most three announcements shown at [at]: not closed, from the start of showing inclusive to the end
     * exclusive.
     *
     * @return the announcements, the latest start of showing first and the larger id first on a tie.
     * @since %CURRENT_VERSION%
     */
    fun findActive(at: Instant): List<Announcement>

    /**
     * Creates an announcement with the [text] trimmed, the [kind] and the period from [shownFrom] to [shownUntil].
     *
     * @return the created announcement, or the first reason of refusal, checking the text before the period; nothing
     * is stored on refusal.
     * @since %CURRENT_VERSION%
     */
    fun create(text: String, kind: AnnouncementKind, shownFrom: Instant, shownUntil: Instant): AnnouncementSave

    /**
     * Replaces the text, the kind and the period of the announcement [id], checking them as [create] does.
     *
     * @return the changed announcement, the first reason of refusal or [AnnouncementSave.NotFound]; nothing is stored
     * on refusal.
     * @since %CURRENT_VERSION%
     */
    fun update(id: AnnouncementId, text: String, kind: AnnouncementKind, shownFrom: Instant, shownUntil: Instant): AnnouncementSave

    /**
     * Closes the announcement [id] at [at] if it is not closed and its end of showing is later than [at], including
     * an announcement whose showing has not started yet.
     *
     * @return `true` if this call closed the announcement; `false` if it is missing, already closed or its end of
     * showing is not later than [at], and nothing is changed then.
     * @since %CURRENT_VERSION%
     */
    fun close(id: AnnouncementId, at: Instant): Boolean
}

/**
 * Identifier of an [Announcement].
 *
 * @property value the raw id.
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class AnnouncementId(val value: Long)

/**
 * Announcement of the sign-in page, active from [shownFrom] inclusive to [shownUntil] exclusive unless closed.
 *
 * @property id the identifier of the announcement.
 * @property text the one-line plain text, trimmed.
 * @property kind the kind of the announcement.
 * @property shownFrom the moment the announcement starts being shown.
 * @property shownUntil the moment the announcement stops being shown, later than [shownFrom].
 * @property closedAt the moment the announcement was closed manually, or `null` if it is not closed.
 * @since %CURRENT_VERSION%
 */
data class Announcement(
    val id: AnnouncementId,
    val text: String,
    val kind: AnnouncementKind,
    val shownFrom: Instant,
    val shownUntil: Instant,
    val closedAt: Instant? = null,
)

/**
 * Kind of an [Announcement].
 *
 * @since %CURRENT_VERSION%
 */
enum class AnnouncementKind {

    /**
     * Information.
     *
     * @since %CURRENT_VERSION%
     */
    Info,

    /**
     * Warning.
     *
     * @since %CURRENT_VERSION%
     */
    Warning,
}

/**
 * Result of [Announcements.create] and [Announcements.update].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AnnouncementSave {

    /**
     * The announcement is stored.
     *
     * @property announcement the stored announcement.
     * @since %CURRENT_VERSION%
     */
    data class Saved(val announcement: Announcement) : AnnouncementSave

    /**
     * The announcement is not stored.
     *
     * @property reason the first failed check.
     * @since %CURRENT_VERSION%
     */
    data class Rejected(val reason: AnnouncementRejection) : AnnouncementSave

    /**
     * No announcement has the id.
     *
     * @since %CURRENT_VERSION%
     */
    data object NotFound : AnnouncementSave
}

/**
 * Reason why [Announcements.create] or [Announcements.update] refused an announcement.
 *
 * @since %CURRENT_VERSION%
 */
enum class AnnouncementRejection {

    /**
     * The text is empty after trimming.
     *
     * @since %CURRENT_VERSION%
     */
    BlankText,

    /**
     * The text is longer than 300 code points.
     *
     * @since %CURRENT_VERSION%
     */
    TextTooLong,

    /**
     * The end of showing is not later than its start.
     *
     * @since %CURRENT_VERSION%
     */
    EmptyPeriod,
}
