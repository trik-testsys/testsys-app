package tech.testsys.web.app.service.student

import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * First entry of a student into a contest of a class for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the entry.
 * @property createdAt the moment the entry was created.
 * @property user the identifier of the student.
 * @property studyClass the identifier of the class.
 * @property contest the identifier of the contest.
 * @property enteredAt the moment of the first entry.
 * @since %CURRENT_VERSION%
 */
data class StudentContestEntryVo(
    val id: StudentContestEntryId,
    val createdAt: Instant,
    val user: MultipleRoleUserId,
    val studyClass: ClassId,
    val contest: ContestId,
    val enteredAt: Instant,
)

internal fun StudentContestEntry.toVo(): StudentContestEntryVo = StudentContestEntryVo(
    id = id,
    createdAt = createdAt,
    user = data.user.id,
    studyClass = data.studyClass.id,
    contest = data.contest.id,
    enteredAt = data.enteredAt,
)
