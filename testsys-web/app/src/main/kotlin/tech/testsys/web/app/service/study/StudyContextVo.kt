package tech.testsys.web.app.service.study

import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.operation.user.StudyOperations
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.student.ClassVo
import tech.testsys.web.app.service.student.toVo
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * Contest in the study context of a participant or a student without its tasks, for the head of its page.
 *
 * @property studyClass the selected class of a student, or `null` for a participant.
 * @property enteredAt the saved first entry time in this context, or `null` before entry.
 * @property contest the contest.
 * @since %CURRENT_VERSION%
 */
data class StudyContestContextVo(val studyClass: ClassVo?, val enteredAt: Instant?, val contest: ContestVo)

/**
 * Task of an entered contest in the study context of a participant or a student without its resources and submissions,
 * for the head of its page and the solution form.
 *
 * @property studyClass the selected class of a student, or `null` for a participant.
 * @property enteredAt the saved first entry time in this context.
 * @property contest the contest.
 * @property task the task.
 * @property languages the languages a solution can be sent in.
 * @since %CURRENT_VERSION%
 */
data class StudyTaskContextVo(
    val studyClass: ClassVo?,
    val enteredAt: Instant,
    val contest: ContestVo,
    val task: TaskVo,
    val languages: List<TrikSupportedLanguage>,
)

internal fun StudyOperations.StudyContestContext.toVo(): StudyContestContextVo =
    StudyContestContextVo(studyClass = studyClass?.toVo(), enteredAt = enteredAt, contest = contest.toVo())

internal fun StudyOperations.StudyTaskContext.toVo(): StudyTaskContextVo = StudyTaskContextVo(
    studyClass = studyClass?.toVo(),
    enteredAt = enteredAt,
    contest = contest.toVo(),
    task = task.toVo(),
    languages = languages,
)
