package tech.testsys.domain.builder.api

import tech.testsys.domain.builder.group.*
import tech.testsys.domain.builder.task.*
import tech.testsys.domain.builder.user.*
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.model.group.*
import tech.testsys.domain.model.task.*
import tech.testsys.domain.model.user.*

/**
 * DSL entry point for building [ClassData].
 *
 * @param builder the configuration block applied to [ClassDataBuilder].
 * @return the constructed [ClassData].
 * @since %CURRENT_VERSION%
 */
inline fun classData(builder: ClassDataBuilder.() -> Unit) =
    ClassDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Class].
 *
 * @param builder the configuration block applied to [ClassBuilder].
 * @return the constructed [Class].
 * @since %CURRENT_VERSION%
 */
inline fun `class`(builder: ClassBuilder.() -> Unit) =
    ClassBuilder().apply(builder).build()

/**
 * DSL entry point for building [CommunityData].
 *
 * @param builder the configuration block applied to [CommunityDataBuilder].
 * @return the constructed [CommunityData].
 * @since %CURRENT_VERSION%
 */
inline fun communityData(builder: CommunityDataBuilder.() -> Unit) =
    CommunityDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Community].
 *
 * @param builder the configuration block applied to [CommunityBuilder].
 * @return the constructed [Community].
 * @since %CURRENT_VERSION%
 */
inline fun community(builder: CommunityBuilder.() -> Unit) =
    CommunityBuilder().apply(builder).build()

/**
 * DSL entry point for building [CompetitionData].
 *
 * @param builder the configuration block applied to [CompetitionDataBuilder].
 * @return the constructed [CompetitionData].
 * @since %CURRENT_VERSION%
 */
inline fun competitionData(builder: CompetitionDataBuilder.() -> Unit) =
    CompetitionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Competition].
 *
 * @param builder the configuration block applied to [CompetitionBuilder].
 * @return the constructed [Competition].
 * @since %CURRENT_VERSION%
 */
inline fun competition(builder: CompetitionBuilder.() -> Unit) =
    CompetitionBuilder().apply(builder).build()

/**
 * DSL entry point for building [ContestData].
 *
 * @param builder the configuration block applied to [ContestDataBuilder].
 * @return the constructed [ContestData].
 * @since %CURRENT_VERSION%
 */
inline fun contestData(builder: ContestDataBuilder.() -> Unit) =
    ContestDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Contest].
 *
 * @param builder the configuration block applied to [ContestBuilder].
 * @return the constructed [Contest].
 * @since %CURRENT_VERSION%
 */
inline fun contest(builder: ContestBuilder.() -> Unit) =
    ContestBuilder().apply(builder).build()


/**
 * DSL entry point for building [DeveloperSolutionData].
 *
 * @param builder the configuration block applied to [DeveloperSolutionDataBuilder].
 * @return the constructed [DeveloperSolutionData].
 * @since %CURRENT_VERSION%
 */
inline fun developerSolutionData(builder: DeveloperSolutionDataBuilder.() -> Unit) =
    DeveloperSolutionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [DeveloperSolution].
 *
 * @param builder the configuration block applied to [DeveloperSolutionBuilder].
 * @return the constructed [DeveloperSolution].
 * @since %CURRENT_VERSION%
 */
inline fun developerSolution(builder: DeveloperSolutionBuilder.() -> Unit) =
    DeveloperSolutionBuilder().apply(builder).build()

/**
 * DSL entry point for building [ExerciseData].
 *
 * @param builder the configuration block applied to [ExerciseDataBuilder].
 * @return the constructed [ExerciseData].
 * @since %CURRENT_VERSION%
 */
inline fun exerciseData(builder: ExerciseDataBuilder.() -> Unit) =
    ExerciseDataBuilder().apply(builder).build()

/**
 * DSL entry point for building an [Exercise].
 *
 * @param builder the configuration block applied to [ExerciseBuilder].
 * @return the constructed [Exercise].
 * @since %CURRENT_VERSION%
 */
inline fun exercise(builder: ExerciseBuilder.() -> Unit) =
    ExerciseBuilder().apply(builder).build()


/**
 * DSL entry point for building [JudgmentOrderData].
 *
 * @param builder the configuration block applied to [JudgmentOrderDataBuilder].
 * @return the constructed [JudgmentOrderData].
 * @since %CURRENT_VERSION%
 */
inline fun judgmentOrderData(builder: JudgmentOrderDataBuilder.() -> Unit) =
    JudgmentOrderDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [JudgmentOrder].
 *
 * @param builder the configuration block applied to [JudgmentOrderBuilder].
 * @return the constructed [JudgmentOrder].
 * @since %CURRENT_VERSION%
 */
inline fun judgmentOrder(builder: JudgmentOrderBuilder.() -> Unit) =
    JudgmentOrderBuilder().apply(builder).build()


/**
 * DSL entry point for building [SolutionData].
 *
 * @param builder the configuration block applied to [SolutionDataBuilder].
 * @return the constructed [SolutionData].
 * @since %CURRENT_VERSION%
 */
inline fun solutionData(builder: SolutionDataBuilder.() -> Unit) =
    SolutionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Solution].
 *
 * @param builder the configuration block applied to [SolutionBuilder].
 * @return the constructed [Solution].
 * @since %CURRENT_VERSION%
 */
inline fun solution(builder: SolutionBuilder.() -> Unit) =
    SolutionBuilder().apply(builder).build()

/**
 * DSL entry point for building [VerdictData].
 *
 * @param builder the configuration block applied to [VerdictDataBuilder].
 * @return the constructed [VerdictData].
 * @since %CURRENT_VERSION%
 */
inline fun verdictData(builder: VerdictDataBuilder.() -> Unit) = VerdictDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Verdict].
 *
 * @param builder the configuration block applied to [VerdictBuilder].
 * @return the constructed [Verdict].
 * @since %CURRENT_VERSION%
 */
inline fun verdict(builder: VerdictBuilder.() -> Unit) = VerdictBuilder().apply(builder).build()

/**
 * Wraps this [GradingResult] into a [SubmissionStatus.Graded] status.
 *
 * @return the graded submission status containing this result.
 * @since %CURRENT_VERSION%
 */
fun GradingResult.graded() = SubmissionStatus.Graded(this)

/**
 * DSL entry point for building [StatementData].
 *
 * @param builder the configuration block applied to [StatementDataBuilder].
 * @return the constructed [StatementData].
 * @since %CURRENT_VERSION%
 */
inline fun statementData(builder: StatementDataBuilder.() -> Unit) =
    StatementDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Statement].
 *
 * @param builder the configuration block applied to [StatementBuilder].
 * @return the constructed [Statement].
 * @since %CURRENT_VERSION%
 */
inline fun statement(builder: StatementBuilder.() -> Unit) =
    StatementBuilder().apply(builder).build()

/**
 * DSL entry point for building [SubmissionData].
 *
 * @param builder the configuration block applied to [SubmissionDataBuilder].
 * @return the constructed [SubmissionData].
 * @since %CURRENT_VERSION%
 */
inline fun submissionData(builder: SubmissionDataBuilder.() -> Unit) =
    SubmissionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Submission].
 *
 * @param builder the configuration block applied to [SubmissionBuilder].
 * @return the constructed [Submission].
 * @since %CURRENT_VERSION%
 */
inline fun submission(builder: SubmissionBuilder.() -> Unit) =
    SubmissionBuilder().apply(builder).build()

/**
 * DSL entry point for building [TaskData].
 *
 * @param builder the configuration block applied to [TaskDataBuilder].
 * @return the constructed [TaskData].
 * @since %CURRENT_VERSION%
 */
inline fun taskData(builder: TaskDataBuilder.() -> Unit) =
    TaskDataBuilder().apply(builder).build()

/**
 * DSL entry point for building [TaskContent.New].
 *
 * @param builder the configuration block applied to [WipTaskContentBuilder].
 * @return the constructed [TaskContent.New].
 * @since %CURRENT_VERSION%
 */
inline fun taskContentNew(builder: WipTaskContentBuilder.() -> Unit) =
    TaskContent.New(WipTaskContentBuilder().apply(builder).build())

/**
 * DSL entry point for building [TaskContent.Committed].
 *
 * @param builder the configuration block applied to [CommittedTaskContentBuilder].
 * @return the constructed [TaskContent.Committed].
 * @since %CURRENT_VERSION%
 */
inline fun taskContentCommited(builder: CommittedTaskContentBuilder.() -> Unit) =
    TaskContent.Committed(CommittedTaskContentBuilder().apply(builder).build())

/**
 * DSL entry point for building [TaskContent.Uncommited].
 *
 * @param wipBuilder the configuration block applied to [WipTaskContentBuilder] for wip.
 * @param lastCommitedBuilder the configuration block applied to [CommittedTaskContentBuilder] for lastCommited.
 * @return the constructed [TaskContent.Uncommited].
 * @since %CURRENT_VERSION%
 */
inline fun taskContentUncommited(
    wipBuilder: WipTaskContentBuilder.() -> Unit,
    lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit,
) =
    TaskContent.Uncommited(
        WipTaskContentBuilder().apply(wipBuilder).build(),
        CommittedTaskContentBuilder().apply(lastCommitedBuilder).build()
    )

/**
 * DSL entry point for building a [Task].
 *
 * @param builder the configuration block applied to [TaskBuilder].
 * @return the constructed [Task].
 * @since %CURRENT_VERSION%
 */
inline fun task(builder: TaskBuilder.() -> Unit) =
    TaskBuilder().apply(builder).build()

/**
 * DSL entry point for building [TestData].
 *
 * @param builder the configuration block applied to [TestDataBuilder].
 * @return the constructed [TestData].
 * @since %CURRENT_VERSION%
 */
inline fun testData(builder: TestDataBuilder.() -> Unit) =
    TestDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Test].
 *
 * @param builder the configuration block applied to [TestBuilder].
 * @return the constructed [Test].
 * @since %CURRENT_VERSION%
 */
inline fun test(builder: TestBuilder.() -> Unit) =
    TestBuilder().apply(builder).build()

/**
 * DSL entry point for building [MultipleRoleUserData].
 *
 * @param builder the configuration block applied to [MultipleRoleUserDataBuilder].
 * @return the constructed [MultipleRoleUserData].
 * @since %CURRENT_VERSION%
 */
inline fun multipleRoleUserData(builder: MultipleRoleUserDataBuilder.() -> Unit) =
    MultipleRoleUserDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [MultipleRoleUser].
 *
 * @param builder the configuration block applied to [MultipleRoleUserBuilder].
 * @return the constructed [MultipleRoleUser].
 * @since %CURRENT_VERSION%
 */
inline fun multipleRoleUser(builder: MultipleRoleUserBuilder.() -> Unit) =
    MultipleRoleUserBuilder().apply(builder).build()

/**
 * DSL entry point for building [ObserverData].
 *
 * @param builder the configuration block applied to [ObserverDataBuilder].
 * @return the constructed [ObserverData].
 * @since %CURRENT_VERSION%
 */
inline fun observerData(builder: ObserverDataBuilder.() -> Unit) =
    ObserverDataBuilder().apply(builder).build()

/**
 * DSL entry point for building an [Observer].
 *
 * @param builder the configuration block applied to [ObserverBuilder].
 * @return the constructed [Observer].
 * @since %CURRENT_VERSION%
 */
inline fun observer(builder: ObserverBuilder.() -> Unit) =
    ObserverBuilder().apply(builder).build()

/**
 * DSL entry point for building [ParticipantData].
 *
 * @param builder the configuration block applied to [ParticipantDataBuilder].
 * @return the constructed [ParticipantData].
 * @since %CURRENT_VERSION%
 */
inline fun participantData(builder: ParticipantDataBuilder.() -> Unit) =
    ParticipantDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Participant].
 *
 * @param builder the configuration block applied to [ParticipantBuilder].
 * @return the constructed [Participant].
 * @since %CURRENT_VERSION%
 */
inline fun participant(builder: ParticipantBuilder.() -> Unit) =
    ParticipantBuilder().apply(builder).build()

/**
 * DSL entry point for building [SupervisorData].
 *
 * @param builder the configuration block applied to [SupervisorDataBuilder].
 * @return the constructed [SupervisorData].
 * @since %CURRENT_VERSION%
 */
inline fun supervisorData(builder: SupervisorDataBuilder.() -> Unit) =
    SupervisorDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Supervisor].
 *
 * @param builder the configuration block applied to [SupervisorBuilder].
 * @return the constructed [Supervisor].
 * @since %CURRENT_VERSION%
 */
inline fun supervisor(builder: SupervisorBuilder.() -> Unit) =
    SupervisorBuilder().apply(builder).build()

/**
 * DSL entry point for building [DeveloperData].
 *
 * @param builder the configuration block applied to [DeveloperDataBuilder].
 * @return the constructed [DeveloperData].
 * @since %CURRENT_VERSION%
 */
inline fun developerData(builder: DeveloperDataBuilder.() -> Unit) =
    DeveloperDataBuilder().apply(builder).build()

/**
 * DSL entry point for building [StudentData].
 *
 * @param builder the configuration block applied to [StudentDataBuilder].
 * @return the constructed [StudentData].
 * @since %CURRENT_VERSION%
 */
inline fun studentData(builder: StudentDataBuilder.() -> Unit) =
    StudentDataBuilder().apply(builder).build()

/**
 * DSL entry point for building [JudgeData].
 *
 * @param builder the configuration block applied to [JudgeDataBuilder].
 * @return the constructed [JudgeData].
 * @since %CURRENT_VERSION%
 */
inline fun judgeData(builder: JudgeDataBuilder.() -> Unit) =
    JudgeDataBuilder().apply(builder).build()

/**
 * DSL entry point for building [ManagerData].
 *
 * @param builder the configuration block applied to [ManagerDataBuilder].
 * @return the constructed [ManagerData].
 * @since %CURRENT_VERSION%
 */
inline fun managerData(builder: ManagerDataBuilder.() -> Unit) =
    ManagerDataBuilder().apply(builder).build()


private fun SubmissionData.toBuilder(): SubmissionDataBuilder {
    val thisData = this
    return SubmissionDataBuilder().apply {
        author = thisData.author.id
        solution = thisData.solution.id
        task = thisData.task.id
        judgmentOrders = thisData.judgmentOrders.ids.toMutableList()

        when (val originStatus = thisData.status) {
            is SubmissionStatus.Graded -> status.graded {
                when (val originGrade = originStatus.grade) {
                    is GradingResult.GradingError -> status.error { description = originGrade.description }
                    is GradingResult.Success -> status.success { verdict = originGrade.verdict.id }
                    GradingResult.Timeout -> status.timeout()
                }
            }
            SubmissionStatus.InProgress -> status.inProgress()
            SubmissionStatus.Queued -> status.queued()
        }

        when (val originKind = thisData.kind) {
            SubmissionKind.DeveloperSolutionTest -> kind.developerSolutionTest()
            is SubmissionKind.Grading -> kind.grading { contest = originKind.contest.id }
        }
    }
}

/**
 * Creates copy of [Submission] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Submission].
 * @return the modified [Submission].
 * @since %CURRENT_VERSION%
 */
fun Submission.withData(builder: SubmissionDataBuilder.() -> Unit): Submission {
    return Submission(
        this.id,
        this.createdAt,
        this.data.toBuilder().apply(builder).build()
    )
}

private fun ClassData.toBuilder(): ClassDataBuilder {
    val thisData = this
    return ClassDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        students = thisData.students.ids.toMutableList()
        contests = thisData.contests.ids.toMutableList()
    }
}

/**
 * Creates copy of [Class] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Class].
 * @return the modified [Class].
 * @since %CURRENT_VERSION%
 */
fun Class.withData(builder: ClassDataBuilder.() -> Unit): Class {
    return Class(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun CommunityData.toBuilder(): CommunityDataBuilder {
    val thisData = this
    return CommunityDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
    }
}

/**
 * Creates copy of [Community] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Community].
 * @return the modified [Community].
 * @since %CURRENT_VERSION%
 */
fun Community.withData(builder: CommunityDataBuilder.() -> Unit): Community {
    return Community(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun CompetitionData.toBuilder(): CompetitionDataBuilder {
    val thisData = this
    return CompetitionDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        participants = thisData.participants.ids.toMutableList()
        contests = thisData.contests.ids.toMutableList()
    }
}

/**
 * Creates copy of [Competition] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Competition].
 * @return the modified [Competition].
 * @since %CURRENT_VERSION%
 */
fun Competition.withData(builder: CompetitionDataBuilder.() -> Unit): Competition {
    return Competition(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun ContestData.toBuilder(): ContestDataBuilder {
    val thisData = this
    return ContestDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        tasks = thisData.tasks.ids.toMutableList()
        startsAt = thisData.startsAt
        contestDuration = thisData.contestDuration
        attemptDuration = thisData.attemptDuration
        trikStudioVersion = thisData.trikStudioVersion
        sharedTo = thisData.sharedTo.ids.toMutableList()
    }
}

/**
 * Creates copy of [Contest] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Contest].
 * @return the modified [Contest].
 * @since %CURRENT_VERSION%
 */
fun Contest.withData(builder: ContestDataBuilder.() -> Unit): Contest {
    return Contest(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun DeveloperSolutionData.toBuilder(): DeveloperSolutionDataBuilder {
    val thisData = this
    return DeveloperSolutionDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        solution = thisData.solution.id
        expectedScore = thisData.expectedScore
        versionBucket = thisData.versionBucket
    }
}

/**
 * Creates copy of [DeveloperSolution] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [DeveloperSolution].
 * @return the modified [DeveloperSolution].
 * @since %CURRENT_VERSION%
 */
fun DeveloperSolution.withData(builder: DeveloperSolutionDataBuilder.() -> Unit): DeveloperSolution {
    return DeveloperSolution(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun ExerciseData.toBuilder(): ExerciseDataBuilder {
    val thisData = this
    return ExerciseDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        file(thisData.file.uploadedFilename, thisData.file.content)
        when (thisData.language) {
            TrikSupportedLanguage.Python -> language.python()
            TrikSupportedLanguage.JavaScript -> language.javaScript()
            TrikSupportedLanguage.VisualLanguage -> language.visualLanguage()
        }
        versionBucket = thisData.versionBucket
    }
}

/**
 * Creates copy of [Exercise] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Exercise].
 * @return the modified [Exercise].
 * @since %CURRENT_VERSION%
 */
fun Exercise.withData(builder: ExerciseDataBuilder.() -> Unit): Exercise {
    return Exercise(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun JudgmentOrderData.toBuilder(): JudgmentOrderDataBuilder {
    val thisData = this
    return JudgmentOrderDataBuilder().apply {
        judge = thisData.judge.id
        verdict = thisData.verdict.id
        reason = thisData.reason
    }
}

/**
 * Creates copy of [JudgmentOrder] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [JudgmentOrder].
 * @return the modified [JudgmentOrder].
 * @since %CURRENT_VERSION%
 */
fun JudgmentOrder.withData(builder: JudgmentOrderDataBuilder.() -> Unit): JudgmentOrder {
    return JudgmentOrder(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun SolutionData.toBuilder(): SolutionDataBuilder {
    val thisData = this
    return SolutionDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        file(thisData.file.uploadedFilename, thisData.file.content)
        when (thisData.language) {
            TrikSupportedLanguage.Python -> language.python()
            TrikSupportedLanguage.JavaScript -> language.javaScript()
            TrikSupportedLanguage.VisualLanguage -> language.visualLanguage()
        }
        versionBucket = thisData.versionBucket
    }
}

/**
 * Creates copy of [Solution] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Solution].
 * @return the modified [Solution].
 * @since %CURRENT_VERSION%
 */
fun Solution.withData(builder: SolutionDataBuilder.() -> Unit): Solution {
    return Solution(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun StatementData.toBuilder(): StatementDataBuilder {
    val thisData = this
    return StatementDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        file(thisData.file.uploadedFilename, thisData.file.content)
        versionBucket = thisData.versionBucket
    }
}

/**
 * Creates copy of [Statement] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Statement].
 * @return the modified [Statement].
 * @since %CURRENT_VERSION%
 */
fun Statement.withData(builder: StatementDataBuilder.() -> Unit): Statement {
    return Statement(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun TestData.toBuilder(): TestDataBuilder {
    val thisData = this
    return TestDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        file(thisData.file.uploadedFilename, thisData.file.content)
        versionBucket = thisData.versionBucket
    }
}

/**
 * Creates copy of [Test] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Test].
 * @return the modified [Test].
 * @since %CURRENT_VERSION%
 */
fun Test.withData(builder: TestDataBuilder.() -> Unit): Test {
    return Test(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun WipTaskContentBuilder.populateFrom(content: WipTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercise = content.exercise?.id
    statement = content.statement?.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}

private fun CommittedTaskContentBuilder.populateFrom(content: CommitedTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercise = content.exercise.id
    statement = content.statement.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}

private fun TaskContentChooser.populateFrom(taskContent: TaskContent) {
    when (taskContent) {
        is TaskContent.New -> new { populateFrom(taskContent.wip) }
        is TaskContent.Uncommited -> uncommited(
            wipBuilder = { populateFrom(taskContent.wip) },
            lastCommitedBuilder = { populateFrom(taskContent.lastCommited) },
        )
        is TaskContent.Committed -> committed { populateFrom(taskContent.lastCommited) }
    }
}

private fun TaskData.toBuilder(): TaskDataBuilder {
    val thisData = this
    return TaskDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        sharedTo = thisData.sharedTo.ids.toMutableList()
        content.populateFrom(thisData.content)
    }
}

/**
 * Creates copy of [Task] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Task].
 * @return the modified [Task].
 * @since %CURRENT_VERSION%
 */
fun Task.withData(builder: TaskDataBuilder.() -> Unit): Task {
    return Task(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun VerdictData.toBuilder(): VerdictDataBuilder {
    val thisData = this
    return VerdictDataBuilder().apply {
        score = thisData.score.value
        task = thisData.task.id
        submission = thisData.submission.id
    }
}

/**
 * Creates copy of [Verdict] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Verdict].
 * @return the modified [Verdict].
 * @since %CURRENT_VERSION%
 */
fun Verdict.withData(builder: VerdictDataBuilder.() -> Unit): Verdict {
    return Verdict(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun ParticipantData.toBuilder(): ParticipantDataBuilder {
    val thisData = this
    return ParticipantDataBuilder().apply {
        competition = thisData.competition.id
        accessToken = thisData.accessToken
        name = thisData.name
    }
}

/**
 * Creates copy of [Participant] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Participant].
 * @return the modified [Participant].
 * @since %CURRENT_VERSION%
 */
fun Participant.withData(builder: ParticipantDataBuilder.() -> Unit): Participant {
    return Participant(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun ObserverData.toBuilder(): ObserverDataBuilder {
    val thisData = this
    return ObserverDataBuilder().apply {
        community = thisData.community.id
        accessToken = thisData.accessToken
        competitions = thisData.competitions.ids.toMutableList()
        name = thisData.name
    }
}

/**
 * Creates copy of [Observer] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Observer].
 * @return the modified [Observer].
 * @since %CURRENT_VERSION%
 */
fun Observer.withData(builder: ObserverDataBuilder.() -> Unit): Observer {
    return Observer(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun MultipleRoleUserData.toBuilder(): MultipleRoleUserDataBuilder {
    val thisData = this
    return MultipleRoleUserDataBuilder().apply {
        accessToken = thisData.accessToken
        name = thisData.name
        email = thisData.email
        roles { addAll(thisData.roles) }
    }
}

/**
 * Creates copy of [MultipleRoleUser] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [MultipleRoleUser].
 * @return the modified [MultipleRoleUser].
 * @since %CURRENT_VERSION%
 */
fun MultipleRoleUser.withData(builder: MultipleRoleUserDataBuilder.() -> Unit): MultipleRoleUser {
    return MultipleRoleUser(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun SupervisorData.toBuilder(): SupervisorDataBuilder {
    val thisData = this
    return SupervisorDataBuilder().apply {
        accessToken = thisData.accessToken
        name = thisData.name
    }
}

/**
 * Creates copy of [Supervisor] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Supervisor].
 * @return the modified [Supervisor].
 * @since %CURRENT_VERSION%
 */
fun Supervisor.withData(builder: SupervisorDataBuilder.() -> Unit): Supervisor {
    return Supervisor(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}
