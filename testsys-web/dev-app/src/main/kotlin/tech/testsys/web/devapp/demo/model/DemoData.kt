package tech.testsys.web.devapp.demo.model

/** Immutable user fixture of the UI demonstration; not a domain entity. */
internal data class DemoUser(
    val id: String,
    val alias: String,
    val role: DemoRole,
    val accessCode: String,
    val communityIds: List<String>,
    val email: String? = null,
    val lastLogin: String = "—",
)

/** Immutable community fixture of the UI demonstration; not a domain entity. */
internal data class DemoCommunity(
    val id: String,
    val name: String,
)

/** Immutable history fixture of the UI demonstration; not a domain entity. */
internal data class DemoHistory(
    val modifiedAt: String,
    val fileName: String,
    val comment: String,
)

/** Immutable resource fixture of the UI demonstration; not a domain entity. */
internal data class DemoResource(
    val id: String,
    val ownerId: String,
    val taskId: String,
    val name: String,
    val category: String,
    val fileName: String,
    val modifiedAt: String,
    val history: List<DemoHistory>,
)

/** Immutable Class fixture of the UI demonstration; not a domain entity. */
internal data class DemoClass(
    val id: String,
    val name: String,
    val studentIds: List<String>,
    val tourIds: List<String>,
)

/** Immutable competition fixture of the UI demonstration; not a domain entity. */
internal data class DemoCompetition(
    val id: String,
    val name: String,
    val organizerId: String,
    val communityId: String,
    val participantIds: List<String>,
    val tourIds: List<String>,
)

/** Immutable tour fixture of the UI demonstration; not a domain entity. */
internal data class DemoTour(
    val id: String,
    val name: String,
    val description: String,
    val startsAt: String,
    val endsAt: String,
    val durationMinutes: Int,
    val remainingSeconds: Int,
    val trikVersion: String,
    val taskIds: List<String>,
    val ownerId: String,
    val communityIds: List<String>,
)

/** Immutable task fixture of the UI demonstration; not a domain entity. */
internal data class DemoTask(
    val id: String,
    val name: String,
    val description: String,
    val authorSolutionKinds: List<DemoSolutionKind>,
    val ownerId: String,
    val communityIds: List<String>,
    val state: DemoTaskState,
    val trikVersions: List<String>,
)

/** Immutable solution fixture of the UI demonstration; not a domain entity. */
internal data class DemoSolution(
    val id: String,
    val userId: String,
    val taskId: String,
    val fileName: String,
    val kind: DemoSolutionKind,
    val submittedAt: String,
    val status: DemoSolutionStatus,
    val score: Int? = null,
)

/** Immutable observerscope fixture of the UI demonstration; not a domain entity. */
internal data class DemoObserverScope(
    val userId: String,
    val competitionId: String,
    val tourIds: List<String>,
)
