package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TrikSupportedLanguage
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
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts

/** Content type of downloaded files, whose real type the system does not know. */
private const val FILE_CONTENT_TYPE = "application/octet-stream"

/**
 * Submission viewed by a Judge (testsys.web.page.judge.solution): its details with the solution file, the scores of the
 * verdict per test with the logs and recordings, the judgment orders and issuing a new one.
 *
 * @since %CURRENT_VERSION%
 */
@Route("judge/submissions/:submissionId([0-9]+)")
@PageTitle("Посылка")
@RolesAllowed("MULTIPLE_ROLE")
class JudgeSolutionView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val judgeService: JudgeService,
    private val fileContentReader: FileContentReader,
) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        // Loads the submission before the page is built, so that a refusal opens its error screen.
        show(SubmissionId(event.routeParameters.getLong(SUBMISSION_ID_PARAMETER).orElseThrow()))
    }

    private fun show(submissionId: SubmissionId) {
        val details = judgeService.viewSolution(submissionId)
        val (status, tone) = statusOf(details.submission.status)
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Посылка") {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Судьи", JudgeView::class.java)
                crumb("Посылки", JudgeView::class.java, judgeSubmissionsParameters())
                badge(status, tone)
                meta("${details.author.name} · Задача «${details.task.name}»")
            }
            detailsBlock(details)
            verdictBlock(details)
            judgmentsBlock(details)
        }
    }

    private fun PageScope.detailsBlock(details: SubmissionDetailsVo) {
        val submissionId = details.submission.id
        val (status, tone) = statusOf(details.submission.status)
        row {
            block(title = "Сведения") {
                row {
                    codeInput("ID", labelSize = 3, size = 9) { value = submissionId.value.toString() }
                    dateTimeInput("Отправлена", labelSize = 3, size = 9) { value = details.submission.createdAt.toServerDateTime() }
                }
                row {
                    textInput("Автор", labelSize = 3, size = 9) { value = details.author.name }
                    codeInput("ID автора", labelSize = 3, size = 9) { value = details.author.id.value.toString() }
                }
                row {
                    textInput("Задача", labelSize = 3, size = 9) { value = details.task.name }
                    textInput("Тур", labelSize = 3, size = 9) { value = details.contest.name }
                }
                row {
                    field("Файл", labelSize = 3, size = 9) {
                        downloadAction(details.solution.fileName, produce = { download(judgeService.downloadSolution(submissionId)) })
                    }
                    textInput("Язык", labelSize = 3, size = 9) { value = languageOf(details.solution.language) }
                }
                row {
                    field("Статус проверки", labelSize = 3, size = 9) { badge(status, tone) }
                    codeInput("Итоговый балл", labelSize = 3, size = 9) { value = details.finalScore?.toString() ?: "—" }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.verdictBlock(details: SubmissionDetailsVo) {
        val submissionId = details.submission.id
        val verdict = details.verdict
        row {
            block(title = "Вердикт") {
                if (verdict == null) {
                    emptyState("Вердикта нет", "Баллы по полигонам появятся после успешной проверки.")
                } else {
                    val outcomes = verdict.testVerdicts.zip(details.tests)
                    table(key = { (_, test): Pair<TestVerdictVo, TestVo> -> test.id }, fetch = { request -> pageOf(outcomes, request) }) {
                        textColumn("Полигон", size = 12) { (_, test) -> test.name }
                        numberColumn("Балл", size = 6) { (outcome, _) -> outcome.score.value }
                        column("Файлы") { (outcome, test) ->
                            iconDownloadAction(
                                "Скачать логи полигона «${test.name}»",
                                produce = { download(judgeService.downloadLogs(submissionId, outcome.test)) },
                            )
                            if (outcome.recording != null) {
                                iconDownloadAction(
                                    "Скачать видеозапись полигона «${test.name}»",
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
        val orders = details.judgmentOrders
        row {
            block(title = "Судейские вердикты") {
                table(
                    key = { (order, _): Pair<JudgmentOrderVo, NamedUserVo> -> order.id },
                    fetch = { request -> pageOf(orders, request) },
                ) {
                    numberColumn("Балл", size = 3) { (order, _) -> order.score.value }
                    textColumn("Обоснование", size = 11) { (order, _) -> order.reason }
                    textColumn("Судья", size = 5) { (_, judge) -> judge.name }
                    dateTimeColumn("Выставлен") { (order, _) -> order.createdAt.toServerDateTime() }
                    empty("Судейских вердиктов нет")
                }
                if (details.verdict != null) {
                    val issuing = judgmentDialog(details.submission.id)
                    actions { action("Выставить вердикт") { onClick { issuing() } } }
                }
            }
        }
    }

    /** Builds the dialog issuing a judgment order for [submissionId] and returns its opening, which starts with an empty form. */
    private fun judgmentDialog(submissionId: SubmissionId): () -> Unit {
        val draft = Binder<JudgmentDraft>()
        val issuing = dialog(title = "Новый судейский вердикт") {
            row {
                integerInput("Балл", labelSize = 6, size = 18, min = 0) {
                    draft.forField(this)
                        .asRequired("Укажите балл")
                        .bind({ values -> values.score }, { values, score -> values.score = score })
                }
            }
            row {
                textArea("Обоснование", labelSize = 6, size = 18) {
                    draft.forField(this)
                        .asRequired("Укажите обоснование")
                        .withValidator({ reason -> reason.isNotBlank() }, "Укажите обоснование")
                        .bind({ values -> values.reason }, { values, reason -> values.reason = reason })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Выставить") {
                    onClick {
                        val values = JudgmentDraft()
                        if (draft.writeBeanIfValid(values)) {
                            judgeService.changeVerdict(submissionId, Score(checkNotNull(values.score)), values.reason)
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

/** Returns the text and tone of the grading [status] of a submission. */
private fun statusOf(status: SubmissionStatusVo): Pair<String, Tone> = when (status) {
    SubmissionStatusVo.Queued -> "В очереди" to Tone.Neutral
    SubmissionStatusVo.InProgress -> "Проверяется" to Tone.Info
    is SubmissionStatusVo.Graded -> when (status.grade) {
        is GradingResultVo.Success -> "Проверена" to Tone.Success
        is GradingResultVo.GradingError -> "Ошибка проверки" to Tone.Danger
        GradingResultVo.Timeout -> "Превышено время проверки" to Tone.Warning
    }
}

/** Returns the name of the programming [language] of a solution. */
private fun languageOf(language: TrikSupportedLanguage): String = when (language) {
    TrikSupportedLanguage.Python -> "Python"
    TrikSupportedLanguage.JavaScript -> "JavaScript"
    TrikSupportedLanguage.VisualLanguage -> "Визуальный язык"
}
