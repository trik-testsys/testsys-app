package tech.testsys.web.app.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.operation.TaskValidationDispatcher
import tech.testsys.operation.TaskValidationOperations
import tech.testsys.operation.user.DeveloperOperations
import tech.testsys.operation.user.JudgeOperations
import tech.testsys.operation.user.ParticipantOperations
import tech.testsys.operation.user.StudentOperations
import tech.testsys.operation.user.StudyOperations
import java.time.Clock
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Assembles the operation classes from the ports supplied by the infrastructure modules.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
class OperationsConfiguration {
    /**
     * Clock of the operations that check time.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /**
     * Grader for operations running in a service transaction.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun afterCommitGrader(grader: BalancingGrader): AfterCommitGrader = AfterCommitGrader(grader)

    /**
     * Processing of saved task validation requests; it runs outside service transactions and grades at once.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun taskValidationOperations(
        requests: TaskValidationRequestRepository,
        tests: TestRepository,
        diagnostics: PolygonDiagnostics,
        submissions: SubmissionRepository,
        verdicts: VerdictRepository,
        grader: BalancingGrader,
    ): TaskValidationOperations = TaskValidationOperations(requests, tests, diagnostics, submissions, verdicts, grader)

    /**
     * Single-thread executor of the task validation dispatcher, shut down by [TaskValidationDispatcherLifecycle].
     *
     * @since %CURRENT_VERSION%
     */
    @Bean(destroyMethod = "", defaultCandidate = false)
    fun taskValidationExecutor(): ThreadPoolExecutor =
        // Not a default candidate, so that Spring Boot keeps its application task executor. No destroy method: the
        // inferred close() would wait for a running task without a limit, and the daemon thread does not keep the JVM
        // alive if that task outlasts the stop timeout.
        ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) { task ->
            Thread(task, "task-validation").apply { isDaemon = true }
        }

    /**
     * Dispatcher of task validation requests; a request scheduled in a transaction is processed after it completes.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun taskValidationDispatcher(
        requests: TaskValidationRequestRepository,
        operations: TaskValidationOperations,
        grader: BalancingGrader,
        @Qualifier("taskValidationExecutor") executor: ThreadPoolExecutor,
    ): TaskValidationDispatcher = TaskValidationDispatcher(requests, operations, grader, AfterTransactionExecutor(executor))

    /**
     * Starts the task validation dispatcher with the context and stops its executor before the grader closes.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun taskValidationDispatcherLifecycle(
        dispatcher: TaskValidationDispatcher,
        @Qualifier("taskValidationExecutor") executor: ThreadPoolExecutor,
    ): TaskValidationDispatcherLifecycle = TaskValidationDispatcherLifecycle(dispatcher, executor, DISPATCHER_STOP_TIMEOUT)

    /**
     * Operations of Developers.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun developerOperations(
        tasks: TaskRepository,
        statements: StatementRepository,
        communities: CommunityRepository,
        exercises: ExerciseRepository,
        tests: TestRepository,
        developerSolutions: DeveloperSolutionRepository,
        solutions: SolutionRepository,
        contests: ContestRepository,
        requests: TaskValidationRequestRepository,
        dispatcher: TaskValidationDispatcher,
        submissions: SubmissionRepository,
        grader: AfterCommitGrader,
    ): DeveloperOperations = DeveloperOperations(
        tasks,
        statements,
        communities,
        exercises,
        tests,
        developerSolutions,
        solutions,
        contests,
        requests,
        dispatcher,
        submissions,
        grader,
    )

    /**
     * Operations of Judges.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun judgeOperations(
        verdicts: VerdictRepository,
        submissions: SubmissionRepository,
        judgmentOrders: JudgmentOrderRepository,
        multipleRoleUsers: MultipleRoleUserRepository,
        participants: ParticipantRepository,
    ): JudgeOperations = JudgeOperations(verdicts, submissions, judgmentOrders, multipleRoleUsers, participants)

    /**
     * Operations of Participants.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun participantOperations(
        competitions: CompetitionRepository,
        contests: ContestRepository,
        entries: ParticipantContestEntryRepository,
        clock: Clock,
    ): ParticipantOperations = ParticipantOperations(competitions, contests, entries, clock)

    /**
     * Operations of Students.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun studentOperations(
        classes: ClassRepository,
        contests: ContestRepository,
        entries: StudentContestEntryRepository,
        clock: Clock,
    ): StudentOperations = StudentOperations(classes, contests, entries, clock)

    /**
     * Operations of Participants and Students in a Contest.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun studyOperations(
        competitions: CompetitionRepository,
        classes: ClassRepository,
        contests: ContestRepository,
        participantEntries: ParticipantContestEntryRepository,
        studentEntries: StudentContestEntryRepository,
        tasks: TaskRepository,
        submissions: SubmissionRepository,
        verdicts: VerdictRepository,
        judgmentOrders: JudgmentOrderRepository,
        statements: StatementRepository,
        exercises: ExerciseRepository,
        solutions: SolutionRepository,
        developerSolutions: DeveloperSolutionRepository,
        clock: Clock,
    ): StudyOperations = StudyOperations(
        competitions,
        classes,
        contests,
        participantEntries,
        studentEntries,
        tasks,
        submissions,
        verdicts,
        judgmentOrders,
        statements,
        exercises,
        solutions,
        developerSolutions,
        clock,
    )
}

private val DISPATCHER_STOP_TIMEOUT: Duration = Duration.ofSeconds(30)
