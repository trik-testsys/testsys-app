package tech.testsys.web.service.view

import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.infra.database.api.announcement.Announcement
import tech.testsys.infra.database.api.announcement.AnnouncementKind
import tech.testsys.infra.database.api.announcement.AnnouncementRejection
import tech.testsys.infra.database.api.announcement.AnnouncementSave
import tech.testsys.infra.database.api.announcement.Announcements
import tech.testsys.infra.localization.PageService
import tech.testsys.web.app.view.LoadedTable
import tech.testsys.web.app.view.loadTable
import tech.testsys.web.app.view.pageOf
import tech.testsys.web.app.view.toUserDateTime
import tech.testsys.web.app.view.toUserInstant
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/**
 * Service lists of the announcements of the sign-in page: the current ones, including future ones, and the history of
 * the finished ones with the reason and the moment of finishing. «Добавить» creates an announcement and the row menu
 * of a current one changes or closes it.
 *
 * @since 1.0.0-beta.2
 */
@Route("announcements")
@RolesAllowed("SERVICE")
class ServiceAnnouncementsView(
    private val headers: ServiceHeaders,
    private val announcements: Announcements,
    private val clock: Clock,
) : TestSysView(),
    HasDynamicTitle {
    private val texts: PageService get() = currentLocalization().pageService

    private lateinit var rows: LoadedTable<Announcement>
    private lateinit var history: LoadedTable<Announcement>

    init {
        page(headers.header(active = ServiceHeaders.MENU_SECTION)) {
            head(texts.announcementsTitle())
            row {
                block(title = texts.announcementsCurrent()) {
                    actions { action(texts.add(), icon = IconName.Plus) { onClick { openDialog(announcement = null) } } }
                    rows = loadTable(
                        key = { announcement: Announcement -> announcement.id.value },
                        fetch = { request -> pageOf(announcements.findAll().filter(::isCurrent), request) },
                    ) {
                        dateTimeColumn(texts.announcementsShownFrom(), size = 4) { announcement -> announcement.shownFrom.toUserDateTime() }
                        dateTimeColumn(texts.announcementsShownUntil(), size = 4) { announcement ->
                            announcement.shownUntil.toUserDateTime()
                        }
                        column(texts.announcementsKind(), size = 3) { announcement ->
                            badge(kindLabel(announcement.kind), toneOf(announcement.kind))
                        }
                        textColumn(texts.announcementsLink(), size = LINK_COLUMN_SIZE) { announcement -> announcement.link }
                        textColumn(texts.announcementsText()) { announcement -> announcement.text }
                        menuColumn(ariaLabel = { announcement -> texts.announcementsActions(text = announcement.text) }) { announcement ->
                            item(texts.announcementsEdit()) { openDialog(announcement) }
                            destructiveItem(texts.close()) { close(announcement) }
                        }
                        empty(texts.announcementsEmpty(), texts.announcementsEmptyHint())
                    }
                }
            }
            row {
                block(title = texts.announcementsHistory()) {
                    // ponytail: both lists filter the whole storage list; separate queries if the table grows.
                    history = loadTable(
                        key = { announcement: Announcement -> announcement.id.value },
                        fetch = { request -> pageOf(announcements.findAll().filterNot(::isCurrent), request) },
                    ) {
                        dateTimeColumn(texts.announcementsShownFrom(), size = 4) { announcement -> announcement.shownFrom.toUserDateTime() }
                        dateTimeColumn(texts.announcementsFinishedAt(), size = 4) { announcement ->
                            (announcement.closedAt ?: announcement.shownUntil).toUserDateTime()
                        }
                        column(texts.announcementsReason(), size = 3) { announcement ->
                            val isManual = announcement.closedAt != null
                            val reason = if (isManual) texts.announcementsReasonManual() else texts.announcementsReasonByTime()
                            badge(reason, Tone.Neutral)
                        }
                        column(texts.announcementsKind(), size = 3) { announcement ->
                            badge(kindLabel(announcement.kind), toneOf(announcement.kind))
                        }
                        textColumn(texts.announcementsLink(), size = LINK_COLUMN_SIZE) { announcement -> announcement.link }
                        textColumn(texts.announcementsText()) { announcement -> announcement.text }
                        empty(texts.announcementsHistoryEmpty(), texts.announcementsHistoryEmptyHint())
                    }
                }
            }
        }
    }

    override fun getPageTitle(): String = texts.announcementsTitle()

    private fun isCurrent(announcement: Announcement): Boolean =
        announcement.closedAt == null && announcement.shownUntil.isAfter(clock.instant())

    private fun refreshLists() {
        rows.refresh()
        history.refresh()
    }

    private fun openDialog(announcement: Announcement?) {
        dialog(title = if (announcement == null) texts.announcementsAddTitle() else texts.announcementsEditTitle()) {
            lateinit var text: ValueInput<String>
            lateinit var link: ValueInput<String>
            lateinit var kind: ValueInput<AnnouncementKind?>
            lateinit var shownFrom: ValueInput<LocalDateTime?>
            lateinit var shownUntil: ValueInput<LocalDateTime?>
            row {
                text = textInput(texts.announcementsText(), labelSize = 8, size = 16, hint = texts.announcementsTextHint()) {
                    value = announcement?.text.orEmpty()
                }
            }
            row {
                link = textInput(texts.announcementsLink(), labelSize = 8, size = 16, hint = texts.announcementsLinkHint()) {
                    value = announcement?.link.orEmpty()
                }
            }
            row {
                kind = select(
                    texts.announcementsKind(),
                    labelSize = 8,
                    size = 16,
                    items = AnnouncementKind.entries,
                    itemLabel = ::kindLabel,
                ) {
                    value = announcement?.kind ?: AnnouncementKind.Info
                }
            }
            row {
                shownFrom = dateTimeInput(texts.announcementsShownFrom(), labelSize = 8, size = 16) {
                    value = announcement?.shownFrom?.toUserDateTime()
                }
            }
            row {
                shownUntil = dateTimeInput(texts.announcementsShownUntil(), labelSize = 8, size = 16) {
                    value = announcement?.shownUntil?.toUserDateTime()
                }
            }
            footer { dialog ->
                action(texts.cancel()) { onClick { dialog.close() } }
                mainAction(if (announcement == null) texts.add() else texts.save()) {
                    onClick {
                        kind.showError(texts.announcementsKindRequired().takeIf { kind.value == null })
                        shownFrom.showError(texts.announcementsShownFromRequired().takeIf { shownFrom.value == null })
                        shownUntil.showError(texts.announcementsShownUntilRequired().takeIf { shownUntil.value == null })
                        val chosenKind = kind.value ?: return@onClick
                        val from = shownFrom.value?.toUserInstant() ?: return@onClick
                        val until = shownUntil.value?.toUserInstant() ?: return@onClick

                        val result = save(
                            announcement,
                            text = text.value,
                            link = link.value,
                            kind = chosenKind,
                            shownFrom = from,
                            shownUntil = until,
                        )
                        when (result) {
                            is AnnouncementSave.Saved -> {
                                dialog.close()
                                val saved = if (announcement == null) texts.announcementsAdded() else texts.announcementsChanged()
                                toast(FeedbackKind.Success, saved)
                                refreshLists()
                            }
                            is AnnouncementSave.Rejected -> {
                                val error = rejectionText(result.reason)
                                val field = when (result.reason) {
                                    AnnouncementRejection.BlankText, AnnouncementRejection.TextTooLong -> text
                                    AnnouncementRejection.InvalidLink, AnnouncementRejection.LinkTooLong -> link
                                    AnnouncementRejection.EmptyPeriod -> shownUntil
                                }
                                text.showError(error.takeIf { field == text })
                                link.showError(error.takeIf { field == link })
                                shownUntil.showError(error.takeIf { field == shownUntil })
                            }
                            AnnouncementSave.NotFound -> {
                                dialog.close()
                                toast(FeedbackKind.Error, texts.announcementsNotFound())
                                refreshLists()
                            }
                        }
                    }
                }
            }
        }.open()
    }

    private fun save(
        announcement: Announcement?,
        text: String,
        link: String,
        kind: AnnouncementKind,
        shownFrom: Instant,
        shownUntil: Instant,
    ) = if (announcement == null) {
        announcements.create(text = text, link = link, kind = kind, shownFrom = shownFrom, shownUntil = shownUntil)
    } else {
        announcements.update(announcement.id, text = text, link = link, kind = kind, shownFrom = shownFrom, shownUntil = shownUntil)
    }

    private fun close(announcement: Announcement) {
        confirm(
            title = texts.announcementsCloseTitle(),
            text = texts.announcementsCloseText(text = announcement.text),
            action = texts.close(),
            isDanger = true,
        ) {
            if (announcements.close(announcement.id, clock.instant())) {
                toast(FeedbackKind.Success, texts.announcementsClosed())
            } else {
                toast(FeedbackKind.Error, texts.announcementsAlreadyFinished())
            }
            refreshLists()
        }
    }

    private fun <T> ValueInput<T>.showError(message: String?) {
        isInvalid = message != null
        errorMessage = message
    }

    private fun kindLabel(kind: AnnouncementKind): String = when (kind) {
        AnnouncementKind.Info -> texts.announcementsKindInfo()
        AnnouncementKind.Warning -> texts.announcementsKindWarning()
    }

    private fun toneOf(kind: AnnouncementKind): Tone = when (kind) {
        AnnouncementKind.Info -> Tone.Info
        AnnouncementKind.Warning -> Tone.Warning
    }

    private fun rejectionText(reason: AnnouncementRejection): String = when (reason) {
        AnnouncementRejection.BlankText -> texts.announcementsRejectionBlankText()
        AnnouncementRejection.TextTooLong -> texts.announcementsRejectionTextTooLong()
        AnnouncementRejection.InvalidLink -> texts.announcementsRejectionInvalidLink()
        AnnouncementRejection.LinkTooLong -> texts.announcementsRejectionLinkTooLong()
        AnnouncementRejection.EmptyPeriod -> texts.announcementsRejectionEmptyPeriod()
    }

    private companion object {

        /** Grid share of the link column in both tables. */
        const val LINK_COLUMN_SIZE = 4
    }
}
