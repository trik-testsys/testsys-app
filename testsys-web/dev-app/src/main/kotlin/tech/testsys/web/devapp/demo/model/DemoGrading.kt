package tech.testsys.web.devapp.demo.model

internal fun DemoState.submitSolution(userId: String, taskId: String, kind: String, fileName: String): DemoResult {
    val user = users.firstOrNull {
        it.id == userId
    } ?: return fail("Пользователь недоступен")
    val task = objects(user).tasks.firstOrNull {
        it.id == taskId
    } ?: return fail("Выберите доступную задачу, вид решения и файл")
    if (kind !in task.authorSolutionKinds || fileName.isBlank()) {
        return fail("Выберите доступную задачу, вид решения и файл")
    }
    val solution = DemoSolution(
        id = "s$nextId",
        userId = userId,
        taskId = taskId,
        fileName = fileName,
        kind = kind,
        submittedAt = "01.10.2026 11:00",
        status = "Queue",
    )
    return copy(solutions = solutions + solution, nextId = nextId + 1).ok("Решение поставлено в очередь демонстрационной проверки")
}

internal fun DemoState.checkSolution(id: String): DemoState = copy(
    solutions = solutions.map {
        if (it.id == id && it.status == "Queue") {
            it.copy(status = "Checking")
        } else {
            it
        }
    },
)

internal fun DemoState.finishSolution(id: String, status: String, score: Int): DemoState = copy(
    solutions = solutions.map {
        if (it.id == id && it.status in listOf("Queue", "Checking")) {
            it.copy(
                status = status,
                score = score.takeIf {
                    status == "Checked"
                },
            )
        } else {
            it
        }
    },
)

internal fun DemoState.resultsCsv(competitionId: String, tourId: String): String {
    val competition = competitions.firstOrNull {
        it.id == competitionId
    } ?: return ""
    val tour = tours.firstOrNull {
        it.id == tourId && it.id in competition.tourIds
    } ?: return ""
    val selectedTasks = tour.taskIds.mapNotNull { taskId -> tasks.firstOrNull { task -> task.id == taskId } }
    val header = listOf("ID", "Псевдоним") + selectedTasks.flatMap {
        listOf("${it.id} ${it.name}: лучший балл", "${it.id} ${it.name}: решений")
    }
    val rows = competition.participantIds.map { id ->
        listOf(
            id,
            users.first {
                it.id == id
            }.alias,
        ) + selectedTasks.flatMap { task ->
            listOf(
                bestScore(solutions = solutions, userId = id, taskId = task.id)?.toString().orEmpty(),
                attemptCount(solutions = solutions, userId = id, taskId = task.id).toString(),
            )
        }
    }
    return "\uFEFF" + (listOf(header) + rows).joinToString("\r\n") { row ->
        row.joinToString(";") {
            "\"${it.replace(oldValue = "\"", newValue = "\"\"")}\""
        }
    }
}

/** Number of attempts for the participant and task, including pending checks. */
internal fun attemptCount(solutions: List<DemoSolution>, userId: String, taskId: String): Int =
    solutions.count { solution -> solution.userId == userId && solution.taskId == taskId }

/** Best finite checked score, preserving zero as a result. */
internal fun bestScore(solutions: List<DemoSolution>, userId: String, taskId: String): Int? = solutions
    .filter {
        it.userId == userId && it.taskId == taskId && it.status == "Checked"
    }.mapNotNull {
        it.score
    }.maxOrNull()
