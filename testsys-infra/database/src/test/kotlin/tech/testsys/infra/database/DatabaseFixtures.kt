package tech.testsys.infra.database

import org.springframework.boot.test.context.TestComponent
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.communityData
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
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.statementData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.Community
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
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import java.time.Duration
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
    private val multipleRoleUsers: MultipleRoleUserRepository,
    private val participants: ParticipantRepository,
    private val observers: ObserverRepository,
    private val supervisors: SupervisorRepository,
    private val communities: CommunityRepository,
    private val competitions: CompetitionRepository,
    private val classes: ClassRepository,
    private val solutions: SolutionRepository,
    private val polygons: TestRepository,
    private val exercises: ExerciseRepository,
    private val statements: StatementRepository,
    private val developerSolutions: DeveloperSolutionRepository,
    private val tasks: TaskRepository,
    private val contests: ContestRepository,
    private val submissions: SubmissionRepository,
    private val verdicts: VerdictRepository,
    private val judgmentOrders: JudgmentOrderRepository,
    private val logs: LogsRepository,
    private val recordings: RecordingRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
) {

    fun unique(prefix: String) = "$prefix-${UUID.randomUUID()}"

    fun email(prefix: String) = "${unique(prefix)}@example.com"

    fun multipleRoleUser(roles: MultipleRoleUserDataBuilder.() -> Unit): MultipleRoleUser = multipleRoleUsers.save(
        multipleRoleUserData {
            accessToken = unique("token")
            name = unique("User")
            email = email("user")
            roles()
        },
    )

    fun developer(): MultipleRoleUser = multipleRoleUser { roles { developer { data = developerData {} } } }

    fun student(): MultipleRoleUser = multipleRoleUser { roles { student { data = studentData {} } } }

    fun judge(): MultipleRoleUser = multipleRoleUser { roles { judge { data = judgeData {} } } }

    fun manager(): MultipleRoleUser = multipleRoleUser { roles { manager { data = managerData {} } } }

    fun participant(competition: Competition = competition()): Participant {
        val competitionId = competition.id.value
        return participants.save(
            participantData {
                competition(competitionId)
                accessToken = unique("token")
                name = unique("Participant")
            },
        )
    }

    fun observer(community: Community = community()): Observer {
        val communityId = community.id.value
        return observers.save(
            observerData {
                community(communityId)
                accessToken = unique("token")
                name = unique("Observer")
            },
        )
    }

    fun supervisor(): Supervisor = supervisors.save(
        supervisorData {
            accessToken = unique("token")
            name = unique("Supervisor")
        },
    )

    fun community(owner: MultipleRoleUser = developer()): Community {
        val ownerId = owner.id.value
        return communities.save(
            communityData {
                owner(ownerId)
                name = unique("Community")
                description = "Community description"
            },
        )
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
        return classes.save(
            classData {
                owner(ownerId)
                name = unique("Class")
                description = "Class description"
                students(studentIds)
            },
        )
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
            versionBucket = UUID.randomUUID()
        },
    )

    fun exercise(language: TrikSupportedLanguage = TrikSupportedLanguage.Python): Exercise = exercises.save(
        exerciseData {
            name = unique("Exercise")
            description = "Exercise description"
            file(unique("exercise") + ".qrs", "exercise".toByteArray())
            this.language.chose(language)
            versionBucket = UUID.randomUUID()
        },
    )

    fun statement(): Statement = statements.save(
        statementData {
            name = unique("Statement")
            description = "Statement description"
            file(unique("statement") + ".pdf", "statement".toByteArray())
            versionBucket = UUID.randomUUID()
        },
    )

    fun developerSolution(solution: Solution = solution()): DeveloperSolution {
        val solutionId = solution.id.value
        return developerSolutions.save(
            developerSolutionData {
                name = unique("Developer solution")
                description = "Developer solution description"
                solution(solutionId)
                expectedScore(100)
                versionBucket = UUID.randomUUID()
            },
        )
    }

    fun task(owner: MultipleRoleUser = developer()): Task {
        val ownerId = owner.id.value
        val exerciseId = exercise().id.value
        val statementId = statement().id.value
        return tasks.save(
            taskData {
                owner(ownerId)
                name = unique("Task")
                description = "Task description"
                content.committed {
                    exercise(exerciseId)
                    statement(statementId)
                }
            },
        )
    }

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

    fun submission(author: MultipleRoleUser = developer(), task: Task = task(author), solution: Solution = solution()): Submission {
        val authorId = author.id
        val taskId = task.id.value
        val solutionId = solution.id.value
        return submissions.save(
            submissionData {
                this.author = authorId
                solution(solutionId)
                task(taskId)
                status.queued()
                kind.developerSolutionTest()
            },
        )
    }

    fun verdict(submission: Submission = submission()): Verdict {
        val submissionId = submission.id.value
        val taskId = submission.data.task.id.value
        return verdicts.save(
            verdictData {
                score = 100
                task(taskId)
                submission(submissionId)
            },
        )
    }

    fun judgmentOrder(judge: MultipleRoleUser = judge(), verdict: Verdict = verdict()): JudgmentOrder {
        val judgeId = judge.id.value
        val verdictId = verdict.id.value
        return judgmentOrders.save(
            judgmentOrderData {
                judge(judgeId)
                verdict(verdictId)
                reason = "Manual review"
            },
        )
    }

    fun logs(): Logs = logs.save(logsData { file(unique("logs") + ".txt", "logs".toByteArray()) })

    fun recording(): Recording = recordings.save(recordingData { file(unique("recording") + ".mp4", "recording".toByteArray()) })

    companion object {

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
