package tech.testsys.web.devapp.demo.model

internal fun DemoState.actor(role: DemoRole): DemoUser = users.firstOrNull { user ->
    user.id == sessionUserId && user.role == role
} ?: users.first { user ->
    user.role == role
}

internal fun DemoState.availableTours(actor: DemoUser): List<DemoTour> = tours.filter { tour ->
    actor.role == DemoRole.Organizer && tour.communityIds.any { communityId ->
        communityId in actor.communityIds
    }
}

internal fun DemoState.objects(actor: DemoUser): DemoObjects {
    val visibleClasses = classes.filter { demoClass ->
        actor.role == DemoRole.Student && actor.id in demoClass.studentIds
    }

    val visibleCompetitions = competitions.filter { competition -> isVisible(competition, actor) }
    val tourIds = when (actor.role) {
        DemoRole.Student -> visibleClasses.flatMap { demoClass ->
            demoClass.tourIds
        }
        DemoRole.Observer -> observerScopes.filter { scope ->
            scope.userId == actor.id && visibleCompetitions.any { competition ->
                competition.id == scope.competitionId
            }
        }.flatMap { scope ->
            scope.tourIds
        }
        DemoRole.Organizer, DemoRole.Participant, DemoRole.Developer, DemoRole.Judge, DemoRole.Administrator, DemoRole.Supervisor ->
            visibleCompetitions.flatMap { competition -> competition.tourIds }
    }

    val visibleTours = tours.filter { tour ->
        if (actor.role == DemoRole.Developer) {
            tour.ownerId == actor.id
        } else {
            tour.id in tourIds
        }
    }

    val visibleTasks = tasks.filter { task ->
        if (actor.role == DemoRole.Developer) {
            task.ownerId == actor.id || task.communityIds.any { communityId ->
                communityId in actor.communityIds
            }
        } else {
            visibleTours.any { tour ->
                task.id in tour.taskIds
            }
        }
    }

    return DemoObjects(
        communities = communities.filter { community ->
            community.id in actor.communityIds
        },
        classes = visibleClasses,
        competitions = visibleCompetitions,
        tours = visibleTours,
        tasks = visibleTasks,
        resources = resources.filter { resource ->
            actor.role == DemoRole.Developer && resource.ownerId == actor.id
        },
        solutions = solutions.filter { solution ->
            if (actor.role == DemoRole.Judge) {
                users.any { author ->
                    author.id == solution.userId && author.role in listOf(DemoRole.Student, DemoRole.Participant)
                }
            } else {
                solution.userId == actor.id && visibleTasks.any { task ->
                    task.id == solution.taskId
                }
            }
        },
        users = users.filter { user ->
            actor.role == DemoRole.Administrator && user.communityIds.any { community ->
                community in actor.communityIds
            }
        },
    )
}

private fun DemoState.isVisible(competition: DemoCompetition, actor: DemoUser): Boolean = when (actor.role) {
    DemoRole.Organizer -> competition.organizerId == actor.id
    DemoRole.Participant -> actor.id in competition.participantIds
    DemoRole.Observer ->
        competition.communityId in actor.communityIds &&
            observerScopes.any { scope -> scope.userId == actor.id && scope.competitionId == competition.id }
    DemoRole.Student, DemoRole.Developer, DemoRole.Judge, DemoRole.Administrator, DemoRole.Supervisor -> false
}
