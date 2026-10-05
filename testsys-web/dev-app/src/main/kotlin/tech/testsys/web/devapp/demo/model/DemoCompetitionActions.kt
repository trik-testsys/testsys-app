package tech.testsys.web.devapp.demo.model

private const val DEFAULT_PARTICIPANTS = 3

internal const val DEMO_PARTICIPANT_LIMIT = 100

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
