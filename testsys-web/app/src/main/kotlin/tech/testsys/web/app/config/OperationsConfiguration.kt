package tech.testsys.web.app.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.testsys.domain.contract.FileContentReader
import org.springframework.core.env.Environment
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.EmailChangeRequestRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.operation.TaskValidationDispatcher
import tech.testsys.operation.TaskValidationOperations
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.config.EmailConfirmationConfig
import tech.testsys.operation.user.AdministratorOperations
import tech.testsys.operation.user.DeveloperOperations
import tech.testsys.operation.user.JudgeOperations
import tech.testsys.operation.user.MultipleRoleUserOperations
import tech.testsys.operation.user.ParticipantOperations
import tech.testsys.operation.user.StudentOperations
import tech.testsys.operation.user.StudyOperations
import tech.testsys.operation.user.UserOperations
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.random.RandomGenerator

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
     * Cryptographically strong generator of confirmation and access codes.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun randomGenerator(): RandomGenerator = SecureRandom()

    /**
     * Public community from the required property `testsys.operation.community.public-community-id`.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun communityConfig(environment: Environment): CommunityConfig {
        val id = environment.getRequiredProperty("$CONFIG_PREFIX.community.public-community-id", Long::class.java)
        return object : CommunityConfig {
            override val publicCommunityId = CommunityId(id)
        }
    }

    /**
     * Confirmation code lifetime and attempts from the required properties under `testsys.operation.email-confirmation`.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun emailConfirmationConfig(environment: Environment): EmailConfirmationConfig {
        val lifetime = Duration.parse(environment.getRequiredProperty("$CONFIG_PREFIX.email-confirmation.confirmation-code-lifetime"))
        val attempts = environment.getRequiredProperty("$CONFIG_PREFIX.email-confirmation.max-confirmation-attempts", Int::class.java)
        return object : EmailConfirmationConfig {
            override val confirmationCodeLifetime = lifetime
            override val maxConfirmationAttempts = attempts
        }
    }

    /**
     * Community invite validity and refresh period from the required properties under `testsys.operation.community-invite`.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun communityInviteConfig(environment: Environment): CommunityInviteConfig {
        val ttl = Duration.parse(environment.getRequiredProperty("$CONFIG_PREFIX.community-invite.ttl"))
        val refreshPeriod = Duration.parse(environment.getRequiredProperty("$CONFIG_PREFIX.community-invite.refresh-period"))
        return object : CommunityInviteConfig {
            override val ttl = ttl
            override val refreshPeriod = refreshPeriod
        }
    }

    /**
     * Operations of Administrators.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun administratorOperations(
        communities: CommunityRepository,
        managerInvites: ManagerCommunityInviteRepository,
        developerInvites: DeveloperCommunityInviteRepository,
        communityInviteConfig: CommunityInviteConfig,
        clock: Clock,
        users: UserRepository,
        contests: ContestRepository,
        observers: ObserverRepository,
        multipleRoleUsers: MultipleRoleUserRepository,
    ): AdministratorOperations = AdministratorOperations(
        communityRepository = communities,
        managerInviteRepository = managerInvites,
        developerInviteRepository = developerInvites,
        communityInviteConfig = communityInviteConfig,
        clock = clock,
        userRepository = users,
        contestRepository = contests,
        observerRepository = observers,
        multipleRoleUserRepository = multipleRoleUsers,
    )

    /**
     * Operations of any user without a fixed role.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun multipleRoleUserOperations(
        multipleRoleUsers: MultipleRoleUserRepository,
        communities: CommunityRepository,
        managerInvites: ManagerCommunityInviteRepository,
        developerInvites: DeveloperCommunityInviteRepository,
        emailChangeRequests: EmailChangeRequestRepository,
        mailSender: UserMailSender,
        emailConfirmationConfig: EmailConfirmationConfig,
        clock: Clock,
        randomGenerator: RandomGenerator,
    ): MultipleRoleUserOperations = MultipleRoleUserOperations(
        multipleRoleUserRepository = multipleRoleUsers,
        communityRepository = communities,
        managerInviteRepository = managerInvites,
        developerInviteRepository = developerInvites,
        emailChangeRequestRepository = emailChangeRequests,
        mailSender = mailSender,
        emailConfirmationConfig = emailConfirmationConfig,
        clock = clock,
        randomGenerator = randomGenerator,
    )

    /**
     * Operations of any user, including login and registration.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun userOperations(
        multipleRoleUsers: MultipleRoleUserRepository,
        participants: ParticipantRepository,
        observers: ObserverRepository,
        supervisors: SupervisorRepository,
        users: UserRepository,
        registrationRequests: RegistrationRequestRepository,
        mailSender: UserMailSender,
        communityConfig: CommunityConfig,
        emailConfirmationConfig: EmailConfirmationConfig,
        clock: Clock,
        randomGenerator: RandomGenerator,
    ): UserOperations = UserOperations(
        multipleRoleUserRepository = multipleRoleUsers,
        participantRepository = participants,
        observerRepository = observers,
        supervisorRepository = supervisors,
        userRepository = users,
        registrationRequestRepository = registrationRequests,
        mailSender = mailSender,
        communityConfig = communityConfig,
        emailConfirmationConfig = emailConfirmationConfig,
        clock = clock,
        randomGenerator = randomGenerator,
    )

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
        invites: ClassInviteRepository,
    ): StudentOperations = StudentOperations(classes, contests, entries, clock, invites)

    /**
     * Operations of Participants and Students in a Contest.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun studyOperations(
        fileContentReader: FileContentReader,
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
        fileContentReader,
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

private const val CONFIG_PREFIX = "testsys.operation"
