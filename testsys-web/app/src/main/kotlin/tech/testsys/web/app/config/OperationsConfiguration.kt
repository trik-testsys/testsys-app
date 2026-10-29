package tech.testsys.web.app.config

import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.PasswordHasher
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.SupervisorCommunityReadPort
import tech.testsys.domain.contract.persistence.SupervisorUserReadPort
import tech.testsys.domain.contract.persistence.repository.AccessRestorationRequestRepository
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
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.contract.persistence.repository.RegradeRepository
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
import tech.testsys.domain.model.user.AccessTokenHasher
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.Settings
import tech.testsys.infra.settings.api.settingsOf
import tech.testsys.operation.TaskValidationDispatcher
import tech.testsys.operation.TaskValidationOperations
import tech.testsys.operation.config.ClassInviteConfig
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.config.CompetitionConfig
import tech.testsys.operation.config.EmailConfirmationConfig
import tech.testsys.operation.user.AdministratorOperations
import tech.testsys.operation.user.DeveloperOperations
import tech.testsys.operation.user.JudgeOperations
import tech.testsys.operation.user.ManagerOperations
import tech.testsys.operation.user.MultipleRoleUserOperations
import tech.testsys.operation.user.ObserverOperations
import tech.testsys.operation.user.ParticipantOperations
import tech.testsys.operation.user.StudentOperations
import tech.testsys.operation.user.StudyOperations
import tech.testsys.operation.user.SupervisorOperations
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
 * @since 1.0.0-beta.1
 */
@Configuration
class OperationsConfiguration {
    /**
     * Supervisor operations over all users and communities.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun supervisorOperations(
        users: SupervisorUserReadPort,
        communities: SupervisorCommunityReadPort,
        communityRepository: CommunityRepository,
        multipleRoleUsers: MultipleRoleUserRepository,
        userRepository: UserRepository,
        communityConfig: CommunityConfig,
        administratorOperations: AdministratorOperations,
        contests: ContestRepository,
        observers: ObserverRepository,
        accessTokenHasher: AccessTokenHasher,
    ): SupervisorOperations = SupervisorOperations(
        users, communities, communityRepository, multipleRoleUsers, userRepository, communityConfig, administratorOperations,
        contests, observers, accessTokenHasher,
    )

    /**
     * Clock of the operations that check time.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /**
     * Cryptographically strong generator of confirmation and access codes.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun randomGenerator(): RandomGenerator = SecureRandom()

    /**
     * Creates or verifies the public community with the required configured id before supplying it to operations.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun communityConfig(
        environment: Environment,
        communities: CommunityRepository,
        multipleRoleUsers: MultipleRoleUserRepository,
        managerInvites: ManagerCommunityInviteRepository,
        developerInvites: DeveloperCommunityInviteRepository,
        transactions: TransactionOperations,
        inviteConfig: CommunityInviteConfig,
        clock: Clock,
        randomGenerator: RandomGenerator,
        passwordHasher: PasswordHasher,
    ): CommunityConfig {
        val id = PublicCommunityBootstrap(
            communities,
            multipleRoleUsers,
            managerInvites,
            developerInvites,
            transactions,
            inviteConfig,
            clock,
            randomGenerator,
            passwordHasher,
        ).ensure(environment)
        return object : CommunityConfig {
            override val publicCommunityId = id
        }
    }

    /**
     * Creates the single Supervisor or sets its name and access code from the required properties under
     * `testsys.operation.supervisor` after all singletons of the shared infrastructure are created, before the web
     * contexts start.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun supervisorBootstrap(
        environment: Environment,
        supervisors: SupervisorRepository,
        participants: ParticipantRepository,
        observers: ObserverRepository,
        transactions: TransactionOperations,
        accessTokenHasher: AccessTokenHasher,
        signIns: CabinetSignInStore,
    ): SmartInitializingSingleton = SmartInitializingSingleton {
        SupervisorBootstrap(supervisors, participants, observers, transactions, accessTokenHasher, signIns).ensure(environment)
    }

    /**
     * Email confirmation settings from the current snapshot of [runtime], retained for the transaction that reads them
     * first so that the lifetime and attempts form one pair.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun emailConfirmationSettings(environment: Environment, runtime: RuntimeSettings): Settings<EmailConfirmationProperties> {
        val type = EmailConfirmationProperties::class.java
        return TransactionSettings(type, settingsOf(type, environment, runtime))
    }

    /**
     * Confirmation code lifetime and attempts from [settings].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun emailConfirmationConfig(settings: Settings<EmailConfirmationProperties>): EmailConfirmationConfig {
        return object : EmailConfirmationConfig {
            override val confirmationCodeLifetime get() = settings.current().confirmationCodeLifetime
            override val maxConfirmationAttempts get() = settings.current().maxConfirmationAttempts
        }
    }

    /**
     * Community invite settings from the current snapshot of [runtime].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun communityInviteSettings(environment: Environment, runtime: RuntimeSettings): Settings<CommunityInviteProperties> =
        settingsOf(CommunityInviteProperties::class.java, environment, runtime)

    /**
     * Community invite validity from [settings] and a static refresh period from [environment].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun communityInviteConfig(environment: Environment, settings: Settings<CommunityInviteProperties>): CommunityInviteConfig {
        val refreshPeriod = environment.requiredDuration("$CONFIG_PREFIX.community-invite.refresh-period")
        return object : CommunityInviteConfig {
            override val ttl get() = settings.current().ttl
            override val refreshPeriod = refreshPeriod
        }
    }

    /**
     * Operations of Administrators.
     *
     * @since 1.0.0-beta.1
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
        communityConfig: CommunityConfig,
        tasks: TaskRepository,
        requests: TaskValidationRequestRepository,
        classes: ClassRepository,
        competitions: CompetitionRepository,
        submissions: SubmissionRepository,
        judgmentOrders: JudgmentOrderRepository,
        verdicts: VerdictRepository,
        accessTokenHasher: AccessTokenHasher,
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
        communityConfig = communityConfig,
        taskRepository = tasks,
        taskValidationRequestRepository = requests,
        classRepository = classes,
        competitionRepository = competitions,
        submissionRepository = submissions,
        judgmentOrderRepository = judgmentOrders,
        verdictRepository = verdicts,
        accessTokenHasher = accessTokenHasher,
    )

    /**
     * Operations of any user without a fixed role.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun multipleRoleUserOperations(
        multipleRoleUsers: MultipleRoleUserRepository,
        communities: CommunityRepository,
        managerInvites: ManagerCommunityInviteRepository,
        developerInvites: DeveloperCommunityInviteRepository,
        emailChangeRequests: EmailChangeRequestRepository,
        accessRestorationRequests: AccessRestorationRequestRepository,
        mailSender: UserMailSender,
        passwordHasher: PasswordHasher,
        emailConfirmationConfig: EmailConfirmationConfig,
        clock: Clock,
        randomGenerator: RandomGenerator,
        communityConfig: CommunityConfig,
    ): MultipleRoleUserOperations = MultipleRoleUserOperations(
        multipleRoleUserRepository = multipleRoleUsers,
        communityRepository = communities,
        managerInviteRepository = managerInvites,
        developerInviteRepository = developerInvites,
        emailChangeRequestRepository = emailChangeRequests,
        accessRestorationRequestRepository = accessRestorationRequests,
        mailSender = mailSender,
        passwordHasher = passwordHasher,
        emailConfirmationConfig = emailConfirmationConfig,
        clock = clock,
        randomGenerator = randomGenerator,
        communityConfig = communityConfig,
    )

    /**
     * Operations of any user, including login and registration.
     *
     * @since 1.0.0-beta.1
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
        passwordHasher: PasswordHasher,
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
        passwordHasher = passwordHasher,
        communityConfig = communityConfig,
        emailConfirmationConfig = emailConfirmationConfig,
        clock = clock,
        randomGenerator = randomGenerator,
    )

    /**
     * Grader for operations running in a service transaction.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun afterCommitGrader(grader: BalancingGrader): AfterCommitGrader = AfterCommitGrader(grader)

    /**
     * Processing of saved task validation requests; it runs outside service transactions and grades at once.
     *
     * @since 1.0.0-beta.1
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
     * @since 1.0.0-beta.1
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
     * Its technical failures are counted in [failures].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun taskValidationDispatcher(
        requests: TaskValidationRequestRepository,
        operations: TaskValidationOperations,
        grader: BalancingGrader,
        @Qualifier("taskValidationExecutor") executor: ThreadPoolExecutor,
        failures: TechnicalFailures,
    ): TaskValidationDispatcher = TaskValidationDispatcher(
        requests = requests,
        operations = operations,
        grader = grader,
        executor = AfterTransactionExecutor(executor),
        onTechnicalFailure = { exception -> failures.record(source = "validation", stage = "processing", failure = exception) },
    )

    /**
     * Starts the task validation dispatcher with the context and stops its executor before the grader closes.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun taskValidationDispatcherLifecycle(
        dispatcher: TaskValidationDispatcher,
        @Qualifier("taskValidationExecutor") executor: ThreadPoolExecutor,
    ): TaskValidationDispatcherLifecycle = TaskValidationDispatcherLifecycle(dispatcher, executor, DISPATCHER_STOP_TIMEOUT)

    /**
     * Operations of Developers.
     *
     * @since 1.0.0-beta.1
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
        regrades: RegradeRepository,
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
        regrades,
        grader,
    )

    /**
     * Operations of Judges.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun judgeOperations(
        verdicts: VerdictRepository,
        submissions: SubmissionRepository,
        judgmentOrders: JudgmentOrderRepository,
        multipleRoleUsers: MultipleRoleUserRepository,
        participants: ParticipantRepository,
        tasks: TaskRepository,
        contests: ContestRepository,
        solutions: SolutionRepository,
        tests: TestRepository,
        logs: LogsRepository,
        recordings: RecordingRepository,
    ): JudgeOperations = JudgeOperations(
        verdicts,
        submissions,
        judgmentOrders,
        multipleRoleUsers,
        participants,
        tasks,
        contests,
        solutions,
        tests,
        logs,
        recordings,
    )

    /**
     * Operations of Participants.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun participantOperations(
        competitions: CompetitionRepository,
        contests: ContestRepository,
        entries: ParticipantContestEntryRepository,
        clock: Clock,
    ): ParticipantOperations = ParticipantOperations(competitions, contests, entries, clock)

    /**
     * Operations of Observers.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun observerOperations(
        contests: ContestRepository,
        competitions: CompetitionRepository,
        participants: ParticipantRepository,
        tasks: TaskRepository,
        submissions: SubmissionRepository,
    ): ObserverOperations = ObserverOperations(contests, competitions, participants, tasks, submissions)

    /**
     * Operations of Students.
     *
     * @since 1.0.0-beta.1
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
     * @since 1.0.0-beta.1
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

    /**
     * Competition settings from the current snapshot of [runtime].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun competitionSettings(environment: Environment, runtime: RuntimeSettings): Settings<CompetitionProperties> =
        settingsOf(CompetitionProperties::class.java, environment, runtime)

    /**
     * Participant limit of competitions from [settings].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun competitionConfig(settings: Settings<CompetitionProperties>): CompetitionConfig {
        return object : CompetitionConfig {
            override val maxParticipants get() = settings.current().maxParticipants
        }
    }

    /**
     * Class invite settings from the current snapshot of [runtime].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun classInviteSettings(environment: Environment, runtime: RuntimeSettings): Settings<ClassInviteProperties> =
        settingsOf(ClassInviteProperties::class.java, environment, runtime)

    /**
     * Class invite validity from [settings] and a static refresh period from [environment].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun classInviteConfig(environment: Environment, settings: Settings<ClassInviteProperties>): ClassInviteConfig {
        val refreshPeriod = environment.requiredDuration("$CONFIG_PREFIX.class-invite.refresh-period")
        return object : ClassInviteConfig {
            override val ttl get() = settings.current().ttl
            override val refreshPeriod = refreshPeriod
        }
    }

    /**
     * Operations of Managers.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun managerOperations(
        classes: ClassRepository,
        competitions: CompetitionRepository,
        contests: ContestRepository,
        submissions: SubmissionRepository,
        participants: ParticipantRepository,
        competitionConfig: CompetitionConfig,
        invites: ClassInviteRepository,
        classInviteConfig: ClassInviteConfig,
        clock: Clock,
        multipleRoleUsers: MultipleRoleUserRepository,
        users: UserRepository,
        tasks: TaskRepository,
        accessTokenHasher: AccessTokenHasher,
    ): ManagerOperations = ManagerOperations(
        classRepository = classes,
        competitionRepository = competitions,
        contestRepository = contests,
        submissionRepository = submissions,
        participantRepository = participants,
        competitionConfig = competitionConfig,
        classInviteRepository = invites,
        classInviteConfig = classInviteConfig,
        clock = clock,
        multipleRoleUserRepository = multipleRoleUsers,
        userRepository = users,
        taskRepository = tasks,
        accessTokenHasher = accessTokenHasher,
    )
}

private val DISPATCHER_STOP_TIMEOUT: Duration = Duration.ofSeconds(30)

private const val CONFIG_PREFIX = "testsys.operation"

/** Binds the required duration [key]; a value that is not a duration fails with a `BindException`. */
private fun Environment.requiredDuration(key: String): Duration =
    checkNotNull(Binder.get(this).bind(key, Duration::class.java).orElse(null)) { "$key is required" }
