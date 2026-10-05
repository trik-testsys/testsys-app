package tech.testsys.web.devapp.demo.model

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
