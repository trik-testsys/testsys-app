package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.infra.localization.PageJudge.SolutionLanguageLanguage
import tech.testsys.infra.localization.PageJudge.SolutionStatusStatus
import tech.testsys.web.app.service.developer.TestVo
import tech.testsys.web.app.service.judge.JudgeService
import tech.testsys.web.app.service.judge.JudgmentOrderVo
import tech.testsys.web.app.service.judge.NamedUserVo
import tech.testsys.web.app.service.judge.SubmissionDetailsVo
import tech.testsys.web.app.service.judge.TestVerdictVo
import tech.testsys.web.app.service.study.GradingResultVo
import tech.testsys.web.app.service.study.SubmissionStatusVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.actions.iconDownloadAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.field
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization

/** Content type of downloaded files, whose real type the system does not know. */
private const val FILE_CONTENT_TYPE = "application/octet-stream"

/**
 * Submission viewed by a Judge (testsys.web.page.judge.solution): its details with the solution file, the scores of the
 * verdict per test with the logs and recordings, the judgment orders and issuing a new one.
 *
 * @since %CURRENT_VERSION%
 */
@Route("judge/submissions/:submissionId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class JudgeSolutionView(
    private val headers: CabinetHeaders,
    private val judgeService: JudgeService,
    private val fileContentReader: FileContentReader,
) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageJudge.submission()

    override fun beforeEnter(event: BeforeEnterEvent) {
        // Loads the submission before the page is built, so that a refusal opens its error screen.
        show(SubmissionId(event.routeParameters.getLong(SUBMISSION_ID_PARAMETER).orElseThrow()))
    }

    private fun show(submissionId: SubmissionId) {
        val details = judgeService.viewSolution(submissionId)
        val (status, tone) = statusOf(details.submission.status)
        val localization = currentLocalization()
        val texts = localization.pageJudge
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.solutionTitle(id = details.submission.id.value)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), JudgeView::class.java)
                crumb(localization.pageCabinet.sectionSubmissions(), JudgeView::class.java, judgeSubmissionsParameters())
                badge(texts.solutionStatus(status = status), tone)
            }
            guide(texts.solutionGuide())
            detailsBlock(details)
            verdictBlock(details)
            judgmentsBlock(details)
        }
    }

    private fun PageScope.detailsBlock(details: SubmissionDetailsVo) {
        val texts = currentLocalization().pageJudge
        val submissionId = details.submission.id
        val (status, tone) = statusOf(details.submission.status)
        row {
            block(title = texts.solutionDetailsTitle()) {
                guide(texts.solutionDetailsGuide())
                row {
                    codeInput(texts.solutionDetailsId(), labelSize = 3, size = 9) { value = submissionId.value.toString() }
                    dateTimeInput(texts.solutionDetailsSent(), labelSize = 3, size = 9) {
                        value = details.submission.createdAt.toUserDateTime()
                    }
                }
                row {
                    textInput(texts.author(), labelSize = 3, size = 9) { value = details.author.name }
                    codeInput(texts.authorId(), labelSize = 3, size = 9) { value = details.author.id.value.toString() }
                }
                row {
                    textInput(texts.solutionDetailsTask(), labelSize = 3, size = 9) { value = details.task.name }
                    textInput(texts.solutionDetailsContest(), labelSize = 3, size = 9) { value = details.contest.name }
                }
                row {
                    field(texts.solutionDetailsFile(), labelSize = 3, size = 9) {
                        downloadAction(details.solution.fileName, produce = { download(judgeService.downloadSolution(submissionId)) })
                    }
                    textInput(texts.solutionDetailsLanguage(), labelSize = 3, size = 9) {
                        value = texts.solutionLanguage(language = languageOf(details.solution.language))
                    }
                }
                row {
                    field(texts.solutionDetailsStatus(), labelSize = 3, size = 9) { badge(texts.solutionStatus(status = status), tone) }
                    codeInput(texts.finalScore(), labelSize = 3, size = 9) { value = details.finalScore?.toString() ?: "—" }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.verdictBlock(details: SubmissionDetailsVo) {
        val texts = currentLocalization().pageJudge
        val submissionId = details.submission.id
        val verdict = details.verdict
        row {
            block(title = texts.solutionVerdictTitle()) {
                guide(texts.solutionVerdictGuide())
                if (verdict == null) {
                    emptyState(texts.solutionVerdictEmpty(), texts.solutionVerdictEmptyHint())
                } else {
                    val outcomes = verdict.testVerdicts.zip(details.tests)
                    table(key = { (_, test): Pair<TestVerdictVo, TestVo> -> test.id }, fetch = { request -> pageOf(outcomes, request) }) {
                        textColumn(texts.solutionVerdictPolygon(), size = 12) { (_, test) -> test.name }
                        numberColumn(texts.solutionScore(), size = 6) { (outcome, _) -> outcome.score.value }
                        column(texts.solutionVerdictFiles()) { (outcome, test) ->
                            iconDownloadAction(
                                texts.solutionVerdictDownloadLogs(name = test.name),
                                produce = { download(judgeService.downloadLogs(submissionId, outcome.test)) },
                            )
                            if (outcome.recording != null) {
                                iconDownloadAction(
                                    texts.solutionVerdictDownloadRecording(name = test.name),
                                    produce = { download(judgeService.downloadRecording(submissionId, outcome.test)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun PageScope.judgmentsBlock(details: SubmissionDetailsVo) {
        val localization = currentLocalization()
        val texts = localization.pageJudge
        val orders = details.judgmentOrders
        row {
            block(title = texts.solutionJudgmentsTitle()) {
                guide(texts.solutionJudgmentsGuide())
                table(
                    key = { (order, _): Pair<JudgmentOrderVo, NamedUserVo> -> order.id },
                    fetch = { request -> pageOf(orders, request) },
                ) {
                    numberColumn(texts.solutionScore(), size = 3) { (order, _) -> order.score.value }
                    textColumn(texts.solutionReason(), size = 11) { (order, _) -> order.reason }
                    textColumn(localization.pageCabinet.roleName(role = RoleNameRole.JUDGE), size = 5) { (_, judge) -> judge.name }
                    dateTimeColumn(texts.solutionJudgmentsIssued()) { (order, _) -> order.createdAt.toUserDateTime() }
                    empty(texts.solutionJudgmentsEmpty())
                }
                if (details.verdict != null) {
                    val issuing = judgmentDialog(details.submission.id)
                    actions { action(texts.solutionJudgmentsIssue()) { onClick { issuing() } } }
                }
            }
        }
    }

    /** Builds the dialog issuing a judgment order for [submissionId] and returns its opening, which starts with an empty form. */
    private fun judgmentDialog(submissionId: SubmissionId): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageJudge
        val draft = Binder<JudgmentDraft>()
        val issuing = dialog(title = texts.solutionDialogTitle()) {
            row {
                integerInput(texts.solutionScore(), labelSize = 6, size = 6, min = 0) {
                    draft.forField(this)
                        .asRequired(texts.solutionDialogScoreRequired())
                        .bind({ values -> values.score }, { values, score -> values.score = score })
                }
            }
            row {
                textArea(texts.solutionReason(), labelSize = 6, size = 18, maxLines = 4) {
                    draft.forField(this)
                        .asRequired(texts.solutionDialogReasonRequired())
                        .withValidator({ reason -> reason.isNotBlank() }, texts.solutionDialogReasonRequired())
                        .bind({ values -> values.reason }, { values, reason -> values.reason = reason })
                }
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.solutionDialogSubmit()) {
                    onClick {
                        val values = JudgmentDraft()
                        if (draft.writeBeanIfValid(values)) {
                            judgeService.changeVerdict(submissionId, Score(checkNotNull(values.score)), values.reason)
                            toast(FeedbackKind.Success, currentLocalization().pageJudge.solutionDialogSaved())
                            dialog.close()
                            show(submissionId)
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(JudgmentDraft())
            issuing.open()
        }
    }

    private fun download(file: FileData): DownloadContent {
        val content = fileContentReader.read(file)
        return DownloadContent(file.uploadedFilename, FILE_CONTENT_TYPE, content.size.toLong()) { content.inputStream() }
    }

    /** Values of the judgment order form. */
    private class JudgmentDraft {
        var score: Int? = null
        var reason: String = ""
    }
}

/** Returns the message variant and tone of the grading [status] of a submission. */
private fun statusOf(status: SubmissionStatusVo): Pair<SolutionStatusStatus, Tone> = when (status) {
    SubmissionStatusVo.Queued -> SolutionStatusStatus.QUEUED to Tone.Neutral
    SubmissionStatusVo.InProgress -> SolutionStatusStatus.IN_PROGRESS to Tone.Info
    is SubmissionStatusVo.Graded -> when (status.grade) {
        is GradingResultVo.Success -> SolutionStatusStatus.SUCCESS to Tone.Success
        is GradingResultVo.GradingError -> SolutionStatusStatus.GRADING_ERROR to Tone.Danger
        GradingResultVo.Timeout -> SolutionStatusStatus.TIMEOUT to Tone.Warning
    }
}

/** Returns the message variant naming the programming [language] of a solution. */
private fun languageOf(language: TrikSupportedLanguage): SolutionLanguageLanguage = when (language) {
    TrikSupportedLanguage.Python -> SolutionLanguageLanguage.PYTHON
    TrikSupportedLanguage.JavaScript -> SolutionLanguageLanguage.JAVASCRIPT
    TrikSupportedLanguage.VisualLanguage -> SolutionLanguageLanguage.VISUAL_LANGUAGE
}
