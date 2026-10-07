package tech.testsys.operation.user

import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.MissedParticipantRoleError
import tech.testsys.operation.error.MissedStudentRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ViewParticipantContestError
import tech.testsys.operation.error.ViewStudentContestError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.hasRole
import java.time.Instant

/**
 * Operations shared by participants and students for studying available contests.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class StudyOperations(
    private val competitionRepository: CompetitionRepository,
    private val classRepository: ClassRepository,
    private val contestRepository: ContestRepository,
    private val participantContestEntryRepository: ParticipantContestEntryRepository,
    private val studentContestEntryRepository: StudentContestEntryRepository,
) {

    /**
     * Returns the first entry time and original [contestId] in the competition of [user].
     * Viewing is allowed regardless of dates and does not save an entry; storage exceptions propagate.
     *
     * @return the exact first entry time or `null`, paired with the original contest without resolving its tasks.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewContest")
    fun viewContest(user: SingleRoleUser, contestId: ContestId): OperationResult<Pair<Instant?, Contest>, ViewParticipantContestError> =
        operation<Pair<Instant?, Contest>, ViewParticipantContestError> {
            ensure(user is Participant, MissedParticipantRoleError)
            val competitionId = user.data.competition.id
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            ensure(contestId in competition.data.contests.ids) { ContestAccessDeniedError(contestId) }
            val entry = participantContestEntryRepository.findByContext(
                participantId = user.id,
                competitionId = competitionId,
                contestId = contestId,
            )
            return (entry?.data?.enteredAt to contest).asSuccess()
        }

    /**
     * Returns the first entry time in [classId] and original [contestId] for [user].
     * Viewing is allowed regardless of dates and does not save an entry; storage exceptions propagate.
     *
     * @return the exact first entry time or `null`, paired with the original contest without resolving its tasks.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewContest")
    fun viewContest(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
    ): OperationResult<Pair<Instant?, Contest>, ViewStudentContestError> = operation<Pair<Instant?, Contest>, ViewStudentContestError> {
        ensure(user.hasRole<Student>(), MissedStudentRoleError)
        val studyClass = classRepository.findById(classId)
        ensure(studyClass != null) { ClassNotExistsError(classId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        ensure(user.id in studyClass.data.students.ids) { ClassAccessDeniedError(classId) }
        ensure(contestId in studyClass.data.contests.ids) { ContestAccessDeniedError(contestId) }
        val entry = studentContestEntryRepository.findByContext(
            userId = user.id,
            studyClassId = classId,
            contestId = contestId,
        )
        return (entry?.data?.enteredAt to contest).asSuccess()
    }
}
