package tech.testsys.web.app.service.study

import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.operation.user.StudyOperations
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.ExerciseVo
import tech.testsys.web.app.service.developer.StatementVo
import tech.testsys.web.app.service.developer.toVo
import tech.testsys.web.app.service.toVo

/**
 * Task of a contest as a participant or a student sees it, for pages.
 *
 * @property task the task.
 * @property statement the statement of the last committed revision, or `null` if the task has no committed revision.
 * @property exercises the exercises of the last committed revision.
 * @property languages the languages a solution can be sent in.
 * @property submissions the submissions of the user.
 * @property best the submission with the highest final score, or `null` if none has a successful verdict.
 * @since %CURRENT_VERSION%
 */
data class StudyTaskVo(
    val task: TaskVo,
    val statement: StatementVo?,
    val exercises: List<ExerciseVo>,
    val languages: List<TrikSupportedLanguage>,
    val submissions: List<StudySubmissionVo>,
    val best: StudySubmissionVo?,
)

/**
 * Submission of a [StudyTaskVo] with the file name of its solution and its final score.
 *
 * @property submission the submission.
 * @property filename the uploaded file name of the solution.
 * @property score the final score, or `null` without a successful verdict.
 * @since %CURRENT_VERSION%
 */
data class StudySubmissionVo(
    val submission: SubmissionVo,
    val filename: String,
    val score: Int?,
)

internal fun StudyOperations.StudyTask.toVo(): StudyTaskVo = StudyTaskVo(
    task = task.toVo(),
    statement = statement?.toVo(),
    exercises = exercises.map { exercise -> exercise.toVo() },
    languages = languages,
    submissions = submissions.map { submission -> submission.toVo() },
    best = best?.toVo(),
)

private fun StudyOperations.StudySubmission.toVo() = StudySubmissionVo(submission = submission.toVo(), filename = filename, score = score)
