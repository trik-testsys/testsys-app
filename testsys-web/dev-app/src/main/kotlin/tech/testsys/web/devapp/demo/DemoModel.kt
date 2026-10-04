package tech.testsys.web.devapp.demo

private const val FIRST_DEMO_ID = 103

private const val DEFAULT_PARTICIPANTS = 3
internal const val DEMO_PARTICIPANT_LIMIT = 100

/** Pending mock email confirmation. */
internal data class DemoRegistration(val alias: String, val email: String, val role: String)

/** One-use mock access recovery, displayed inside the demonstration. */
internal data class DemoRecovery(val userId: String, val token: String, val isCompleted: Boolean = false)

/** Records visible in the selected demonstration context. */
internal data class DemoObjects(
    val communities: List<DemoCommunity>,
    val classes: List<DemoGroup>,
    val competitions: List<DemoCompetition>,
    val tours: List<DemoTour>,
    val tasks: List<DemoTask>,
    val resources: List<DemoResource>,
    val solutions: List<DemoSolution>,
    val users: List<DemoUser>,
)

/** Result of a pure demonstration transition. */
internal data class DemoResult(val state: DemoState, val isSuccess: Boolean, val message: String)

/** Immutable mock state; no persistence, production permissions or operations are attached. */
internal data class DemoState(
    val users: List<DemoUser>,
    val communities: List<DemoCommunity>,
    val classes: List<DemoGroup>,
    val competitions: List<DemoCompetition>,
    val tours: List<DemoTour>,
    val tasks: List<DemoTask>,
    val resources: List<DemoResource>,
    val solutions: List<DemoSolution>,
    val observerScopes: List<DemoObserverScope>,
    val sessionUserId: String? = null,
    val pendingRegistration: DemoRegistration? = null,
    val recovery: DemoRecovery? = null,
    val nextId: Int = FIRST_DEMO_ID,
)

internal fun DemoState.actor(role: String): DemoUser = users.firstOrNull {
    it.id == sessionUserId && it.role == role
} ?: users.first {
    it.role == role
}

internal fun DemoState.availableTours(actor: DemoUser): List<DemoTour> = tours.filter { tour ->
    actor.role == "Организатор" && tour.communityIds.any {
        it in actor.communityIds
    }
}

internal fun DemoState.objects(actor: DemoUser): DemoObjects {
    val groups = classes.filter {
        actor.role == "Ученик" && actor.id in it.studentIds
    }
    val contests = competitions.filter { contest ->
        when (actor.role) {
            "Организатор" -> contest.organizerId == actor.id
            "Участник" -> actor.id in contest.participantIds
            "Наблюдатель" ->
                contest.communityId in actor.communityIds &&
                    observerScopes.any { scope -> scope.userId == actor.id && scope.competitionId == contest.id }
            else -> false
        }
    }
    val tourIds = when (actor.role) {
        "Ученик" -> groups.flatMap {
            it.tourIds
        }
        "Наблюдатель" -> observerScopes.filter { scope ->
            scope.userId == actor.id && contests.any {
                it.id == scope.competitionId
            }
        }.flatMap {
            it.tourIds
        }
        else -> contests.flatMap {
            it.tourIds
        }
    }
    val visibleTours = tours.filter {
        if (actor.role == "Разработчик") {
            it.ownerId == actor.id
        } else {
            it.id in tourIds
        }
    }
    val visibleTasks = tasks.filter { task ->
        if (actor.role == "Разработчик") {
            task.ownerId == actor.id || task.communityIds.any {
                it in actor.communityIds
            }
        } else {
            visibleTours.any {
                task.id in it.taskIds
            }
        }
    }
    return DemoObjects(
        communities = communities.filter {
            it.id in actor.communityIds
        },
        classes = groups,
        competitions = contests,
        tours = visibleTours,
        tasks = visibleTasks,
        resources = resources.filter {
            actor.role == "Разработчик" && it.ownerId == actor.id
        },
        solutions = solutions.filter { solution ->
            if (actor.role == "Судья") {
                users.any {
                    it.id == solution.userId && it.role in listOf("Ученик", "Участник")
                }
            } else {
                solution.userId == actor.id && visibleTasks.any {
                    it.id == solution.taskId
                }
            }
        },
        users = users.filter {
            actor.role == "Администратор" && it.communityIds.any { community ->
                community in actor.communityIds
            }
        },
    )
}

internal fun DemoState.login(code: String): DemoResult {
    val user = users.firstOrNull {
        it.accessCode == code.trim()
    } ?: return fail("Код-доступа невалиден")
    return copy(sessionUserId = user.id).ok("Вход выполнен: ${user.alias}")
}

internal fun DemoState.register(alias: String, email: String, role: String): DemoResult {
    val normalized = email.trim().lowercase()
    if (alias.isBlank() || !EMAIL.matches(normalized) || role !in listOf("Ученик", "Организатор")) {
        return fail("Укажите псевдоним, корректную почту и роль")
    }
    if (
        users.any {
            it.email?.lowercase() == normalized
        }
    ) {
        return fail("Почта уже привязана к кабинету")
    }
    return copy(
        pendingRegistration = DemoRegistration(alias = alias.trim(), email = normalized, role = role),
    ).ok("Демонстрационное письмо: код 246810")
}

internal fun DemoState.confirmRegistration(code: String): DemoResult {
    val pending = pendingRegistration ?: return fail("Код подтверждения указан неверно")
    if (code.trim() != "246810") {
        return fail("Код подтверждения указан неверно")
    }
    if (
        users.any {
            it.email == pending.email
        }
    ) {
        return fail("Почта уже привязана к кабинету")
    }
    val user = DemoUser(
        id = "user$nextId",
        alias = pending.alias,
        role = pending.role,
        accessCode = "ACCESS-$nextId",
        communityIds = listOf("public"),
        email = pending.email,
    )
    return copy(
        users = users + user,
        sessionUserId = user.id,
        nextId = nextId + 1,
        pendingRegistration = null,
    )
        .ok("Регистрация завершена. Код-доступа: ${user.accessCode}")
}

internal fun DemoState.requestRecovery(email: String): DemoResult {
    val user = users.firstOrNull {
        it.email?.lowercase() == email.trim().lowercase()
    } ?: return fail("Кабинет с такой почтой не найден")
    return copy(recovery = DemoRecovery(userId = user.id, token = "restore-$nextId")).ok("Демонстрационная ссылка: restore-$nextId")
}

internal fun DemoState.restoreAccess(token: String): DemoResult {
    val current = recovery ?: return fail("Ссылка восстановления недействительна")
    if (current.isCompleted || current.token != token) {
        return fail("Ссылка восстановления недействительна")
    }
    val code = "RESTORED-$nextId"
    return copy(
        users = users.map {
            if (it.id == current.userId) {
                it.copy(accessCode = code)
            } else {
                it
            }
        },
        recovery = current.copy(isCompleted = true),
        nextId = nextId + 1,
    ).ok("Новый код-доступа: $code. Старый код невалиден")
}

internal fun DemoState.createCompetition(name: String): DemoResult {
    if (name.isBlank()) {
        return fail("Введите название соревнования")
    }
    val organizer = actor("Организатор")
    val competition = DemoCompetition(
        id = "competition$nextId",
        name = name.trim(),
        organizerId = organizer.id,
        communityId = organizer.communityIds.first(),
        participantIds = emptyList(),
        tourIds = emptyList(),
    )
    return copy(competitions = competitions + competition, nextId = nextId + 1).ok("Соревнование создано")
}

internal fun DemoState.addTour(competitionId: String, tourId: String): DemoResult {
    val organizer = actor("Организатор")
    val competition = competitions.firstOrNull {
        it.id == competitionId && it.organizerId == organizer.id
    } ?: return fail("Соревнование или тур недоступны")
    if (
        availableTours(organizer).none {
            it.id == tourId
        }
    ) {
        return fail("Соревнование или тур недоступны")
    }
    if (tourId in competition.tourIds) {
        return fail("Тур уже добавлен")
    }
    return copy(
        competitions = competitions.map {
            if (it.id == competition.id) {
                it.copy(tourIds = it.tourIds + tourId)
            } else {
                it
            }
        },
    ).ok("Тур добавлен")
}

internal fun DemoState.createParticipants(competitionId: String, count: Int = DEFAULT_PARTICIPANTS): DemoResult {
    if (count !in 1..DEMO_PARTICIPANT_LIMIT) {
        return fail("Укажите целое количество от 1 до 100 (предел демонстрации)")
    }
    val competition = competitions.firstOrNull {
        it.id == competitionId && it.organizerId == actor("Организатор").id
    } ?: return fail("Соревнование недоступно")
    val participants = (nextId until nextId + count).map { id ->
        DemoUser(
            id = "p$id",
            alias = "Участник $id",
            role = "Участник",
            accessCode = "PART-$id",
            communityIds = listOf(competition.communityId),
        )
    }
    return copy(
        users = users + participants,
        nextId = nextId + count,
        competitions = competitions.map {
            if (it.id == competition.id) {
                it.copy(
                    participantIds = it.participantIds + participants.map { user ->
                        user.id
                    },
                )
            } else {
                it
            }
        },
    ).ok("Созданы участники: $count; каждому выдан код-доступа")
}

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

private fun DemoState.fail(message: String): DemoResult = DemoResult(state = this, isSuccess = false, message = message)

private fun DemoState.ok(message: String): DemoResult = DemoResult(state = this, isSuccess = true, message = message)

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

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
