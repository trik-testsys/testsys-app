package tech.testsys.web.app.service.student

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.StudentOperations
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * Runs [StudentOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class StudentService(private val operations: StudentOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [StudentOperations.viewContests].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewContests(classId: ClassId): List<Pair<Instant?, ContestVo>> = operations.viewContests(currentUser.multipleRoleUser(), classId)
        .getOrThrow().map { (enteredAt, contest) -> enteredAt to contest.toVo() }

    /**
     * Runs [StudentOperations.enterContest].
     *
     * @since %CURRENT_VERSION%
     */
    fun enterContest(classId: ClassId, contestId: ContestId): StudentContestEntryVo =
        operations.enterContest(currentUser.multipleRoleUser(), classId, contestId).getOrThrow().toVo()

    /**
     * Runs [StudentOperations.viewClasses].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewClasses(): List<ClassVo> =
        operations.viewClasses(currentUser.multipleRoleUser()).getOrThrow().map { studyClass -> studyClass.toVo() }
}
