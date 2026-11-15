package tech.testsys.infra.database.api.announcement

import java.time.Instant

/**
 * Storage of the announcements of the sign-in page: listing, the active ones, creation, change and closing.
 *
 * @since 1.0.0-beta.2
 */
interface Announcements {

    /**
     * Finds every announcement, including future, closed and finished ones.
     *
     * @return the announcements, the latest start of showing first and the larger id first on a tie.
     * @since 1.0.0-beta.2
     */
    fun findAll(): List<Announcement>

    /**
     * Finds at most three announcements shown at [at]: not closed, from the start of showing inclusive to the end
     * exclusive.
     *
     * @return the announcements, the latest start of showing first and the larger id first on a tie.
     * @since 1.0.0-beta.2
     */
    fun findActive(at: Instant): List<Announcement>

    /**
     * Creates an announcement with the [text] and the [link] trimmed, the [kind] and the period from [shownFrom]
     * to [shownUntil]; a blank or `null` [link] means no link.
     *
     * @return the created announcement, or the first reason of refusal, checking the text, then the link, then the
     * period; nothing is stored on refusal.
     * @since 1.0.0-beta.2
     */
    fun create(text: String, link: String?, kind: AnnouncementKind, shownFrom: Instant, shownUntil: Instant): AnnouncementSave

    /**
     * Replaces the text, the link, the kind and the period of the announcement [id], checking them as [create] does;
     * a blank or `null` [link] removes the link.
     *
     * @return the changed announcement, the first reason of refusal or [AnnouncementSave.NotFound]; nothing is stored
     * on refusal.
     * @since 1.0.0-beta.2
     */
    fun update(
        id: AnnouncementId,
        text: String,
        link: String?,
        kind: AnnouncementKind,
        shownFrom: Instant,
        shownUntil: Instant,
    ): AnnouncementSave

    /**
     * Closes the announcement [id] at [at] if it is not closed and its end of showing is later than [at], including
     * an announcement whose showing has not started yet.
     *
     * @return `true` if this call closed the announcement; `false` if it is missing, already closed or its end of
     * showing is not later than [at], and nothing is changed then.
     * @since 1.0.0-beta.2
     */
    fun close(id: AnnouncementId, at: Instant): Boolean
}

/**
 * Identifier of an [Announcement].
 *
 * @property value the raw id.
 * @since 1.0.0-beta.2
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
 * @property link the absolute http or https address the announcement opens, trimmed, or `null` if it has no link.
 * @since 1.0.0-beta.2
 */
data class Announcement(
    val id: AnnouncementId,
    val text: String,
    val kind: AnnouncementKind,
    val shownFrom: Instant,
    val shownUntil: Instant,
    val closedAt: Instant? = null,
    val link: String? = null,
)

/**
 * Kind of an [Announcement].
 *
 * @since 1.0.0-beta.2
 */
enum class AnnouncementKind {

    /**
     * Information.
     *
     * @since 1.0.0-beta.2
     */
    Info,

    /**
     * Warning.
     *
     * @since 1.0.0-beta.2
     */
    Warning,
}

/**
 * Result of [Announcements.create] and [Announcements.update].
 *
 * @since 1.0.0-beta.2
 */
sealed interface AnnouncementSave {

    /**
     * The announcement is stored.
     *
     * @property announcement the stored announcement.
     * @since 1.0.0-beta.2
     */
    data class Saved(val announcement: Announcement) : AnnouncementSave

    /**
     * The announcement is not stored.
     *
     * @property reason the first failed check.
     * @since 1.0.0-beta.2
     */
    data class Rejected(val reason: AnnouncementRejection) : AnnouncementSave

    /**
     * No announcement has the id.
     *
     * @since 1.0.0-beta.2
     */
    data object NotFound : AnnouncementSave
}

/**
 * Reason why [Announcements.create] or [Announcements.update] refused an announcement.
 *
 * @since 1.0.0-beta.2
 */
enum class AnnouncementRejection {

    /**
     * The text is empty after trimming.
     *
     * @since 1.0.0-beta.2
     */
    BlankText,

    /**
     * The text is longer than 300 code points.
     *
     * @since 1.0.0-beta.2
     */
    TextTooLong,

    /**
     * The link is not an absolute http or https address with a host and without user information.
     *
     * @since 1.0.0-beta.2
     */
    InvalidLink,

    /**
     * The link is longer than 2048 code points.
     *
     * @since 1.0.0-beta.2
     */
    LinkTooLong,

    /**
     * The end of showing is not later than its start.
     *
     * @since 1.0.0-beta.2
     */
    EmptyPeriod,
}
