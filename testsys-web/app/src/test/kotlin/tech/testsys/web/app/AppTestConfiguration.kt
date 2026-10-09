package tech.testsys.web.app

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.task.SubmissionDataBuilder
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.User
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.security.UserKind
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/** Test beans of the application: stored fixtures, the public community made of them and recorded letters. */
@TestConfiguration
@Import(StudyFixtures::class)
class AppTestConfiguration {
    @Bean
    fun appFixtures(
        multipleRoleUsers: MultipleRoleUserRepository,
        participants: ParticipantRepository,
        observers: ObserverRepository,
        supervisors: SupervisorRepository,
        communities: CommunityRepository,
        competitions: CompetitionRepository,
        classes: ClassRepository,
        contests: ContestRepository,
        tasks: TaskRepository,
        solutions: SolutionRepository,
        submissions: SubmissionRepository,
        tests: TestRepository,
        logs: LogsRepository,
        recordings: RecordingRepository,
        verdicts: VerdictRepository,
        dataSource: DataSource,
    ): AppFixtures = AppFixtures(
        multipleRoleUsers,
        participants,
        observers,
        supervisors,
        communities,
        competitions,
        classes,
        contests,
        tasks,
        solutions,
        submissions,
        tests,
        logs,
        recordings,
        verdicts,
        dataSource,
    )

    @Bean
    @Primary
    fun publicCommunityConfig(fixtures: AppFixtures): CommunityConfig = object : CommunityConfig {
        override val publicCommunityId: CommunityId by lazy { fixtures.community().id }
    }

    @Bean
    @Primary
    fun recordedMail(): RecordedMail = RecordedMail()
}

/** Stores users, groups and submissions with unique access codes, names and e-mail addresses. */
class AppFixtures(
    private val multipleRoleUsers: MultipleRoleUserRepository,
    private val participants: ParticipantRepository,
    private val observers: ObserverRepository,
    private val supervisors: SupervisorRepository,
    private val communities: CommunityRepository,
    private val competitions: CompetitionRepository,
    private val classes: ClassRepository,
    private val contests: ContestRepository,
    private val tasks: TaskRepository,
    private val solutions: SolutionRepository,
    private val submissions: SubmissionRepository,
    private val tests: TestRepository,
    private val logs: LogsRepository,
    private val recordings: RecordingRepository,
    private val verdicts: VerdictRepository,
    private val dataSource: DataSource,
) {
    fun unique(prefix: String): String = "$prefix-${UUID.randomUUID()}"

    /** Returns a confirmation code of the same length that differs from [code] in every digit. */
    fun otherCode(code: String): String = code.map { digit -> '0' + (digit - '0' + 1) % DIGITS }.joinToString("")

    fun multipleRoleUser(
        name: String = unique("User"),
        rawAccessToken: String = unique("token"),
        email: String = "${unique("user")}@example.com",
        roles: MultipleRoleUserDataBuilder.() -> Unit = {},
    ): MultipleRoleUser = multipleRoleUsers.save(
        multipleRoleUserData {
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
            this.name = name
            this.email = email
            roles()
        },
    )

    fun userOf(kind: UserKind, name: String = unique("User"), rawAccessToken: String = unique("token")): User<*> = when (kind) {
        UserKind.MULTIPLE_ROLE -> multipleRoleUser(name = name, rawAccessToken = rawAccessToken)
        UserKind.PARTICIPANT -> {
            val competitionId = competitions.save(
                competitionData {
                    owner(multipleRoleUser().id.value)
                    this.name = unique("Competition")
                    description = "Competition"
                },
            ).id.value
            participants.save(
                participantData {
                    competition(competitionId)
                    accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
                    this.name = name
                },
            )
        }
        UserKind.OBSERVER -> observer(of = community(), name = name, rawAccessToken = rawAccessToken)
        UserKind.SUPERVISOR -> supervisors.save(
            supervisorData {
                accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
                this.name = name
            },
        )
    }

    /** Returns a new user holding only the administrator role. */
    fun administrator(): MultipleRoleUser = multipleRoleUser { roles { administrator {} } }

    fun observer(of: Community, name: String = unique("Observer"), rawAccessToken: String = unique("token")): Observer = observers.save(
        observerData {
            community(of.id.value)
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
            this.name = name
        },
    )

    fun community(owner: MultipleRoleUser = multipleRoleUser(), name: String = unique("Community")): Community {
        val ownerId = owner.id.value
        return communities.saveWithInvites(managerInvite = invite(), developerInvite = invite()) { managerInviteId, developerInviteId ->
            communityData {
                owner(ownerId)
                this.name = name
                description = "Community"
                managerInvite = managerInviteId
                developerInvite = developerInviteId
            }
        }
    }

    /** Makes [user] a member of [community] in [role], granting the role if the user did not hold it. */
    fun grantRole(user: MultipleRoleUser, role: CommunityRole, community: Community = community()): MultipleRoleUser =
        multipleRoleUsers.addCommunityMembership(userId = user.id, communityId = community.id, role = role)

    fun studyClass(
        owner: MultipleRoleUser,
        name: String = unique("Class"),
        students: List<MultipleRoleUser> = emptyList(),
        contests: List<Contest> = emptyList(),
    ): Class {
        val inviteData = classInviteData {
            code(unique("code"), HashAlgorithm.Identity)
            expiresAt = Instant.parse("2100-01-01T00:00:00Z")
        }
        return classes.saveWithInvite(inviteData) { inviteId ->
            classData {
                owner(owner.id.value)
                this.name = name
                description = ""
                invite = inviteId
                students(students.map { student -> student.id.value })
                contests(contests.map { contest -> contest.id.value })
            }
        }
    }

    fun competition(owner: MultipleRoleUser, name: String = unique("Competition"), contests: List<Contest> = emptyList()): Competition =
        competitions.save(
            competitionData {
                owner(owner.id.value)
                this.name = name
                description = ""
                contests(contests.map { contest -> contest.id.value })
            },
        )

    /** Saves a contest of a new developer shared to [sharedTo], registering its TRIK Studio version first. */
    fun contest(name: String = unique("Contest"), sharedTo: List<Community> = emptyList(), tasks: List<TaskId> = emptyList()): Contest {
        val tag = trikStudioVersion()
        val ownerId = multipleRoleUser().id.value
        return contests.save(
            contestData {
                owner(ownerId)
                this.name = name
                description = ""
                trikStudioVersion(tag)
                sharedTo(sharedTo.map { community -> community.id.value })
                this.tasks = tasks.toMutableList()
            },
        )
    }

    /** Returns a new user holding only the judge role. */
    fun judge(): MultipleRoleUser = multipleRoleUser { roles { judge { data = judgeData {} } } }

    /**
     * Stores a contest submission of [author] in Python; unless [isGraded] is `false`, it is successfully graded on one
     * polygon with [score], logs and a recording.
     */
    fun gradingSubmission(author: User<*>, score: Int = DEFAULT_SCORE, isGraded: Boolean = true): Submission {
        val task = task()
        val contest = contests.save(
            contestData {
                owner(task.data.owner.id.value)
                name = unique("Contest")
                description = "Contest"
                trikStudioVersion(trikStudioVersion())
                tasks(listOf(task.id.value))
            },
        )
        val submission = submission(author, task) { kind.grading { this.contest = contest.id } }
        return if (isGraded) graded(submission, score) else submission
    }

    /** Stores a successfully graded test run of a developer solution made by [author]. */
    fun developerSolutionTestSubmission(author: MultipleRoleUser): Submission {
        val version = trikStudioVersion()
        return graded(submission(author, task()) { kind.developerSolutionTest { trikStudioVersion(version) } }, DEFAULT_SCORE)
    }

    private fun task(): Task = tasks.save(
        taskData {
            owner = multipleRoleUser { roles { developer { data = developerData {} } } }.id
            name = unique("Task")
            description = "Task"
            content.new {}
        },
    )

    private fun submission(author: User<*>, task: Task, chooseKind: SubmissionDataBuilder.() -> Unit): Submission {
        val solution = solutions.save(
            solutionData {
                file("solution.py", "print(1)".toByteArray())
                language.python()
            },
        )
        return submissions.save(
            submissionData {
                this.author = author.id
                task(task.id.value)
                solution(solution.id.value)
                status.queued()
                chooseKind()
            },
        )
    }

    private fun graded(submission: Submission, score: Int): Submission {
        val polygon = tests.save(
            testData {
                name = unique("Polygon")
                description = "Polygon"
                file("polygon.xml", "<world/>".toByteArray())
                versionBucket = VersionBucket(UUID.randomUUID())
            },
        )
        val logsId = logs.save(logsData { file("logs.txt", "logs".toByteArray()) }).id.value
        val recordingId = recordings.save(recordingData { file("recording.mp4", "video".toByteArray()) }).id.value
        val verdict = verdicts.save(
            verdictData {
                task(submission.data.task.id.value)
                submission(submission.id.value)
                testVerdict {
                    this.score = score
                    test(polygon.id.value)
                    logs(logsId)
                    recording(recordingId)
                }
            },
        )
        return submissions.update(submission.withData { status.graded { status.success { this.verdict = verdict.id } } })
    }

    /** Adds a TRIK Studio version and returns its tag. */
    private fun trikStudioVersion(): String {
        // Versions come from the grader; no port stores them, so the fixture adds the row a contest refers to.
        val tag = unique("tsv")
        dataSource.connection.use { connection ->
            val insert = "insert into ts_trik_studio_version (id, tag) select coalesce(max(id), 0) + 1, ? from ts_trik_studio_version"
            connection.prepareStatement(insert).use { statement ->
                statement.setString(1, tag)
                statement.executeUpdate()
            }
        }
        return tag
    }

    private fun invite(): CommunityInviteData = communityInviteData {
        code(unique("code"), HashAlgorithm.Identity)
        expiresAt = Instant.parse("2100-01-01T00:00:00Z")
    }

    private companion object {
        const val DIGITS = 10
        const val DEFAULT_SCORE = 75
    }
}

/** Mail port that keeps the codes it was asked to send instead of sending letters. */
class RecordedMail : UserMailSender {
    val confirmationCodes = mutableMapOf<String, String>()
    val accessTokens = mutableMapOf<String, String>()

    override fun sendRegistrationConfirmationCode(email: String, confirmationCode: String) {
        confirmationCodes[email] = confirmationCode
    }

    override fun sendAccessToken(email: String, name: String, accessToken: String) {
        accessTokens[email] = accessToken
    }

    override fun sendEmailChangeConfirmationCode(email: String, confirmationCode: String) {
        confirmationCodes[email] = confirmationCode
    }

    override fun sendEmailChangedNotice(email: String, name: String) = Unit
}
