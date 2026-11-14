package tech.testsys.web.service.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.infra.database.api.announcement.Announcement
import tech.testsys.infra.database.api.announcement.AnnouncementKind
import tech.testsys.infra.database.api.announcement.AnnouncementRejection
import tech.testsys.infra.database.api.announcement.AnnouncementSave
import tech.testsys.infra.database.api.announcement.Announcements
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/**
 * Service lists of the announcements of the sign-in page: the current ones, including future ones, and the history of
 * the finished ones with the reason and the moment of finishing. «Добавить» creates an announcement and the row menu
 * of a current one changes or closes it.
 *
 * @since %CURRENT_VERSION%
 */
@Route("announcements")
@PageTitle("Объявления")
@RolesAllowed("SERVICE")
class ServiceAnnouncementsView(
    private val headers: ServiceHeaders,
    private val announcements: Announcements,
    private val clock: Clock,
) : TestSysView() {
    private lateinit var rows: LoadedTable<Announcement>
    private lateinit var history: LoadedTable<Announcement>

    init {
        page(headers.header(active = ServiceHeaders.ANNOUNCEMENTS_SECTION)) {
            head("Объявления")
            row {
                block(title = "Объявления страницы входа") {
                    actions { action("Добавить", icon = IconName.Plus) { onClick { openDialog(announcement = null) } } }
                    rows = loadTable(
                        key = { announcement: Announcement -> announcement.id.value },
                        fetch = { request -> pageOf(announcements.findAll().filter(::isCurrent), request) },
                    ) {
                        dateTimeColumn("Начало показа", size = 4) { announcement -> announcement.shownFrom.toUserDateTime() }
                        dateTimeColumn("Окончание показа", size = 4) { announcement -> announcement.shownUntil.toUserDateTime() }
                        column("Тип", size = 3) { announcement -> badge(kindLabel(announcement.kind), toneOf(announcement.kind)) }
                        textColumn("Текст") { announcement -> announcement.text }
                        menuColumn(ariaLabel = { announcement -> "Действия с объявлением «${announcement.text}»" }) { announcement ->
                            item("Изменить") { openDialog(announcement) }
                            destructiveItem("Закрыть") { close(announcement) }
                        }
                        empty("Объявлений нет", "Добавьте объявление: оно появится под ссылками страницы входа.")
                    }
                }
            }
            row {
                block(title = "История") {
                    // ponytail: both lists filter the whole storage list; separate queries if the table grows.
                    history = loadTable(
                        key = { announcement: Announcement -> announcement.id.value },
                        fetch = { request -> pageOf(announcements.findAll().filterNot(::isCurrent), request) },
                    ) {
                        dateTimeColumn("Начало показа", size = 4) { announcement -> announcement.shownFrom.toUserDateTime() }
                        dateTimeColumn("Завершено", size = 4) { announcement ->
                            (announcement.closedAt ?: announcement.shownUntil).toUserDateTime()
                        }
                        column("Причина", size = 3) { announcement ->
                            badge(if (announcement.closedAt == null) "По времени" else "Вручную", Tone.Neutral)
                        }
                        column("Тип", size = 3) { announcement -> badge(kindLabel(announcement.kind), toneOf(announcement.kind)) }
                        textColumn("Текст") { announcement -> announcement.text }
                        empty("Завершённых объявлений нет", "Сюда попадут объявления после окончания показа или закрытия.")
                    }
                }
            }
        }
    }

    private fun isCurrent(announcement: Announcement): Boolean =
        announcement.closedAt == null && announcement.shownUntil.isAfter(clock.instant())

    private fun refreshLists() {
        rows.refresh()
        history.refresh()
    }

    private fun openDialog(announcement: Announcement?) {
        dialog(title = if (announcement == null) "Новое объявление" else "Изменение объявления") {
            lateinit var text: ValueInput<String>
            lateinit var kind: ValueInput<AnnouncementKind?>
            lateinit var shownFrom: ValueInput<LocalDateTime?>
            lateinit var shownUntil: ValueInput<LocalDateTime?>
            row {
                text = textInput("Текст", labelSize = 8, size = 16, hint = "Одна строка без форматирования, до 300 символов") {
                    value = announcement?.text.orEmpty()
                }
            }
            row {
                kind = select("Тип", labelSize = 8, size = 16, items = AnnouncementKind.entries, itemLabel = ::kindLabel) {
                    value = announcement?.kind ?: AnnouncementKind.Info
                }
            }
            row {
                shownFrom = dateTimeInput("Начало показа", labelSize = 8, size = 16) {
                    value = announcement?.shownFrom?.toUserDateTime()
                }
            }
            row {
                shownUntil = dateTimeInput("Окончание показа", labelSize = 8, size = 16) {
                    value = announcement?.shownUntil?.toUserDateTime()
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction(if (announcement == null) "Добавить" else "Сохранить") {
                    onClick {
                        kind.showError("Укажите тип".takeIf { kind.value == null })
                        shownFrom.showError("Укажите начало показа".takeIf { shownFrom.value == null })
                        shownUntil.showError("Укажите окончание показа".takeIf { shownUntil.value == null })
                        val chosenKind = kind.value ?: return@onClick
                        val from = shownFrom.value?.toUserInstant() ?: return@onClick
                        val until = shownUntil.value?.toUserInstant() ?: return@onClick

                        val result = save(
                            announcement,
                            text = text.value,
                            kind = chosenKind,
                            shownFrom = from,
                            shownUntil = until,
                        )
                        when (result) {
                            is AnnouncementSave.Saved -> {
                                dialog.close()
                                toast(FeedbackKind.Success, if (announcement == null) "Объявление добавлено" else "Объявление изменено")
                                refreshLists()
                            }
                            is AnnouncementSave.Rejected -> {
                                val error = rejectionText(result.reason)
                                val isPeriodError = result.reason == AnnouncementRejection.EmptyPeriod
                                text.showError(error.takeUnless { isPeriodError })
                                shownUntil.showError(error.takeIf { isPeriodError })
                            }
                            AnnouncementSave.NotFound -> {
                                dialog.close()
                                toast(FeedbackKind.Error, "Объявление не найдено")
                                refreshLists()
                            }
                        }
                    }
                }
            }
        }.open()
    }

    private fun save(announcement: Announcement?, text: String, kind: AnnouncementKind, shownFrom: Instant, shownUntil: Instant) =
        if (announcement == null) {
            announcements.create(text = text, kind = kind, shownFrom = shownFrom, shownUntil = shownUntil)
        } else {
            announcements.update(announcement.id, text = text, kind = kind, shownFrom = shownFrom, shownUntil = shownUntil)
        }

    private fun close(announcement: Announcement) {
        confirm(
            title = "Закрыть объявление?",
            text = "Объявление «${announcement.text}» пропадёт со страницы входа и перейдёт в историю. Вернуть его будет нельзя.",
            action = "Закрыть",
            isDanger = true,
        ) {
            if (announcements.close(announcement.id, clock.instant())) {
                toast(FeedbackKind.Success, "Объявление закрыто")
            } else {
                toast(FeedbackKind.Error, "Объявление уже завершено")
            }
            refreshLists()
        }
    }

    private fun <T> ValueInput<T>.showError(message: String?) {
        isInvalid = message != null
        errorMessage = message
    }

    private fun kindLabel(kind: AnnouncementKind): String = when (kind) {
        AnnouncementKind.Info -> "Информация"
        AnnouncementKind.Warning -> "Предупреждение"
    }

    private fun toneOf(kind: AnnouncementKind): Tone = when (kind) {
        AnnouncementKind.Info -> Tone.Info
        AnnouncementKind.Warning -> Tone.Warning
    }

    private fun rejectionText(reason: AnnouncementRejection): String = when (reason) {
        AnnouncementRejection.BlankText -> "Укажите текст"
        AnnouncementRejection.TextTooLong -> "Текст длиннее 300 символов"
        AnnouncementRejection.EmptyPeriod -> "Окончание показа должно быть позже начала"
    }
}
