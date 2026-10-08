package tech.testsys.infra.database

import org.springframework.boot.test.context.TestComponent
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.developerSolutionData
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.participantContestEntryData
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.registrationRequestData
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.statementData
import tech.testsys.domain.builder.api.studentContestEntryData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
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
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID
import tech.testsys.domain.model.task.Test as Polygon

/**
 * Creates prerequisite rows for adapter tests through the persistence adapters themselves; every value that must be
 * unique (access tokens, e-mails, version tags, file names) gets a random suffix from [unique].
 */
@TestComponent
@OptIn(InternalDatabaseApi::class)
@Suppress("LongParameterList", "TooManyFunctions")
class DatabaseFixtures(
    private val participantContestEntryRepository: ParticipantContestEntryRepository,
    private val studentContestEntryRepository: StudentContestEntryRepository,
    private val multipleRoleUsers: MultipleRoleUserRepository,
    private val participants: ParticipantRepository,
    private val observers: ObserverRepository,
    private val supervisors: SupervisorRepository,
    private val communities: CommunityRepository,
    private val competitions: CompetitionRepository,
    private val classes: ClassRepository,
    private val classInvites: ClassInviteRepository,
    private val managerCommunityInvites: ManagerCommunityInviteRepository,
    private val developerCommunityInvites: DeveloperCommunityInviteRepository,
    private val solutions: SolutionRepository,
    private val polygons: TestRepository,
    private val exercises: ExerciseRepository,
    private val statements: StatementRepository,
    private val developerSolutions: DeveloperSolutionRepository,
    private val tasks: TaskRepository,
    private val taskValidationRequests: TaskValidationRequestRepository,
    private val contests: ContestRepository,
    private val submissions: SubmissionRepository,
    private val verdicts: VerdictRepository,
    private val judgmentOrders: JudgmentOrderRepository,
    private val logs: LogsRepository,
    private val recordings: RecordingRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
    private val registrationRequests: RegistrationRequestRepository,
) {

    fun participantContestEntry(
        participant: Participant = participant(),
        contest: Contest = contest(),
        enteredAt: Instant = Instant.EPOCH,
    ): ParticipantContestEntry = participantContestEntryRepository.save(
        participantContestEntryData {
            this.participant = participant.id
            competition = participant.data.competition.id
            this.contest = contest.id
            this.enteredAt = enteredAt
        },
    )

    fun studentContestEntry(
        user: MultipleRoleUser = student(),
        studyClass: Class = studentClass(),
        contest: Contest = contest(),
        enteredAt: Instant = Instant.EPOCH,
    ): StudentContestEntry = studentContestEntryRepository.save(
        studentContestEntryData {
            this.user = user.id
            this.studyClass = studyClass.id
            this.contest = contest.id
            this.enteredAt = enteredAt
        },
    )

    fun unique(prefix: String) = "$prefix-${UUID.randomUUID()}"

    fun email(prefix: String) = "${unique(prefix)}@example.com"

    fun multipleRoleUser(roles: MultipleRoleUserDataBuilder.() -> Unit): MultipleRoleUser = multipleRoleUsers.save(
        multipleRoleUserData {
            accessToken(unique("token"), algorithm = HashAlgorithm.Identity)
            name = unique("User")
            email = email("user")
            roles()
        },
    )

    fun developer(): MultipleRoleUser = multipleRoleUser { roles { developer { data = developerData {} } } }

    fun student(): MultipleRoleUser = multipleRoleUser { roles { student { data = studentData {} } } }

    fun judge(): MultipleRoleUser = multipleRoleUser { roles { judge { data = judgeData {} } } }

    fun manager(): MultipleRoleUser = multipleRoleUser { roles { manager { data = managerData {} } } }

    fun administrator(): MultipleRoleUser = multipleRoleUser { roles { administrator {} } }

    fun participant(competition: Competition = competition(), rawAccessToken: String = unique("token")): Participant {
        val competitionId = competition.id.value
        return participants.save(
            participantData {
                competition(competitionId)
                accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
                name = unique("Participant")
            },
        )
    }

    fun observer(community: Community = community(), rawAccessToken: String = unique("token")): Observer {
        val communityId = community.id.value
        return observers.save(
            observerData {
                community(communityId)
                accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
                name = unique("Observer")
            },
        )
    }

    fun registrationRequest(email: String = email("registration")): RegistrationRequest = registrationRequests.save(
        registrationRequestData {
            this.email = email
            confirmationCode = "123456"
            expiresAt = Instant.EPOCH
            attemptsLeft = 5
        },
    )

    fun supervisor(rawAccessToken: String = unique("token")): Supervisor = supervisors.save(
        supervisorData {
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
            name = unique("Supervisor")
        },
    )

    fun community(owner: MultipleRoleUser = developer()): Community {
        val ownerId = owner.id.value
        return communities.saveWithInvites(
            managerInvite = communityInviteDataOf(),
            developerInvite = communityInviteDataOf(),
        ) { managerInviteId, developerInviteId ->
            communityData {
                owner(ownerId)
                name = unique("Community")
                description = "Community description"
                managerInvite = managerInviteId
                developerInvite = developerInviteId
            }
        }
    }

    fun competition(owner: MultipleRoleUser = manager()): Competition {
        val ownerId = owner.id.value
        return competitions.save(
            competitionData {
                owner(ownerId)
                name = unique("Competition")
                description = "Competition description"
            },
        )
    }

    fun studentClass(owner: MultipleRoleUser = manager(), students: List<MultipleRoleUser> = emptyList()): Class {
        val ownerId = owner.id.value
        val studentIds = students.map { it.id.value }
        return classes.saveWithInvite(classInviteDataOf()) { inviteId ->
            classData {
                owner(ownerId)
                name = unique("Class")
                description = "Class description"
                students(studentIds)
                invite = inviteId
            }
        }
    }

    fun classInvite(code: String = unique("code"), expiresAt: Instant = FAR_FUTURE): ClassInvite =
        classInvites.save(classInviteDataOf(code = code, expiresAt = expiresAt))

    fun managerCommunityInvite(code: String = unique("code"), expiresAt: Instant = FAR_FUTURE): CommunityInvite.Manager =
        managerCommunityInvites.save(communityInviteDataOf(code = code, expiresAt = expiresAt))

    fun developerCommunityInvite(code: String = unique("code"), expiresAt: Instant = FAR_FUTURE): CommunityInvite.Developer =
        developerCommunityInvites.save(communityInviteDataOf(code = code, expiresAt = expiresAt))

    fun communityInviteDataOf(code: String = unique("code"), expiresAt: Instant = FAR_FUTURE): CommunityInviteData = communityInviteData {
        this.code(code, HashAlgorithm.Identity)
        this.expiresAt = expiresAt
    }

    fun classInviteDataOf(code: String = unique("code"), expiresAt: Instant = FAR_FUTURE): ClassInviteData = classInviteData {
        this.code(code, HashAlgorithm.Identity)
        this.expiresAt = expiresAt
    }

    fun trikStudioVersion(tag: String = unique("tsv")): TrikStudioVersion {
        trikStudioVersionJpaEntityRepository.save(TrikStudioVersionJpaEntity(tag))
        return TrikStudioVersion(tag)
    }

    fun solution(language: TrikSupportedLanguage = TrikSupportedLanguage.Python): Solution = solutions.save(
        solutionData {
            file(unique("solution") + ".py", "print('solution')".toByteArray())
            this.language.chose(language)
        },
    )

    fun polygon(): Polygon = polygons.save(
        testData {
            name = unique("Polygon")
            description = "Polygon description"
            file(unique("polygon") + ".xml", "<field/>".toByteArray())
            versionBucket = VersionBucket(UUID.randomUUID())
        },
    )

    fun exercise(language: TrikSupportedLanguage = TrikSupportedLanguage.Python): Exercise = exercises.save(
        exerciseData {
            name = unique("Exercise")
            description = "Exercise description"
            file(unique("exercise") + ".qrs", "exercise".toByteArray())
            this.language.chose(language)
            versionBucket = VersionBucket(UUID.randomUUID())
        },
    )

    fun statement(): Statement = statements.save(
        statementData {
            name = unique("Statement")
            description = "Statement description"
            file(unique("statement") + ".pdf", "statement".toByteArray())
            versionBucket = VersionBucket(UUID.randomUUID())
        },
    )

    fun developerSolution(solution: Solution = solution(), expectedScore: Int = 100): DeveloperSolution {
        val solutionId = solution.id.value
        return developerSolutions.save(
            developerSolutionData {
                name = unique("Developer solution")
                description = "Developer solution description"
                solution(solutionId)
                expectedScore(expectedScore)
                versionBucket = VersionBucket(UUID.randomUUID())
            },
        )
    }

    fun task(owner: MultipleRoleUser = developer()): Task {
        val ownerId = owner.id.value
        val uploadedExercise = exercise()
        val uploadedStatement = statement()
        val exerciseId = uploadedExercise.id.value
        val statementId = uploadedStatement.id.value
        return tasks.save(
            taskData {
                owner(ownerId)
                uploadedResources = mutableSetOf(uploadedExercise.data.versionBucket, uploadedStatement.data.versionBucket)
                name = unique("Task")
                description = "Task description"
                content.committed {
                    exercises(listOf(exerciseId))
                    statement(statementId)
                }
            },
        )
    }

    fun workingTask(owner: MultipleRoleUser = developer()): Task = tasks.save(
        taskData {
            this.owner = owner.id
            name = unique("Task")
            description = "Working task"
            content.new {}
        },
    )

    fun taskValidationRequest(task: Task = workingTask()): TaskValidationRequest =
        taskValidationRequests.findOrCreateActive(task.id, task.data.owner.id)

    fun contest(owner: MultipleRoleUser = developer(), version: TrikStudioVersion = trikStudioVersion()): Contest {
        val ownerId = owner.id.value
        return contests.save(
            contestData {
                owner(ownerId)
                name = unique("Contest")
                description = "Contest description"
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
                trikStudioVersion = version
            },
        )
    }

    fun submission(
        author: MultipleRoleUser = developer(),
        task: Task = task(author),
        solution: Solution = solution(),
        version: TrikStudioVersion = trikStudioVersion(),
    ): Submission {
        val authorId = author.id
        val taskId = task.id.value
        val solutionId = solution.id.value
        return submissions.save(
            submissionData {
                this.author = authorId
                solution(solutionId)
                task(taskId)
                status.queued()
                kind.developerSolutionTest { trikStudioVersion = version }
            },
        )
    }

    fun gradingSubmission(authorId: UserId = student().id, contest: Contest = contest(), task: Task = task()): Submission {
        val taskId = task.id.value
        val solutionId = solution().id.value
        return submissions.save(
            submissionData {
                author = authorId
                task(taskId)
                solution(solutionId)
                status.queued()
                kind.grading { this.contest = contest.id }
            },
        )
    }

    fun successfulGradingVerdict(submission: Submission = gradingSubmission()): Verdict {
        val verdict = verdict(submission)
        submissions.update(submission.withData { status.graded { status.success { this.verdict = verdict.id } } })
        return verdict
    }

    fun verdict(submission: Submission = submission(), polygon: Polygon = polygon()): Verdict {
        val submissionId = submission.id.value
        val taskId = submission.data.task.id.value
        val polygonId = polygon.id.value
        val logsId = logs().id.value
        return verdicts.save(
            verdictData {
                task(taskId)
                submission(submissionId)
                testVerdict {
                    score = 100
                    test(polygonId)
                    logs(logsId)
                }
            },
        )
    }

    fun judgmentOrder(judge: MultipleRoleUser = judge(), submission: Submission = submission()): JudgmentOrder {
        val judgeId = judge.id.value
        val submissionId = submission.id.value
        return judgmentOrders.save(
            judgmentOrderData {
                judge(judgeId)
                submission(submissionId)
                score = 50
                reason = "Manual review"
            },
        )
    }

    fun logs(): Logs = logs.save(logsData { file(unique("logs") + ".txt", "logs".toByteArray()) })

    fun recording(): Recording = recordings.save(recordingData { file(unique("recording") + ".mp4", "recording".toByteArray()) })

    companion object {

        /**
         * Expiration moment of fixture invites that keeps them valid in every test.
         */
        val FAR_FUTURE: Instant = Instant.parse("2030-01-01T00:00:00Z")

        /**
         * Selects [language] on this chooser.
         */
        fun LanguageChooser.chose(language: TrikSupportedLanguage) = when (language) {
            TrikSupportedLanguage.Python -> python()
            TrikSupportedLanguage.JavaScript -> javaScript()
            TrikSupportedLanguage.VisualLanguage -> visualLanguage()
        }
    }
}
