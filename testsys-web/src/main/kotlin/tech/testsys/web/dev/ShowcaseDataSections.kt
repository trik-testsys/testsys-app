package tech.testsys.web.dev

import com.vaadin.flow.data.binder.Binder
import tech.testsys.web.ui.TextHandle
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.actions.destructiveAction
import tech.testsys.web.ui.actions.mainAction
import tech.testsys.web.ui.data.Page
import tech.testsys.web.ui.data.PageRequest
import tech.testsys.web.ui.data.TableHandle
import tech.testsys.web.ui.data.TableScope
import tech.testsys.web.ui.data.table
import tech.testsys.web.ui.display.CounterKind
import tech.testsys.web.ui.display.Tone
import tech.testsys.web.ui.display.badge
import tech.testsys.web.ui.display.counter
import tech.testsys.web.ui.feedback.FeedbackKind
import tech.testsys.web.ui.feedback.toast
import tech.testsys.web.ui.forms.DateRange
import tech.testsys.web.ui.forms.ValueInput
import tech.testsys.web.ui.forms.dateRangeInput
import tech.testsys.web.ui.forms.lookup
import tech.testsys.web.ui.forms.textArea
import tech.testsys.web.ui.forms.textInput
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.overlay.DialogHandle
import tech.testsys.web.ui.overlay.confirm
import tech.testsys.web.ui.overlay.dialog
import java.time.LocalDate
import java.time.LocalDateTime

private const val SUBMISSION_COUNT = 140
private const val FIRST_SUBMISSION_ID = 10_001L
private const val MINUTES_BETWEEN_SUBMISSIONS = 7L
private const val SCORE_STEP = 13
private const val SCORE_LIMIT = 101
private const val SMALL_TABLE_ROWS = 3
private const val CONTEST_YEARS = 2
private const val FIRST_CONTEST_YEAR = 2025
private const val WEEKS_BETWEEN_CONTESTS = 2L
private const val DESCRIPTION_LINES = 4
private const val SCORE_SORT = "score"
private const val TIME_SORT = "time"
private const val DANGER_TOUR_NAME = "Весенний кубок"

private val SHOWCASE_START: LocalDateTime = LocalDateTime.parse("2026-09-01T10:00")
private val CONTESTS_START: LocalDate = LocalDate.parse("2025-01-13")
private val AUTHORS = listOf("Анна Петрова", "Иван Смирнов", "Мария Козлова", "Пётр Волков", "Ольга Новикова")
private val TASKS = listOf("A. Кратчайший путь", "B. Робот в лабиринте", "C. Движение по линии", "D. Сумма чисел")
private val SEASONS = listOf("Весенний", "Летний", "Осенний", "Зимний", "Открытый")
private val CONTEST_KINDS = listOf("кубок", "тур", "марафон")

/** Verdict of a showcase submission with its badge tone. */
private enum class ShowcaseVerdict(val label: String, val tone: Tone) {
    Accepted("Принято", Tone.Success),
    WrongAnswer("Неверный ответ", Tone.Danger),
    TimeLimit("Превышено время", Tone.Warning),
    Running("Проверяется", Tone.Info),
}

/** Submission row of the showcase tables; a running submission has no score yet. */
private class ShowcaseSubmission(
    val id: Long,
    val author: String,
    val task: String,
    val verdict: ShowcaseVerdict,
    val score: Int?,
    val sentAt: LocalDateTime,
)

/** Contest chosen by the showcase lookups. */
private class ShowcaseContest(val id: Int, val name: String, val startsOn: LocalDate, val taskCount: Int)

/** Tour created by the showcase form dialog. */
private class ShowcaseTour(var name: String = "", var period: DateRange = DateRange(), var description: String = "")

/** Form of the required showcase lookup. */
private class ShowcaseChoice(var contest: ShowcaseContest? = null)

private val SUBMISSIONS: List<ShowcaseSubmission> = (0 until SUBMISSION_COUNT).map { index ->
    val verdict = ShowcaseVerdict.entries[index % ShowcaseVerdict.entries.size]
    ShowcaseSubmission(
        id = FIRST_SUBMISSION_ID + index,
        author = AUTHORS[index % AUTHORS.size],
        task = TASKS[index % TASKS.size],
        verdict = verdict,
        score = if (verdict == ShowcaseVerdict.Running) null else index * SCORE_STEP % SCORE_LIMIT,
        sentAt = SHOWCASE_START.plusMinutes(index * MINUTES_BETWEEN_SUBMISSIONS),
    )
}

private val CONTESTS: List<ShowcaseContest> = (0 until SEASONS.size * CONTEST_KINDS.size * CONTEST_YEARS).map { index ->
    val season = SEASONS[index % SEASONS.size]
    val kind = CONTEST_KINDS[index / SEASONS.size % CONTEST_KINDS.size]
    val year = FIRST_CONTEST_YEAR + index / (SEASONS.size * CONTEST_KINDS.size)
    ShowcaseContest(
        id = index + 1,
        name = "$season $kind $year",
        startsOn = CONTESTS_START.plusWeeks(index * WEEKS_BETWEEN_CONTESTS),
        taskCount = index % TASKS.size + 1,
    )
}

/** Paged, filterable and selectable table of submissions, an empty table and a table of one page. */
internal fun PageScope.tableSection() {
    var verdict: ShowcaseVerdict? = null
    lateinit var submissions: TableHandle<ShowcaseSubmission>
    lateinit var selectedCount: TextHandle
    block(title = "Посылки", subtitle = "Сортировка по баллам и времени, фильтр по вердикту, выбор строк") {
        actions {
            selectedCount = counter(value = 0, kind = CounterKind.Neutral)
            action("Все") {
                onClick {
                    verdict = null
                    submissions.refresh(toFirstPage = true)
                }
            }
            ShowcaseVerdict.entries.forEach { option ->
                action(option.label) {
                    onClick {
                        verdict = option
                        submissions.refresh(toFirstPage = true)
                    }
                }
            }
        }
        submissions = table(
            key = { row -> row.id },
            selectable = true,
            fetch = { request -> submissionPage(SUBMISSIONS.filter { row -> verdict == null || row.verdict == verdict }, request) },
        ) {
            submissionColumns()
            onRowClick { row -> toast(FeedbackKind.Info, "Посылка #${row.id}") }
        }
    }
    submissions.onSelectionChange { keys ->
        selectedCount.text = "${keys.size}"
        selectedCount.isVisible = keys.isNotEmpty()
    }
    selectedCount.isVisible = false
    row {
        slot(size = 12) {
            row {
                block(title = "Пустая таблица") {
                    table(key = { row -> row.id }, fetch = { request -> submissionPage(emptyList(), request) }) {
                        submissionColumns()
                        empty("Посылок пока нет")
                    }
                }
            }
        }
        slot(size = 12) {
            row {
                block(title = "Маленькая таблица", subtitle = "Одна страница — без пагинации") {
                    table(key = { row -> row.id }, fetch = { request -> submissionPage(SUBMISSIONS.take(SMALL_TABLE_ROWS), request) }) {
                        codeColumn("ID") { row -> "#${row.id}" }
                        textColumn("Участник") { row -> row.author }
                        column("Вердикт") { row -> badge(row.verdict.label, row.verdict.tone) }
                    }
                }
            }
        }
    }
}

/** Confirmations of every kind and a form dialog that stays open until its fields are valid. */
internal fun PageScope.dialogSection() {
    val newTour = tourDialog()
    block(title = "Диалоги", subtitle = "«Создать» с пустым названием диалог не закрывает") {
        row {
            horizontal {
                action("Подтвердить") {
                    onClick {
                        confirm(title = "Опубликовать тур?", text = "Участники увидят тур в своих кабинетах.", action = "Опубликовать") {
                            toast(FeedbackKind.Success, "Тур опубликован")
                        }
                    }
                }
                destructiveAction("Удалить тур") {
                    onClick {
                        confirm(title = "Удалить тур?", text = "Это действие нельзя отменить.", action = "Удалить", isDanger = true) {
                            toast(FeedbackKind.Success, "Тур удалён")
                        }
                    }
                }
                destructiveAction("Удалить с вводом названия") {
                    onClick {
                        confirm(
                            title = "Удалить тур «$DANGER_TOUR_NAME»?",
                            text = "Вместе с туром удалятся все посылки.",
                            action = "Удалить",
                            isDanger = true,
                            typeToConfirm = DANGER_TOUR_NAME,
                        ) { toast(FeedbackKind.Success, "Тур удалён") }
                    }
                }
                mainAction("Новый тур") { onClick { newTour.open() } }
            }
        }
    }
}

/** Lookups of a contest: optional, required and read-only with a chosen value. */
internal fun PageScope.lookupSection() {
    val choice = ShowcaseChoice()
    val binder = Binder<ShowcaseChoice>()
    block(title = "Лукап", subtitle = "Выбор тура в диалоге с поиском по названию") {
        row { contestLookup("Тур", hint = "Поиск по части названия") }
        row {
            contestLookup("Обязательный") {
                binder.forField(this)
                    .asRequired("Выберите тур")
                    .bind({ source -> source.contest }, { target, value -> target.contest = value })
            }
        }
        row {
            contestLookup("Только чтение") {
                value = CONTESTS.first()
                isEditable = false
            }
        }
        footer { mainAction("Проверить") { onClick { binder.writeBeanIfValid(choice) } } }
    }
    binder.readBean(choice)
}

private fun tourDialog(): DialogHandle {
    val binder = Binder<ShowcaseTour>()
    return dialog(title = "Новый тур", subtitle = "Название обязательно") {
        row {
            textInput("Название", labelSize = 4, size = 8) {
                binder.forField(this)
                    .asRequired("Заполните название")
                    .bind({ source -> source.name }, { target, value -> target.name = value })
            }
        }
        row {
            dateRangeInput("Период", labelSize = 4, size = 8) {
                binder.forField(this).bind({ source -> source.period }, { target, value -> target.period = value })
            }
        }
        row {
            textArea("Описание", labelSize = 4, size = 8, maxLines = DESCRIPTION_LINES) {
                binder.forField(this).bind({ source -> source.description }, { target, value -> target.description = value })
            }
        }
        footer { handle ->
            action("Отмена") { onClick { handle.close() } }
            mainAction("Создать") {
                onClick {
                    val tour = ShowcaseTour()
                    if (binder.writeBeanIfValid(tour)) {
                        toast(kind = FeedbackKind.Success, title = "Тур создан", description = tour.name)
                        handle.close()
                    }
                }
            }
        }
    }
}

private fun BlockRowScope.contestLookup(
    label: String,
    hint: String? = null,
    configure: ValueInput<ShowcaseContest?>.() -> Unit = {},
): ValueInput<ShowcaseContest?> = lookup(
    label,
    labelSize = 4,
    size = 8,
    fetch = { query, request -> contestPage(CONTESTS.filter { contest -> contest.name.contains(query, ignoreCase = true) }, request) },
    display = { contest -> contest.name },
    columns = {
        codeColumn("ID") { contest -> "${contest.id}" }
        textColumn("Название") { contest -> contest.name }
        dateColumn("Начало") { contest -> contest.startsOn }
        numberColumn("Задач") { contest -> contest.taskCount }
    },
    hint = hint,
    configure = configure,
)

private fun TableScope<ShowcaseSubmission>.submissionColumns() {
    codeColumn("ID") { row -> "#${row.id}" }
    textColumn("Участник") { row -> row.author }
    textColumn("Задача") { row -> row.task }
    column("Вердикт") { row -> badge(row.verdict.label, row.verdict.tone) }
    numberColumn("Баллы", sortKey = SCORE_SORT) { row -> row.score }
    dateTimeColumn("Время", sortKey = TIME_SORT) { row -> row.sentAt }
}

/** The page of [rows] asked by [request], sorted by the showcase sort keys. */
private fun submissionPage(rows: List<ShowcaseSubmission>, request: PageRequest): Page<ShowcaseSubmission> {
    val sorted = when (request.sort?.key) {
        SCORE_SORT -> rows.sortedBy { row -> row.score }
        TIME_SORT -> rows.sortedBy { row -> row.sentAt }
        else -> rows
    }
    val ordered = if (request.sort?.isDescending == true) sorted.reversed() else sorted
    return Page(ordered.drop(request.offset).take(request.limit), ordered.size)
}

/** The page of [rows] asked by [request] in their natural order. */
private fun contestPage(rows: List<ShowcaseContest>, request: PageRequest): Page<ShowcaseContest> =
    Page(rows.drop(request.offset).take(request.limit), rows.size)
