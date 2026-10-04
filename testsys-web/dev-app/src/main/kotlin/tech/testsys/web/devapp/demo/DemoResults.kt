package tech.testsys.web.devapp.demo

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.ColumnWidth
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.PageScope

/** Shared participant-by-task result matrix for organizer and observer contexts. */
internal fun BlockScope.demoResultMatrix(context: DemoContext, actor: DemoUser, tour: DemoTour, participants: List<DemoUser>) {
    val tasks = tour.taskIds.map { id -> context.state.tasks.first { it.id == id } }
    val rows = participants.map { DemoRow(id = it.id, title = it.alias) }
    demoTable(
        state = context.table("${actor.id}:${tour.id}:matrix"),
        rows = rows,
        columns = {
            textColumn("Участник", width = ColumnWidth.Medium) { it.title }
            tasks.forEach { task ->
                column(task.name, width = ColumnWidth.Narrow) { row ->
                    val score = bestScore(solutions = context.state.solutions, userId = row.id, taskId = task.id)
                    if (score == null) {
                        text("Нет результата")
                    } else {
                        verdict(score.toDouble(), "баллов")
                    }
                    val count = attemptCount(solutions = context.state.solutions, userId = row.id, taskId = task.id)
                    text("Решений: $count")
                }
            }
        },
    )
}

/** Judge selection remains outside the filtered list and retains its mock materials. */
internal fun PageScope.demoJudge(context: DemoContext, actor: DemoUser, objects: DemoObjects, section: String) {
    val selectionKey = "${actor.id}:solution"
    if (section in listOf("overview", "solutions")) {
        demoSolutionTable(context, actor, objects.solutions) { row ->
            context.session.selections[selectionKey] = row.id
            context.navigate("judge.result")
        }
    }
    val selectedId = context.selection(key = selectionKey, fallback = objects.solutions.firstOrNull()?.id)
    val solution = objects.solutions.firstOrNull { it.id == selectedId } ?: return
    if (section in listOf("overview", "result")) {
        demoInfo(
            "Результат проверки",
            listOf(
                "Решение" to solution.id,
                "Файл" to solution.fileName,
                "Отправлено" to solution.submittedAt,
                "Логи проверки" to if (solution.status == "Checked") {
                    "Демонстрационная проверка завершена"
                } else {
                    "Материалы проверки отсутствуют"
                },
                "Видеозапись" to "Не приложена к этому демонстрационному решению",
            ),
            wideLabels = setOf("Логи проверки", "Видеозапись"),
        )
        block("Вердикт") {
            row {
                horizontal {
                    if (solution.status == "Checked") {
                        verdict(checkNotNull(solution.score).toDouble(), "баллов")
                    } else {
                        badge(demoStatusLabel(solution.status), demoStatusTone(solution.status))
                    }
                }
            }
            footer {
                action("К списку решений") {
                    onClick { context.navigate("judge.solutions") }
                }
            }
        }
    }
}

/** Observer matrix uses only assigned tours and the corresponding participants. */
internal fun PageScope.demoObserverResults(context: DemoContext, actor: DemoUser, objects: DemoObjects) {
    val tourKey = "${actor.id}:observer-tour"
    block("Доступные туры") {
        demoTable(context.table(tourKey), objects.tours.map { it.row() }) { context.select(key = tourKey, id = it.id) }
    }
    val selectedId = context.selection(key = tourKey, fallback = objects.tours.firstOrNull()?.id)
    val tour = objects.tours.firstOrNull { it.id == selectedId } ?: return
    val participantIds = objects.competitions.filter { tour.id in it.tourIds }.flatMap { it.participantIds }
    val participants = context.state.users.filter { it.id in participantIds }
    block("Обзор результата") { demoResultMatrix(context, actor, tour, participants) }
}
