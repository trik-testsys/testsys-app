package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.task.developerSolutionTest
import tech.testsys.domain.builder.task.grading
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.SubmissionKind

class SubmissionKindChooser : Chooser<SubmissionKind>() {

    fun grading(contest: ContestId) = makeChoice(SubmissionKind.grading(contest))

    fun grading(contest: Long) = makeChoice(SubmissionKind.grading(contest))

    fun developerSolutionTest() = makeChoice(SubmissionKind.developerSolutionTest())

}
