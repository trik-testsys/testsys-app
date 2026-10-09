package tech.testsys.web.app

import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.developerSolutionData
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.participantContestEntryData
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.statementData
import tech.testsys.domain.builder.api.studentContestEntryData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.UserId
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Stores the classes, contests, tasks, submissions and contest entries that the pages of students and participants show.
 * [AppTestConfiguration] imports it as a bean.
 */
class StudyFixtures(
    private val fixtures: AppFixtures,
    private val classes: ClassRepository,
    private val competitions: CompetitionRepository,
    private val participants: ParticipantRepository,
    private val contests: ContestRepository,
    private val tasks: TaskRepository,
    private val statements: StatementRepository,
    private val exercises: ExerciseRepository,
    private val solutions: SolutionRepository,
    private val developerSolutions: DeveloperSolutionRepository,
    private val polygons: TestRepository,
    private val logs: LogsRepository,
    private val verdicts: VerdictRepository,
    private val submissions: SubmissionRepository,
    private val studentEntries: StudentContestEntryRepository,
    private val participantEntries: ParticipantContestEntryRepository,
    private val dataSource: DataSource,
) {
    /** Returns a new user holding only the student role. */
    fun student(): MultipleRoleUser = fixtures.multipleRoleUser { roles { student { data = studentData {} } } }

    fun studentClass(
        students: List<MultipleRoleUser>,
        contests: List<Contest> = emptyList(),
        name: String = fixtures.unique("Class"),
        inviteExpiresAt: Instant = FAR_FUTURE,
    ): Class {
        val ownerId = fixtures.multipleRoleUser().id.value
        val invite = classInviteData {
            code(fixtures.unique("code"), HashAlgorithm.Identity)
            expiresAt = inviteExpiresAt
        }
        return classes.saveWithInvite(invite) { inviteId ->
            classData {
                owner(ownerId)
                this.name = name
                description = "Описание класса"
                students(students.map { student -> student.id.value })
                contests(contests.map { contest -> contest.id.value })
                this.invite = inviteId
            }
        }
    }

    fun contest(
        name: String = fixtures.unique("Contest"),
        tasks: List<Task> = emptyList(),
        startsAt: Instant? = null,
        contestDuration: Duration? = null,
        attemptDuration: Duration? = null,
    ): Contest {
        val ownerId = fixtures.multipleRoleUser().id.value
        val version = trikStudioVersion()
        return contests.save(
            contestData {
                owner(ownerId)
                this.name = name
                description = "Описание тура"
                tasks(tasks.map { task -> task.id.value })
                this.startsAt = startsAt
                this.contestDuration = contestDuration
                this.attemptDuration = attemptDuration
                trikStudioVersion(version)
            },
        )
    }

    /** Returns a contest of [tasks] that started at [PAST] and ends at [FAR_FUTURE]. */
    fun runningContest(name: String = fixtures.unique("Contest"), tasks: List<Task> = emptyList()): Contest =
        contest(name = name, tasks = tasks, startsAt = PAST, contestDuration = Duration.between(PAST, FAR_FUTURE))

    /** Returns a committed task with a statement, an exercise and a developer solution in each of [languages]. */
    fun task(name: String = fixtures.unique("Task"), languages: List<TrikSupportedLanguage> = listOf(TrikSupportedLanguage.Python)): Task {
        val ownerId = fixtures.multipleRoleUser().id.value
        val statement = statements.save(
            statementData {
                this.name = "Условие"
                description = ""
                file("statement.pdf", "statement".toByteArray())
                versionBucket = VersionBucket(UUID.randomUUID())
            },
        )
        val exercise = exercises.save(
            exerciseData {
                this.name = "Разминка"
                description = ""
                file("exercise.qrs", "exercise".toByteArray())
                language.python()
                versionBucket = VersionBucket(UUID.randomUUID())
            },
        )
        val developerSolutionIds = languages.map { language ->
            val solutionId = solution("reference.qrs", language).id.value
            developerSolutions.save(
                developerSolutionData {
                    this.name = "Эталон"
                    description = ""
                    solution(solutionId)
                    expectedScore(100)
                    versionBucket = VersionBucket(UUID.randomUUID())
                },
            ).id.value
        }
        return tasks.save(
            taskData {
                owner(ownerId)
                this.name = name
                description = "Описание задачи"
                uploadedResources = mutableSetOf(statement.data.versionBucket, exercise.data.versionBucket)
                content.committed {
                    exercises(listOf(exercise.id.value))
                    statement(statement.id.value)
                    developerSolutions(developerSolutionIds)
                }
            },
        )
    }

    /** Returns a participant of a new competition of [contests]. */
    fun participant(contests: List<Contest>): Participant {
        val ownerId = fixtures.multipleRoleUser().id.value
        val competitionId = competitions.save(
            competitionData {
                owner(ownerId)
                name = fixtures.unique("Competition")
                description = "Competition"
                contests(contests.map { contest -> contest.id.value })
            },
        ).id.value
        return participants.save(
            participantData {
                competition(competitionId)
                accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
                name = fixtures.unique("Participant")
            },
        )
    }

    fun studentEntry(student: MultipleRoleUser, studyClass: Class, contest: Contest, enteredAt: Instant = PAST) {
        studentEntries.findOrCreate(
            studentContestEntryData {
                user = student.id
                this.studyClass = studyClass.id
                this.contest = contest.id
                this.enteredAt = enteredAt
            },
        )
    }

    fun participantEntry(participant: Participant, contest: Contest, enteredAt: Instant = PAST) {
        participantEntries.findOrCreate(
            participantContestEntryData {
                this.participant = participant.id
                competition = participant.data.competition.id
                this.contest = contest.id
                this.enteredAt = enteredAt
            },
        )
    }

    /** Returns a queued submission of [filename] by [author], or a graded one with a verdict of [score] if given. */
    fun submission(author: UserId, task: Task, contest: Contest, filename: String, score: Int? = null): Submission {
        val solutionId = solution(filename, TrikSupportedLanguage.Python).id.value
        val taskId = task.id.value
        val submission = submissions.save(
            submissionData {
                this.author = author
                solution(solutionId)
                task(taskId)
                status.queued()
                kind.grading { this.contest = contest.id }
            },
        )
        if (score == null) return submission

        val polygonId = polygons.save(
            testData {
                name = "Полигон"
                description = ""
                file("polygon.xml", "<field/>".toByteArray())
                versionBucket = VersionBucket(UUID.randomUUID())
            },
        ).id.value
        val logsId = logs.save(logsData { file("logs.txt", "logs".toByteArray()) }).id.value
        val submissionId = submission.id.value
        val verdict = verdicts.save(
            verdictData {
                task(taskId)
                submission(submissionId)
                testVerdict {
                    this.score = score
                    test(polygonId)
                    logs(logsId)
                }
            },
        )
        return submissions.update(submission.withData { status.graded { status.success { this.verdict = verdict.id } } })
    }

    private fun solution(filename: String, language: TrikSupportedLanguage) = solutions.save(
        solutionData {
            file(filename, "solution".toByteArray())
            this.language.choose(language)
        },
    )

    /** Adds a TRIK Studio version: versions come from the grader and no port stores them. */
    private fun trikStudioVersion(): String {
        val tag = fixtures.unique("tsv")
        dataSource.connection.use { connection ->
            val insert = "insert into ts_trik_studio_version (id, tag) select coalesce(max(id), 0) + 1, ? from ts_trik_studio_version"
            connection.prepareStatement(insert).use { statement ->
                statement.setString(1, tag)
                statement.executeUpdate()
            }
        }
        return tag
    }

    private fun LanguageChooser.choose(language: TrikSupportedLanguage) = when (language) {
        TrikSupportedLanguage.Python -> python()
        TrikSupportedLanguage.JavaScript -> javaScript()
        TrikSupportedLanguage.VisualLanguage -> visualLanguage()
    }

    companion object {
        /** Moment far before any contest of the tests ends. */
        val PAST: Instant = Instant.parse("2020-01-01T00:00:00Z")

        /** Moment far after the tests run. */
        val FAR_FUTURE: Instant = Instant.parse("2100-01-01T00:00:00Z")
    }
}
