package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.task.graded
import tech.testsys.domain.builder.task.inProgress
import tech.testsys.domain.builder.task.queued
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.SubmissionStatus

class SubmissionStatusChooser : Chooser<SubmissionStatus>() {

    fun graded(builder: GradingResult.Companion.() -> GradingResult) =
        makeChoice(builder(GradingResult.Companion).graded())

    fun queued() = makeChoice(SubmissionStatus.queued())

    fun inProgress() = makeChoice(SubmissionStatus.inProgress())

}
