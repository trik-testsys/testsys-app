package tech.testsys.operation.user

import tech.testsys.domain.builder.api.studentContestEntryData
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestEndedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.ContestNotStartedError
import tech.testsys.operation.error.EnterStudentContestError
import tech.testsys.operation.error.MissedStudentRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ViewStudentContestsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.hasRole
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Operations performed by students in a selected class.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class StudentOperations(
    private val classRepository: ClassRepository,
    private val contestRepository: ContestRepository,
    private val contestEntryRepository: StudentContestEntryRepository,
    private val clock: Clock,
) {

    /**
     * Returns pairs of first entry time and contest for [user] in [classId], including future and completed contests.
     * The time is null until entry in this class; entries in other classes are independent.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.student.viewContests")
    fun viewContests(user: MultipleRoleUser, classId: ClassId): OperationResult<List<Pair<Instant?, Contest>>, ViewStudentContestsError> =
        operation<List<Pair<Instant?, Contest>>, ViewStudentContestsError> {
            ensure(user.hasRole<Student>(), MissedStudentRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            ensure(user.id in studyClass.data.students.ids) { ClassAccessDeniedError(classId) }
            val contests = studyClass.data.contests.load(contestRepository)
            val entries = contestEntryRepository.findByContests(
                userId = user.id,
                studyClassId = classId,
                contestIds = contests.map { it.id },
            ).associateBy { it.data.contest.id }
            return contests.map { contest -> entries[contest.id]?.data?.enteredAt to contest }.asSuccess()
        }

    /**
     * Returns the first entry of [user] into [contestId] in [classId].
     * A new entry requires the current start-inclusive, end-exclusive interval; a repeat keeps its first time.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.student.enterContest")
    fun enterContest(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
    ): OperationResult<StudentContestEntry, EnterStudentContestError> = operation<StudentContestEntry, EnterStudentContestError> {
        ensure(user.hasRole<Student>(), MissedStudentRoleError)
        val studyClass = classRepository.findById(classId)
        ensure(studyClass != null) { ClassNotExistsError(classId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        ensure(user.id in studyClass.data.students.ids) { ClassAccessDeniedError(classId) }
        ensure(contestId in studyClass.data.contests.ids) { ContestAccessDeniedError(contestId) }
        val existing = contestEntryRepository.findByContext(
            userId = user.id,
            studyClassId = classId,
            contestId = contestId,
        )
        if (existing != null) return existing.asSuccess()
        val now = clock.instant()
        val startsAt = contest.data.startsAt
        val endsAt = contest.data.endsAt
        if ((startsAt != null && now.isBefore(startsAt)) || (endsAt != null && !now.isBefore(endsAt))) {
            val concurrentEntry = contestEntryRepository.findByContext(
                userId = user.id,
                studyClassId = classId,
                contestId = contestId,
            )
            if (concurrentEntry != null) return concurrentEntry.asSuccess()
        }
        ensure(startsAt == null || !now.isBefore(startsAt)) { ContestNotStartedError(contestId, requireNotNull(startsAt)) }
        ensure(endsAt == null || now.isBefore(endsAt)) { ContestEndedError(contestId, requireNotNull(endsAt)) }
        val data = studentContestEntryData {
            this.user = user.id
            this.studyClass = classId
            this.contest = contestId
            enteredAt = now.truncatedTo(ChronoUnit.MICROS)
        }
        return contestEntryRepository.findOrCreate(data).asSuccess()
    }
}
