package tech.testsys.web.devapp.demo.model

private const val FIRST_DEMO_ID = 103

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

internal fun DemoState.fail(message: String): DemoResult = DemoResult(state = this, isSuccess = false, message = message)

internal fun DemoState.ok(message: String): DemoResult = DemoResult(state = this, isSuccess = true, message = message)
