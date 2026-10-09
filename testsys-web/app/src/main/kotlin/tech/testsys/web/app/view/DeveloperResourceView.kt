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
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.DeveloperSolutionVo
import tech.testsys.web.app.service.developer.ExerciseVo
import tech.testsys.web.app.service.developer.ResourceVo
import tech.testsys.web.app.service.developer.SolutionVo
import tech.testsys.web.app.service.developer.StatementVo
import tech.testsys.web.app.service.developer.TestVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.FileDropHandle
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.texts.UiTexts
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** Limits of the new file of a resource. */
private val FILE_LIMITS =
    UploadLimits(maxFiles = 1, maxFileBytes = MAX_RESOURCE_BYTES, maxMemoryBytes = MAX_RESOURCE_BYTES.toLong())

/** Order of the versions of a resource from the latest one: by creation time, then by identifier, as the system orders them. */
private val NEWEST_FIRST: Comparator<Pair<ResourceVo, SolutionVo?>> =
    compareBy<Pair<ResourceVo, SolutionVo?>>({ (version, _) -> version.createdAt }, { (version, _) -> version.id.value }).reversed()

/** Content type of a downloaded resource file, whose real type the system does not check. */
private const val FILE_CONTENT_TYPE = "application/octet-stream"

/**
 * Resource of a task of a Developer (testsys.web.page.developer.resource): its details with their editing and the history
 * of its versions with downloading their files. Before changing an attached resource the page warns about the new
 * uncommitted changes of the task.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/tasks/:taskId([0-9]+)/resources/:resourceId([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
@PageTitle("Ресурс")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperResourceView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val fileContentReader: FileContentReader,
) : TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val parameters = event.routeParameters
        val taskId = TaskId(parameters.getLong(TASK_ID_PARAMETER).orElseThrow())
        show(taskId, VersionBucket(UUID.fromString(parameters.get(RESOURCE_ID_PARAMETER).orElseThrow())))
    }

    private fun show(taskId: TaskId, versionBucket: VersionBucket) {
        // Checks the role, the task, its owner and the chain before the task, so that a refusal opens its error screen.
        val history = developerService.viewResource(taskId, versionBucket).sortedWith(NEWEST_FIRST)
        val (task, _) = developerService.viewTask(taskId)
        val latest = history.first().first
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("${typeOf(latest)} «${latest.name}»") {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Разработчика", DeveloperView::class.java)
                crumb("Задачи", DeveloperView::class.java, developerSectionParameters(TASKS_SECTION))
                crumb("Задача «${task.name}»", DeveloperTaskView::class.java, taskParameters(taskId))
            }
            detailsBlock(task, history)
            historyBlock(taskId, history)
        }
    }

    private fun PageScope.detailsBlock(task: TaskVo, history: List<Pair<ResourceVo, SolutionVo?>>) {
        val (latest, solution) = history.first()
        val draft = Binder<ResourceDraft>()
        val initial = ResourceDraft(latest.name, (latest as? DeveloperSolutionVo)?.expectedScore?.value)
        val newFile = AtomicReference<FileData?>()
        lateinit var drop: FileDropHandle
        row {
            block(title = "Сведения") {
                editing(
                    onSave = {
                        val values = ResourceDraft()
                        draft.writeBeanIfValid(values) && save(task, history, values, newFile.get())
                    },
                    onCancel = {
                        draft.readBean(initial)
                        newFile.set(null)
                        drop.clear()
                    },
                )
                row {
                    codeInput("ID", labelSize = 6, size = 18) {
                        value = latest.versionBucket.value.toString()
                        isEditable = false
                    }
                }
                row {
                    textInput("Тип", labelSize = 6, size = 18) {
                        value = typeOf(latest)
                        isEditable = false
                    }
                }
                row {
                    textInput("Название", labelSize = 6, size = 18) {
                        draft.forField(this)
                            .nameRules("Укажите название")
                            .bind({ values -> values.name }, { values, name -> values.name = name })
                    }
                }
                languageOf(latest, solution)?.let { language ->
                    row {
                        textInput("Язык", labelSize = 6, size = 18) {
                            value = language
                            isEditable = false
                        }
                    }
                }
                row {
                    textInput("Файл", labelSize = 6, size = 18) {
                        value = fileNameOf(latest, solution)
                        isEditable = false
                    }
                }
                row {
                    drop = fileDrop("Новый файл", FILE_LIMITS, consume = { file -> newFile.set(file.toFileData()) })
                }
                if (latest is DeveloperSolutionVo) {
                    row {
                        integerInput("Ожидаемый балл", labelSize = 6, size = 18, min = 0) {
                            draft.forField(this)
                                .asRequired("Укажите балл")
                                .bind({ values -> values.score }, { values, score -> values.score = score })
                        }
                    }
                }
            }
        }

        draft.readBean(initial)
    }

    private fun PageScope.historyBlock(taskId: TaskId, history: List<Pair<ResourceVo, SolutionVo?>>) {
        val isSolution = history.first().first is DeveloperSolutionVo
        row {
            block(title = "История версий") {
                table(
                    key = { (version, _): Pair<ResourceVo, SolutionVo?> -> version.id },
                    fetch = { request -> pageOf(history, request) },
                ) {
                    dateTimeColumn("Изменено", size = 5) { (version, _) -> version.createdAt.toServerDateTime() }
                    textColumn("Название", size = if (isSolution) 6 else 8) { (version, _) -> version.name }
                    textColumn("Файл", size = if (isSolution) 6 else 7) { (version, solution) -> fileNameOf(version, solution) }
                    if (isSolution) {
                        numberColumn("Балл", size = 3) { (version, _) -> (version as? DeveloperSolutionVo)?.expectedScore?.value }
                    }
                    column("Скачать") { (version, _) ->
                        downloadAction("Скачать", produce = { _ ->
                            val file = developerService.downloadResourceVersion(taskId, version.versionBucket, version.id)
                            val bytes = fileContentReader.read(file)
                            DownloadContent(
                                filename = file.uploadedFilename,
                                contentType = FILE_CONTENT_TYPE,
                                length = bytes.size.toLong(),
                            ) { ByteArrayInputStream(bytes) }
                        })
                    }
                }
            }
        }
    }

    /**
     * Saves the edited [values] of the latest version of [history] with [file] if given; if that creates a version of a
     * resource attached to [task], it asks to confirm the uncommitted changes first. Returns whether it saved without asking.
     */
    private fun save(task: TaskVo, history: List<Pair<ResourceVo, SolutionVo?>>, values: ResourceDraft, file: FileData?): Boolean {
        val latest = history.first().first
        val isNewVersion = file != null || (latest is DeveloperSolutionVo && values.score != latest.expectedScore.value)
        val ids = history.map { (version, _) -> version.id }.toSet()
        val isInWip = idsOf(task.wip).any { id -> id in ids }
        val isOnlyCommitted = task.wip == null && idsOf(task.lastCommitted).any { id -> id in ids }
        val saving = {
            update(task.id, latest, values, file)
            show(task.id, latest.versionBucket)
        }
        if (!isNewVersion || !(isInWip || isOnlyCommitted)) {
            saving()
            return true
        }

        val state = if (isOnlyCommitted) " Задача перейдёт в состояние «${TaskState.Uncommitted.label}»." else ""
        confirm(
            title = "Изменить прикреплённый ресурс?",
            text = "Появится новая версия ресурса, и у задачи появятся незафиксированные изменения.$state",
            action = "Изменить",
            onConfirm = saving,
        )
        return false
    }

    private fun update(taskId: TaskId, latest: ResourceVo, values: ResourceDraft, file: FileData?) {
        when (latest) {
            is StatementVo -> developerService.updateStatement(taskId, latest.id, values.name, file)
            is ExerciseVo -> developerService.updateExercise(taskId, latest.id, values.name, file)
            is TestVo -> developerService.updateTest(taskId, latest.id, values.name, file)
            is DeveloperSolutionVo -> developerService.updateDeveloperSolution(
                taskId = taskId,
                developerSolutionId = latest.id,
                resourceName = values.name,
                file = file,
                expectedScore = values.score?.takeIf { score -> score != latest.expectedScore.value }?.let(::Score),
            )
        }
    }

    /** Values of the resource editing form. */
    private class ResourceDraft(var name: String = "", var score: Int? = null)
}

/** Returns the uploaded file name of [version], the file of its [solution] for a developer solution. */
private fun fileNameOf(version: ResourceVo, solution: SolutionVo?): String = when (version) {
    is StatementVo -> version.fileName
    is ExerciseVo -> version.fileName
    is TestVo -> version.fileName
    is DeveloperSolutionVo -> solution?.fileName.orEmpty()
}

/** Returns the language label of [version], of its [solution] for a developer solution, or `null` for a resource without one. */
private fun languageOf(version: ResourceVo, solution: SolutionVo?): String? = when (version) {
    is ExerciseVo -> LANGUAGE_LABELS.getValue(version.language)
    is DeveloperSolutionVo -> solution?.let { program -> LANGUAGE_LABELS.getValue(program.language) }
    is StatementVo, is TestVo -> null
}
