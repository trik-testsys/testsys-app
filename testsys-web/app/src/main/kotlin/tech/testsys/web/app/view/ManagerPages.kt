package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.RouteParameters
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.infra.localization.PageManager
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.manager.IssuedParticipantVo
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.lookup
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.PageHeadScope
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization
import tech.testsys.web.components.texts.currentTimeZone
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Route parameter of [ManagerView] with the open tab. */
internal const val MANAGER_SECTION_PARAMETER = "section"

/** Route parameter of [ManagerClassView] and [ManagerContestView] with the class id. */
internal const val CLASS_ID_PARAMETER = "classId"

/** Route parameter of [ManagerCompetitionView] and [ManagerContestView] with the competition id. */
internal const val COMPETITION_ID_PARAMETER = "competitionId"

/** Route parameter of [ManagerContestView] with the contest id. */
internal const val MANAGER_CONTEST_ID_PARAMETER = "contestId"

/** Section of [ManagerView] with the classes. */
internal const val CLASSES_SECTION = "classes"

/** Section of [ManagerView] with the competitions. */
internal const val COMPETITIONS_SECTION = "competitions"

/** Returns the route parameters of the [section] of the Cabinet of a Manager. */
internal fun managerSection(section: String): RouteParameters = RouteParameters(MANAGER_SECTION_PARAMETER, section)

/** Returns the route parameters of the page of the class [classId]. */
internal fun classParameters(classId: ClassId): RouteParameters = RouteParameters(CLASS_ID_PARAMETER, classId.value.toString())

/** Returns the route parameters of the page of the competition [competitionId]. */
internal fun competitionParameters(competitionId: CompetitionId): RouteParameters =
    RouteParameters(COMPETITION_ID_PARAMETER, competitionId.value.toString())

/** Returns the route parameters of the page of [contestId] in the class [classId]. */
internal fun classContestParameters(classId: ClassId, contestId: ContestId): RouteParameters =
    RouteParameters(mapOf(CLASS_ID_PARAMETER to classId.value.toString(), MANAGER_CONTEST_ID_PARAMETER to contestId.value.toString()))

/** Returns the route parameters of the page of [contestId] in the competition [competitionId]. */
internal fun competitionContestParameters(competitionId: CompetitionId, contestId: ContestId): RouteParameters = RouteParameters(
    mapOf(COMPETITION_ID_PARAMETER to competitionId.value.toString(), MANAGER_CONTEST_ID_PARAMETER to contestId.value.toString()),
)

/** Adds the breadcrumbs from the main page to the [section] of the Cabinet of a Manager. */
internal fun PageHeadScope.managerCrumbs(section: String) {
    val localization = currentLocalization()
    val cabinet = localization.pageCabinet
    crumb(cabinet.home(), MultiMainView::class.java)
    crumb(localization.pageManager.title(), ManagerView::class.java)
    val title = if (section == CLASSES_SECTION) cabinet.sectionClasses() else cabinet.sectionCompetitions()
    crumb(title, ManagerView::class.java, managerSection(section))
}

/** Returns the first moment of [date] in the time zone of the user of the current UI. */
internal fun startOf(date: LocalDate): Instant = date.atStartOfDay(currentTimeZone()).toInstant()

/** Returns the last microsecond of [date] in the time zone of the user of the current UI. */
internal fun endOf(date: LocalDate): Instant = startOf(date.plusDays(1)).minusNanos(NANOS_IN_MICROSECOND)

/** Formats [duration] as hours and minutes `Ч:ММ`, or returns `null` without one. */
internal fun formatDuration(duration: Duration?): String? =
    duration?.let { value -> "${value.toHours()}:${value.toMinutesPart().toString().padStart(2, '0')}" }

/**
 * Adds the block of [contests] added to a class or competition: a row opens the page of the contest through [onOpen], and
 * the action of adding a contest adds one shared to the communities of the manager through [onAdd]
 * (testsys.user.multi.manager.addContest).
 */
internal fun PageScope.managerContestsBlock(
    contests: List<ContestVo>,
    service: ManagerService,
    onOpen: (ContestVo) -> Unit,
    onAdd: (ContestVo) -> Unit,
) {
    val localization = currentLocalization()
    val texts = localization.pageManager
    val draft = Binder<ContestDraft>()
    val adding = dialog(title = texts.contestsDialogTitle()) {
        row {
            lookup(
                texts.contestsContest(),
                labelSize = 6,
                size = 18,
                fetch = { query, request ->
                    val found = service.viewAvailableContests(request.toPagination(), query.ifEmpty { null })
                    Page(found.content, found.totalElements.toInt())
                },
                display = ContestVo::name,
                columns = {
                    codeColumn(texts.id(), size = 6) { contest -> contest.id.value.toString() }
                    textColumn(texts.name(), size = 16) { contest -> contest.name }
                },
                hint = texts.contestsLookupHint(),
                dialogSize = DialogSize.L,
            ) {
                draft.forField(this)
                    .asRequired(texts.contestsRequired())
                    .bind({ values -> values.contest }, { values, contest -> values.contest = contest })
            }
        }
        footer { dialog ->
            action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
            mainAction(texts.contestsSubmit()) {
                onClick {
                    val values = ContestDraft()
                    if (draft.writeBeanIfValid(values)) {
                        dialog.close()
                        onAdd(checkNotNull(values.contest))
                    }
                }
            }
        }
    }

    row {
        block(title = localization.pageCabinet.sectionContests()) {
            guide(texts.contestsGuide())
            table(key = { contest: ContestVo -> contest.id }, fetch = { request -> pageOf(contests, request) }) {
                textColumn(texts.name(), size = 9) { contest -> contest.name }
                dateTimeColumn(texts.startsAt(), size = 5) { contest -> contest.startsAt?.toUserDateTime() }
                dateTimeColumn(texts.endsAt(), size = 5) { contest -> contest.endsAt?.toUserDateTime() }
                codeColumn(texts.attemptDuration()) { contest -> formatDuration(contest.attemptDuration) }
                empty(texts.contestsEmpty(), texts.contestsEmptyHint())
                onRowClick(isNavigation = true, listener = onOpen)
            }
            actions {
                action(texts.contestsAdd()) {
                    onClick {
                        draft.readBean(ContestDraft())
                        adding.open()
                    }
                }
            }
        }
    }
}

/** Opens the page of [contestId] in the class [classId]. */
internal fun openClassContest(classId: ClassId, contestId: ContestId) {
    UI.getCurrent().navigate(ManagerContestView::class.java, classContestParameters(classId, contestId))
}

/** Opens the page of [contestId] in the competition [competitionId]. */
internal fun openCompetitionContest(competitionId: CompetitionId, contestId: ContestId) {
    UI.getCurrent().navigate(ManagerContestView::class.java, competitionContestParameters(competitionId, contestId))
}

/**
 * Runs [produce] under the session lock of [ui] and returns its result. A download producer runs in a background
 * thread, and the localization of a UI is used only under its lock, so a producer formats its texts through this call.
 */
internal fun <T> inUiOf(ui: UI, produce: () -> T): T {
    var result: Result<T>? = null
    ui.accessSynchronously { result = runCatching(produce) }
    return checkNotNull(result) { "UI access did not run" }.getOrThrow()
}

/** Values of the contest adding form. */
private class ContestDraft {
    var contest: ContestVo? = null
}

private const val NANOS_IN_MICROSECOND = 1_000L

/** Serializes [rows] as semicolon-separated CSV with a UTF-8 byte order mark and RFC 4180 quoting. */
internal fun csvOf(rows: List<List<String>>): String = rows.joinToString(separator = "", prefix = "﻿") { cells ->
    cells.joinToString(separator = ";", postfix = "\r\n", transform = ::csvCell)
}

/** Exports the identifiers, nicknames and original access codes of just created [participants] with the header from [texts]. */
internal fun participantsCsv(participants: List<IssuedParticipantVo>, texts: PageManager): String = csvOf(
    listOf(listOf(texts.id(), texts.nickname(), texts.accessCode())) + participants.map { (participant, accessToken) ->
        listOf(participant.id.value.toString(), participant.name, accessToken)
    },
)

private fun csvCell(value: String): String =
    if (value.any { char -> char == ';' || char == '"' || char == '\r' || char == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
