package tech.testsys.web.devapp.showcase

import com.vaadin.flow.data.binder.Binder
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.lookup
import tech.testsys.web.components.forms.lookupMany
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.PageScope
import java.time.LocalDate

private const val CONTEST_YEARS = 2

private const val FIRST_CONTEST_YEAR = 2025

private const val WEEKS_BETWEEN_CONTESTS = 2L

private const val TASK_COUNT = 24

private const val TOUR_TASK_COUNT = 5

private val CONTESTS_START: LocalDate = LocalDate.parse("2025-01-13")

private val SEASONS = listOf("Весенний", "Летний", "Осенний", "Зимний", "Открытый")

private val CONTEST_KINDS = listOf("кубок", "тур", "марафон")

private val TASK_TITLES =
    listOf("Кратчайший путь", "Робот в лабиринте", "Движение по линии", "Сумма чисел", "Обход препятствий", "Поиск выхода")

private val TASK_TOPICS = listOf("Графы", "Алгоритмы", "Датчики", "Арифметика")

/** Contest chosen by the showcase lookups. */
private class ShowcaseContest(val id: Int, val name: String, val startsOn: LocalDate, val taskCount: Int)

/** Task chosen by the showcase lookups of several values; a data class, since they match values by `equals`. */
private data class ShowcaseTask(val id: Int, val name: String, val topic: String)

/** Form of the required showcase lookup. */
private class ShowcaseChoice(var contest: ShowcaseContest? = null)

private val SHOWCASE_TASKS: List<ShowcaseTask> = (0 until TASK_COUNT).map { index ->
    ShowcaseTask(
        id = index + 1,
        name = "${TASK_TITLES[index % TASK_TITLES.size]} ${index / TASK_TITLES.size + 1}",
        topic = TASK_TOPICS[index % TASK_TOPICS.size],
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

/** Lookups of a contest (optional, required and read-only with a chosen value) and of several tasks (empty and chosen). */
internal fun PageScope.lookupSection() {
    val choice = ShowcaseChoice()
    val binder = Binder<ShowcaseChoice>()
    row {
        block(title = "Лукап", subtitle = "Выбор тура или нескольких задач в диалоге с поиском по названию") {
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
            row { taskLookup("Задачи", hint = "Несколько задач; отметки сохраняются при поиске и листании") }
            row { taskLookup("Задачи тура") { value = SHOWCASE_TASKS.take(TOUR_TASK_COUNT).toSet() } }
            footer { mainAction("Проверить") { onClick { binder.writeBeanIfValid(choice) } } }
        }
    }

    binder.readBean(choice)
}

private fun BlockRowScope.contestLookup(
    label: String,
    hint: String? = null,
    configure: ValueInput<ShowcaseContest?>.() -> Unit = {},
): ValueInput<ShowcaseContest?> = lookup(
    label,
    labelSize = 4,
    size = 8,
    fetch = { query, request -> pageOf(CONTESTS.filter { contest -> contest.name.contains(query, ignoreCase = true) }, request) },
    display = { contest -> contest.name },
    columns = {
        codeColumn("ID", size = 4) { contest -> "${contest.id}" }
        textColumn("Название", size = 10) { contest -> contest.name }
        dateColumn("Начало", size = 6) { contest -> contest.startsOn }
        numberColumn("Задач", size = 2) { contest -> contest.taskCount }
    },
    hint = hint,
    configure = configure,
)

private fun BlockRowScope.taskLookup(
    label: String,
    hint: String? = null,
    configure: ValueInput<Set<ShowcaseTask>>.() -> Unit = {},
): ValueInput<Set<ShowcaseTask>> = lookupMany(
    label,
    labelSize = 4,
    size = 8,
    fetch = { query, request -> pageOf(SHOWCASE_TASKS.filter { task -> task.name.contains(query, ignoreCase = true) }, request) },
    display = { task -> task.name },
    columns = {
        codeColumn("ID", size = 4) { task -> "${task.id}" }
        textColumn("Название", size = 10) { task -> task.name }
        textColumn("Тема", size = 8) { task -> task.topic }
    },
    hint = hint,
    configure = configure,
)

/** The page of [rows] asked by [request] in their natural order. */
private fun <T> pageOf(rows: List<T>, request: PageRequest): Page<T> = Page(rows.drop(request.offset).take(request.limit), rows.size)
