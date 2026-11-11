package tech.testsys.web.app.view

import tech.testsys.infra.localization.PageParticipant
import tech.testsys.infra.localization.PageStudent
import tech.testsys.web.components.texts.currentLocalization

/**
 * Texts of the contest and task pages that a Student and a Participant share; each role reads them from its own bundle in the
 * current localization.
 */
internal interface StudyTexts {
    /** State of a contest for the user. */
    enum class ContestState { NOT_STARTED, RUNNING, FINISHED }

    /** Grading status of a submission. */
    enum class SubmissionStatus { QUEUED, IN_PROGRESS, SUCCESS, GRADING_ERROR, TIMEOUT }

    /** Language of a solution. */
    enum class Language { PYTHON, JAVASCRIPT, VISUAL_LANGUAGE }

    fun id(): String
    fun name(): String
    fun description(): String
    fun detailsTitle(): String
    fun startsAt(): String
    fun endsAt(): String
    fun attemptDuration(): String
    fun contestState(state: ContestState): String
    fun remainingTitle(): String
    fun remainingGuide(): String
    fun remainingOver(): String
    fun remainingNotStarted(): String
    fun remainingUnlimited(): String
    fun submissionStatus(status: SubmissionStatus): String
    fun contestTabTitle(): String
    fun contestTitle(name: String): String
    fun contestGuide(): String
    fun contestStart(): String
    fun contestStartConfirmTitle(name: String): String
    fun contestStartConfirmText(): String
    fun contestStartConfirmAction(): String
    fun contestStarted(): String
    fun contestDetailsGuide(): String
    fun contestTasksGuide(): String
    fun contestTasksScore(): String
    fun contestTasksLastSubmission(): String
    fun contestTasksEmpty(): String
    fun taskTabTitle(): String
    fun taskTitle(name: String): String
    fun taskGuide(): String
    fun taskDetailsGuide(): String
    fun taskFiles(): String
    fun taskFilesEmpty(): String
    fun taskDownloadStatement(): String
    fun taskDownloadExercise(language: String): String
    fun taskBest(): String
    fun taskBestNone(): String
    fun taskSendTitle(): String
    fun taskSendUnavailable(): String
    fun taskSendLanguage(): String
    fun taskSendLanguageName(language: Language): String
    fun taskSendFile(): String
    fun taskSendGuide(): String
    fun taskSendLanguageStepTitle(): String
    fun taskSendLanguageStepDescription(): String
    fun taskSendFileStepDescription(): String
    fun taskSendSubmit(): String
    fun taskSendLanguageRequired(): String
    fun taskSendFileRequired(): String
    fun taskSendSent(): String
    fun taskSubmissionsTitle(): String
    fun taskSubmissionsGuide(): String
    fun taskSubmissionsSent(): String
    fun taskSubmissionsFile(): String
    fun taskSubmissionsResult(): String
    fun taskSubmissionsBest(): String
    fun taskSubmissionsDownload(): String
    fun taskSubmissionsEmpty(): String
    fun taskSubmissionsEmptyHintSendable(): String
    fun taskSubmissionsEmptyHintClosed(): String
}

/** Study texts of a Student from the `page_student` bundle. */
internal object StudentStudyTexts : StudyTexts {
    private val texts: PageStudent get() = currentLocalization().pageStudent

    override fun id() = texts.id()
    override fun name() = texts.name()
    override fun description() = texts.description()
    override fun detailsTitle() = texts.detailsTitle()
    override fun startsAt() = texts.startsAt()
    override fun endsAt() = texts.endsAt()
    override fun attemptDuration() = texts.attemptDuration()
    override fun contestState(state: StudyTexts.ContestState) = texts.contestState(state = enumValueOf(state.name))
    override fun remainingTitle() = texts.remainingTitle()
    override fun remainingGuide() = texts.remainingGuide()
    override fun remainingOver() = texts.remainingOver()
    override fun remainingNotStarted() = texts.remainingNotStarted()
    override fun remainingUnlimited() = texts.remainingUnlimited()
    override fun submissionStatus(status: StudyTexts.SubmissionStatus) = texts.submissionStatus(status = enumValueOf(status.name))
    override fun contestTabTitle() = texts.contestTabTitle()
    override fun contestTitle(name: String) = texts.contestTitle(name = name)
    override fun contestGuide() = texts.contestGuide()
    override fun contestStart() = texts.contestStart()
    override fun contestStartConfirmTitle(name: String) = texts.contestStartConfirmTitle(name = name)
    override fun contestStartConfirmText() = texts.contestStartConfirmText()
    override fun contestStartConfirmAction() = texts.contestStartConfirmAction()
    override fun contestStarted() = texts.contestStarted()
    override fun contestDetailsGuide() = texts.contestDetailsGuide()
    override fun contestTasksGuide() = texts.contestTasksGuide()
    override fun contestTasksScore() = texts.contestTasksScore()
    override fun contestTasksLastSubmission() = texts.contestTasksLastSubmission()
    override fun contestTasksEmpty() = texts.contestTasksEmpty()
    override fun taskTabTitle() = texts.taskTabTitle()
    override fun taskTitle(name: String) = texts.taskTitle(name = name)
    override fun taskGuide() = texts.taskGuide()
    override fun taskDetailsGuide() = texts.taskDetailsGuide()
    override fun taskFiles() = texts.taskFiles()
    override fun taskFilesEmpty() = texts.taskFilesEmpty()
    override fun taskDownloadStatement() = texts.taskDownloadStatement()
    override fun taskDownloadExercise(language: String) = texts.taskDownloadExercise(language = language)
    override fun taskBest() = texts.taskBest()
    override fun taskBestNone() = texts.taskBestNone()
    override fun taskSendTitle() = texts.taskSendTitle()
    override fun taskSendUnavailable() = texts.taskSendUnavailable()
    override fun taskSendLanguage() = texts.taskSendLanguage()
    override fun taskSendLanguageName(language: StudyTexts.Language) = texts.taskSendLanguageName(language = enumValueOf(language.name))
    override fun taskSendFile() = texts.taskSendFile()
    override fun taskSendGuide() = texts.taskSendGuide()
    override fun taskSendLanguageStepTitle() = texts.taskSendLanguageStepTitle()
    override fun taskSendLanguageStepDescription() = texts.taskSendLanguageStepDescription()
    override fun taskSendFileStepDescription() = texts.taskSendFileStepDescription()
    override fun taskSendSubmit() = texts.taskSendSubmit()
    override fun taskSendLanguageRequired() = texts.taskSendLanguageRequired()
    override fun taskSendFileRequired() = texts.taskSendFileRequired()
    override fun taskSendSent() = texts.taskSendSent()
    override fun taskSubmissionsTitle() = texts.taskSubmissionsTitle()
    override fun taskSubmissionsGuide() = texts.taskSubmissionsGuide()
    override fun taskSubmissionsSent() = texts.taskSubmissionsSent()
    override fun taskSubmissionsFile() = texts.taskSubmissionsFile()
    override fun taskSubmissionsResult() = texts.taskSubmissionsResult()
    override fun taskSubmissionsBest() = texts.taskSubmissionsBest()
    override fun taskSubmissionsDownload() = texts.taskSubmissionsDownload()
    override fun taskSubmissionsEmpty() = texts.taskSubmissionsEmpty()
    override fun taskSubmissionsEmptyHintSendable() = texts.taskSubmissionsEmptyHintSendable()
    override fun taskSubmissionsEmptyHintClosed() = texts.taskSubmissionsEmptyHintClosed()
}

/** Study texts of a Participant from the `page_participant` bundle. */
internal object ParticipantStudyTexts : StudyTexts {
    private val texts: PageParticipant get() = currentLocalization().pageParticipant

    override fun id() = texts.id()
    override fun name() = texts.name()
    override fun description() = texts.description()
    override fun detailsTitle() = texts.detailsTitle()
    override fun startsAt() = texts.startsAt()
    override fun endsAt() = texts.endsAt()
    override fun attemptDuration() = texts.attemptDuration()
    override fun contestState(state: StudyTexts.ContestState) = texts.contestState(state = enumValueOf(state.name))
    override fun remainingTitle() = texts.remainingTitle()
    override fun remainingGuide() = texts.remainingGuide()
    override fun remainingOver() = texts.remainingOver()
    override fun remainingNotStarted() = texts.remainingNotStarted()
    override fun remainingUnlimited() = texts.remainingUnlimited()
    override fun submissionStatus(status: StudyTexts.SubmissionStatus) = texts.submissionStatus(status = enumValueOf(status.name))
    override fun contestTabTitle() = texts.contestTabTitle()
    override fun contestTitle(name: String) = texts.contestTitle(name = name)
    override fun contestGuide() = texts.contestGuide()
    override fun contestStart() = texts.contestStart()
    override fun contestStartConfirmTitle(name: String) = texts.contestStartConfirmTitle(name = name)
    override fun contestStartConfirmText() = texts.contestStartConfirmText()
    override fun contestStartConfirmAction() = texts.contestStartConfirmAction()
    override fun contestStarted() = texts.contestStarted()
    override fun contestDetailsGuide() = texts.contestDetailsGuide()
    override fun contestTasksGuide() = texts.contestTasksGuide()
    override fun contestTasksScore() = texts.contestTasksScore()
    override fun contestTasksLastSubmission() = texts.contestTasksLastSubmission()
    override fun contestTasksEmpty() = texts.contestTasksEmpty()
    override fun taskTabTitle() = texts.taskTabTitle()
    override fun taskTitle(name: String) = texts.taskTitle(name = name)
    override fun taskGuide() = texts.taskGuide()
    override fun taskDetailsGuide() = texts.taskDetailsGuide()
    override fun taskFiles() = texts.taskFiles()
    override fun taskFilesEmpty() = texts.taskFilesEmpty()
    override fun taskDownloadStatement() = texts.taskDownloadStatement()
    override fun taskDownloadExercise(language: String) = texts.taskDownloadExercise(language = language)
    override fun taskBest() = texts.taskBest()
    override fun taskBestNone() = texts.taskBestNone()
    override fun taskSendTitle() = texts.taskSendTitle()
    override fun taskSendUnavailable() = texts.taskSendUnavailable()
    override fun taskSendLanguage() = texts.taskSendLanguage()
    override fun taskSendLanguageName(language: StudyTexts.Language) = texts.taskSendLanguageName(language = enumValueOf(language.name))
    override fun taskSendFile() = texts.taskSendFile()
    override fun taskSendGuide() = texts.taskSendGuide()
    override fun taskSendLanguageStepTitle() = texts.taskSendLanguageStepTitle()
    override fun taskSendLanguageStepDescription() = texts.taskSendLanguageStepDescription()
    override fun taskSendFileStepDescription() = texts.taskSendFileStepDescription()
    override fun taskSendSubmit() = texts.taskSendSubmit()
    override fun taskSendLanguageRequired() = texts.taskSendLanguageRequired()
    override fun taskSendFileRequired() = texts.taskSendFileRequired()
    override fun taskSendSent() = texts.taskSendSent()
    override fun taskSubmissionsTitle() = texts.taskSubmissionsTitle()
    override fun taskSubmissionsGuide() = texts.taskSubmissionsGuide()
    override fun taskSubmissionsSent() = texts.taskSubmissionsSent()
    override fun taskSubmissionsFile() = texts.taskSubmissionsFile()
    override fun taskSubmissionsResult() = texts.taskSubmissionsResult()
    override fun taskSubmissionsBest() = texts.taskSubmissionsBest()
    override fun taskSubmissionsDownload() = texts.taskSubmissionsDownload()
    override fun taskSubmissionsEmpty() = texts.taskSubmissionsEmpty()
    override fun taskSubmissionsEmptyHintSendable() = texts.taskSubmissionsEmptyHintSendable()
    override fun taskSubmissionsEmptyHintClosed() = texts.taskSubmissionsEmptyHintClosed()
}
