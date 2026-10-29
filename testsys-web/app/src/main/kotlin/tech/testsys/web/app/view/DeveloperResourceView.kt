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
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.localization.PageDeveloper.ResourceTitleType
import tech.testsys.web.app.service.TaskRevisionVo
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
import tech.testsys.web.components.actions.iconDownloadAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.FileDropHandle
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.texts.currentLocalization
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
 * @since 1.0.0-beta.1
 */
@Route("developer/tasks/:taskId([0-9]+)/resources/:resourceId([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperResourceView(
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val fileContentReader: FileContentReader,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageDeveloper.resourceTabTitle()

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
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.resourceTitle(type = titleTypeOf(kindOf(latest)), name = latest.name)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), DeveloperView::class.java)
                crumb(localization.pageCabinet.sectionTasks(), DeveloperView::class.java, developerSectionParameters(TASKS_SECTION))
                crumb(texts.taskTitle(name = task.name), DeveloperTaskView::class.java, taskParameters(taskId))
            }
            guide(texts.resourceGuide())
            detailsBlock(task, history)
            historyBlock(task, history)
        }
    }

    private fun PageScope.detailsBlock(task: TaskVo, history: List<Pair<ResourceVo, SolutionVo?>>) {
        val texts = currentLocalization().pageDeveloper
        val (latest, solution) = history.first()
        val draft = Binder<ResourceDraft>()
        val initial = ResourceDraft(latest.name, (latest as? DeveloperSolutionVo)?.expectedScore?.value)
        val fileCopy = AtomicReference<Pair<String, FileData>?>()
        lateinit var drop: FileDropHandle
        row {
            block(title = texts.details()) {
                guide(texts.editingGuide())
                editing(
                    onSave = {
                        val values = ResourceDraft()
                        val file = fileCopy.get()?.takeIf { (id, _) -> id in drop.fileIds }?.second
                        draft.writeBeanIfValid(values) && save(task, history, values, file)
                    },
                    onCancel = {
                        draft.readBean(initial)
                        fileCopy.set(null)
                        drop.clear()
                    },
                )
                row {
                    codeInput(texts.id(), labelSize = 4, size = 8) {
                        value = latest.versionBucket.value.toString()
                        isEditable = false
                    }
                    textInput(texts.type(), labelSize = 4, size = 8) {
                        value = typeOf(latest)
                        isEditable = false
                    }
                }
                row {
                    codeInput(texts.resourceLatestId(), labelSize = 4, size = 8) {
                        value = latest.id.value.toString()
                        isEditable = false
                    }
                }
                row {
                    codeInput(texts.resourceWorkingId(), labelSize = 4, size = 8) {
                        value = attachedVersionId(history, task.wip, texts.resourceNoWorking())
                        isEditable = false
                    }
                    codeInput(texts.resourceCommittedId(), labelSize = 4, size = 8) {
                        value = attachedVersionId(history, task.lastCommitted, texts.resourceNotCommitted())
                        isEditable = false
                    }
                }
                row {
                    textInput(texts.name(), labelSize = 4, size = 8) {
                        draft.forField(this)
                            .nameRules(texts.nameRequired())
                            .bind({ values -> values.name }, { values, name -> values.name = name })
                    }
                    languageOf(latest, solution)?.let { language ->
                        textInput(texts.language(), labelSize = 4, size = 8) {
                            value = language
                            isEditable = false
                        }
                    }
                }
                row {
                    textInput(texts.file(), labelSize = 4, size = 8) {
                        value = fileNameOf(latest, solution)
                        isEditable = false
                    }
                    drop = fileDrop(
                        texts.resourceNewFile(),
                        FILE_LIMITS,
                        labelSize = 4,
                        size = 8,
                        consume = { file -> fileCopy.set(file.id to file.toFileData()) },
                    )
                }
                if (latest is DeveloperSolutionVo) {
                    row {
                        integerInput(texts.expectedScore(), labelSize = 4, size = 8, min = 0) {
                            draft.forField(this)
                                .asRequired(texts.scoreRequired())
                                .bind({ values -> values.score }, { values, score -> values.score = score })
                        }
                    }
                }
            }
        }

        draft.readBean(initial)
    }

    private fun PageScope.historyBlock(task: TaskVo, history: List<Pair<ResourceVo, SolutionVo?>>) {
        val texts = currentLocalization().pageDeveloper
        val isSolution = history.first().first is DeveloperSolutionVo
        row {
            block(title = texts.resourceHistoryTitle()) {
                guide(texts.resourceHistoryGuide())
                table(
                    key = { (version, _): Pair<ResourceVo, SolutionVo?> -> version.id },
                    fetch = { request -> pageOf(history, request) },
                ) {
                    codeColumn(texts.versionId(), size = 4) { (version, _) -> version.id.value.toString() }
                    dateTimeColumn(texts.resourceHistoryChangedAt(), size = 3) { (version, _) -> version.createdAt.toUserDateTime() }
                    textColumn(texts.name(), size = if (isSolution) 4 else 5) { (version, _) -> version.name }
                    textColumn(texts.file(), size = if (isSolution) 4 else 6) { (version, solution) -> fileNameOf(version, solution) }
                    if (isSolution) {
                        numberColumn(texts.score(), size = 3) { (version, _) -> (version as? DeveloperSolutionVo)?.expectedScore?.value }
                    }
                    column(texts.attached(), size = 5) { (version, _) ->
                        if (version.id in idsOf(task.lastCommitted)) badge(texts.attachedCommitted(), tone = Tone.Success)
                        if (version.id in idsOf(task.wip)) badge(texts.attachedWorking(), tone = Tone.Warning)
                    }
                    column("") { (version, _) ->
                        iconDownloadAction(label = texts.download(), produce = { _ ->
                            val file = developerService.downloadResourceVersion(task.id, version.versionBucket, version.id)
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
            toast(FeedbackKind.Success, currentLocalization().pageDeveloper.resourceEdited())
            show(task.id, latest.versionBucket)
        }
        if (!isNewVersion || !(isInWip || isOnlyCommitted)) {
            saving()
            return true
        }

        val texts = currentLocalization().pageDeveloper
        confirm(
            title = texts.resourceChangeTitle(),
            text = if (isOnlyCommitted) texts.resourceChangeTextUncommitted() else texts.resourceChangeText(),
            action = texts.resourceChangeAction(),
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

/** Returns the ID from [history] attached to [revision], distinguishing a missing revision by [absentLabel]. */
private fun attachedVersionId(history: List<Pair<ResourceVo, SolutionVo?>>, revision: TaskRevisionVo?, absentLabel: String): String {
    if (revision == null) return absentLabel

    val ids = idsOf(revision)
    return history.firstOrNull { (version, _) -> version.id in ids }?.first?.id?.value?.toString()
        ?: currentLocalization().pageDeveloper.resourceNotAttached()
}

/** Returns the message variant of the page heading of a resource of [kind]. */
private fun titleTypeOf(kind: ResourceKind): ResourceTitleType = when (kind) {
    ResourceKind.Statement -> ResourceTitleType.STATEMENT
    ResourceKind.Exercise -> ResourceTitleType.EXERCISE
    ResourceKind.Test -> ResourceTitleType.TEST
    ResourceKind.DeveloperSolution -> ResourceTitleType.DEVELOPER_SOLUTION
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
    is ExerciseVo -> languageLabel(version.language)
    is DeveloperSolutionVo -> solution?.let { program -> languageLabel(program.language) }
    is StatementVo, is TestVo -> null
}
