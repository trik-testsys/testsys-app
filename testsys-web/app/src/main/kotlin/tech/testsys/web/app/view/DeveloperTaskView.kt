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
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.infra.localization.PageDeveloper.ResourceTypeType
import tech.testsys.infra.localization.PageDeveloper.TaskResourcesUsageValueUsage
import tech.testsys.infra.localization.PageDeveloper.TaskStateChangeTitleState
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.TaskRevisionVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.DeveloperSolutionVo
import tech.testsys.web.app.service.developer.ExerciseVo
import tech.testsys.web.app.service.developer.ResourceVo
import tech.testsys.web.app.service.developer.StatementVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.app.service.developer.TestVo
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.FileDropHandle
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.checkbox
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.lookupMany
import tech.testsys.web.components.forms.multiSelect
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.skipWhenHidden
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.DialogSectionScope
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization
import java.util.concurrent.ConcurrentHashMap

/** Columns of the task details block; the access block takes the rest of the row beside it. */
private const val DETAILS_COLUMNS = 14

/** Bytes in a mebibyte, the unit of the upload limit a page shows. */
private const val BYTES_PER_MEBIBYTE = 1024 * 1024

/** Limits of one upload of resource files. */
private val UPLOAD_LIMITS =
    UploadLimits(maxFiles = 20, maxFileBytes = MAX_RESOURCE_BYTES, maxMemoryBytes = 5L * MAX_RESOURCE_BYTES)

/**
 * Type of an uploaded resource.
 *
 * @property type the message variant naming the type.
 * @property hasLanguage whether the resource has a language.
 * @property hasScore whether the resource has an expected score.
 */
internal enum class ResourceKind(val type: ResourceTypeType, val hasLanguage: Boolean, val hasScore: Boolean) {
    /** Statement of the task. */
    Statement(ResourceTypeType.STATEMENT, hasLanguage = false, hasScore = false),

    /** Exercise of a language. */
    Exercise(ResourceTypeType.EXERCISE, hasLanguage = true, hasScore = false),

    /** Polygon. */
    Test(ResourceTypeType.TEST, hasLanguage = false, hasScore = false),

    /** Developer solution with its expected score. */
    DeveloperSolution(ResourceTypeType.DEVELOPER_SOLUTION, hasLanguage = true, hasScore = true),
}

/**
 * Revisions in which a resource chain is used.
 *
 * @property usage the message variant naming the revisions.
 */
internal enum class ResourceUsage(val usage: TaskResourcesUsageValueUsage) {
    All(TaskResourcesUsageValueUsage.ALL),
    Working(TaskResourcesUsageValueUsage.WORKING),
    Committed(TaskResourcesUsageValueUsage.COMMITTED),
    AnyRevision(TaskResourcesUsageValueUsage.ANY_REVISION),
    Unattached(TaskResourcesUsageValueUsage.UNATTACHED),
}

/**
 * Task of a Developer (testsys.web.page.developer.task): its details with their editing, its resources with uploading,
 * attaching and detaching, its testing with committing and reverting, and its access of communities. Before an action
 * that changes the state of the task, the page asks to confirm the new state.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/tasks/:taskId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperTaskView(
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val multipleRoleUserService: MultipleRoleUserService,
    private val fileContentReader: FileContentReader,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageDeveloper.taskTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        show(TaskId(event.routeParameters.getLong(TASK_ID_PARAMETER).orElseThrow()))
    }

    /**
     * Opens the dialog that describes each of [files] uploaded to [taskId]: its type, name, language and expected score;
     * saving it uploads the files one by one and shows the task again.
     */
    internal fun describeUploads(taskId: TaskId, files: List<FileData>) {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val uploads = mutableListOf<Pair<FileData, Binder<UploadDraft>>>()
        val describing = dialog(
            title = texts.taskDescribeTitle(),
            subtitle = texts.taskDescribeSubtitle(),
            size = DialogSize.L,
        ) {
            files.forEachIndexed { index, file ->
                section(texts.taskDescribeSection(number = index + 1, fileName = file.uploadedFilename)) {
                    uploads.add(file to uploadRows(file))
                }
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.taskDescribeSubmit()) {
                    onClick {
                        val drafts = uploads.map { (file, binder) ->
                            file to UploadDraft().takeIf { values -> binder.writeBeanIfValid(values) }
                        }
                        if (drafts.all { (_, values) -> values != null }) {
                            dialog.close()
                            try {
                                drafts.forEach { (file, values) -> upload(taskId, file, checkNotNull(values)) }
                                val current = currentLocalization().pageDeveloper
                                toast(FeedbackKind.Success, current.taskDescribeDone(), current.taskTotal(count = drafts.size))
                            } finally {
                                show(taskId)
                            }
                        }
                    }
                }
            }
        }
        describing.open()
    }

    private fun show(taskId: TaskId) {
        val (task, shared) = developerService.viewTask(taskId)
        val versions = developerService.viewTrikStudioVersions()
        val chains = chainsOf(task)
        val requests = developerService.viewTaskValidationRequests(taskId).reversed()
        val state = stateOf(task)
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.taskTitle(name = task.name)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), DeveloperView::class.java)
                crumb(localization.pageCabinet.sectionTasks(), DeveloperView::class.java, developerSectionParameters(TASKS_SECTION))
                badge(texts.taskState(state = state.variant), state.tone)
                actions {
                    action(texts.taskViewCommitted()) {
                        isEnabled = task.lastCommitted != null
                        onClick { committedDialog(task, chains) }
                    }
                }
            }
            guide(texts.taskGuide())
            row {
                detailsBlock(task, versions)
                accessBlock(task, shared)
            }
            resourcesBlock(task, chains)
            testingBlock(task, requests)
        }
    }

    private fun committedDialog(task: TaskVo, chains: List<Chain>) {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val committed = checkNotNull(task.lastCommitted)
        dialog(title = texts.taskCommittedTitle(), size = DialogSize.L) {
            row {
                textInput(texts.trikStudioVersions(), labelSize = 6, size = 18) {
                    value = committed.supportedTrikStudioVersions.joinToString(", ") { version -> version.version }
                    isEditable = false
                }
            }
            chains.mapNotNull { chain -> chain.attachedIn(committed) }.forEach { resource ->
                section(typeOf(resource)) {
                    row {
                        textInput(texts.name(), labelSize = 6, size = 18) {
                            value = resource.name
                            isEditable = false
                        }
                    }
                    row {
                        codeInput(texts.versionId(), labelSize = 6, size = 18) {
                            value = resource.id.value.toString()
                            isEditable = false
                        }
                    }
                    row {
                        downloadAction(texts.download(), produce = {
                            val file = developerService.downloadResourceVersion(task.id, resource.versionBucket, resource.id)
                            val bytes = fileContentReader.read(file)
                            DownloadContent(file.uploadedFilename, "application/octet-stream", bytes.size.toLong()) { bytes.inputStream() }
                        })
                    }
                }
            }
            footer { view -> action(localization.uiOverlay.dialogClose()) { onClick { view.close() } } }
        }.open()
    }

    /**
     * Returns the chains of the resources uploaded to [task] with their latest versions. Every version of the chains is
     * loaded only when a revision of the task refers to a version that is not the latest one.
     */
    private fun chainsOf(task: TaskVo): List<Chain> {
        val latest = developerService.viewResources().filter { resource -> resource.versionBucket in task.uploadedResources }
        val known = latest.map { resource -> resource.id }.toSet()
        val hasOlder = (idsOf(task.wip) + idsOf(task.lastCommitted)).any { id -> id !in known }
        // ponytail: loads the history of every chain with its files when an older version is attached; a query of the
        // attached versions if tasks grow large.
        return latest.sortedWith(compareBy({ resource -> typeOf(resource) }, { resource -> resource.name })).map { resource ->
            val versions = if (hasOlder) {
                developerService.viewResource(task.id, resource.versionBucket).map { (version, _) -> version }
            } else {
                listOf(resource)
            }
            Chain(resource, versions)
        }
    }

    private fun PageRowScope.detailsBlock(task: TaskVo, versions: List<TrikStudioVersion>) {
        val texts = currentLocalization().pageDeveloper
        val draft = Binder<TaskDraft>()
        val supported = (task.wip ?: task.lastCommitted)?.supportedTrikStudioVersions.orEmpty().toSet()
        val initial = TaskDraft(task.name, task.description, supported)
        block(size = DETAILS_COLUMNS, title = texts.details()) {
            guide(texts.editingGuide())
            editing(
                onSave = {
                    val values = TaskDraft()
                    draft.writeBeanIfValid(values) && saveDetails(task, values, supported)
                },
                onCancel = { draft.readBean(initial) },
            )
            row {
                codeInput(texts.id(), labelSize = 6, size = 18) {
                    value = task.id.value.toString()
                    isEditable = false
                }
            }
            row {
                textInput(texts.name(), labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules(texts.nameRequired())
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                textArea(texts.description(), labelSize = 6, size = 18, maxLines = 4) {
                    draft.forField(this)
                        .bind({ values -> values.description }, { values, description -> values.description = description })
                }
            }
            row {
                multiSelect(
                    texts.trikStudioVersions(),
                    labelSize = 6,
                    size = 18,
                    items = versions,
                    itemLabel = TrikStudioVersion::version,
                ) {
                    draft.forField(this).bind({ values -> values.versions }, { values, chosen -> values.versions = chosen })
                }
            }
        }

        draft.readBean(initial)
    }

    /**
     * Saves the edited [values] of [task] whose editable revision supports [supported] versions, after confirming the new
     * state if the versions change it; returns whether it saved without asking.
     */
    private fun saveDetails(task: TaskVo, values: TaskDraft, supported: Set<TrikStudioVersion>): Boolean {
        val hasVersionChanges = values.versions != supported
        return changeState(task, next = if (hasVersionChanges) TaskState.Uncommitted else null) {
            developerService.editTaskInfo(
                taskId = task.id,
                taskName = values.name,
                taskDescription = values.description,
                supportedTrikStudioVersions = values.versions.toList().takeIf { hasVersionChanges },
            )
            toast(FeedbackKind.Success, currentLocalization().pageDeveloper.taskEdited())
            show(task.id)
        }
    }

    private fun PageRowScope.accessBlock(task: TaskVo, shared: List<CommunityVo>) {
        val texts = currentLocalization().pageDeveloper
        block(title = texts.access()) {
            guide(texts.taskAccessGuide())
            table(key = { community: CommunityVo -> community.id }, fetch = { request -> pageOf(shared, request) }) {
                textColumn(texts.community()) { community -> community.name }
                empty(texts.accessEmpty(), texts.taskAccessEmptyHint())
            }
            val sharing = shareDialog(task, shared)
            actions {
                action(texts.share()) {
                    isEnabled = task.lastCommitted != null
                    onClick { sharing() }
                }
            }
        }
    }

    private fun PageScope.resourcesBlock(task: TaskVo, chains: List<Chain>) {
        val texts = currentLocalization().pageDeveloper
        val editable = task.wip ?: task.lastCommitted
        row {
            block(title = texts.taskResourcesTitle(), subtitle = texts.taskResourcesSubtitle()) {
                guide(texts.taskResourcesGuide())
                var resourceFilter = ResourceFilter()
                val rows = table(
                    key = { chain: Chain -> chain.latest.versionBucket },
                    fetch = { request -> pageOf(chains.filter { chain -> resourceFilter.matches(chain, task) }, request) },
                ) {
                    textColumn(texts.name(), size = 6) { chain -> chain.latest.name }
                    textColumn(texts.type(), size = 3) { chain -> typeOf(chain.latest) }
                    column(texts.attached()) { chain ->
                        if (chain.attachedIn(task.lastCommitted) != null) badge(texts.attachedCommitted(), tone = Tone.Success)
                        if (chain.attachedIn(task.wip) != null) badge(texts.attachedWorking(), tone = Tone.Warning)
                    }
                    menuColumn(ariaLabel = { chain -> texts.taskResourcesActions(name = chain.latest.name) }) { chain ->
                        val attached = chain.attachedIn(editable)
                        item(texts.detach(), isEnabled = attached != null) {
                            changeState(task, next = TaskState.Uncommitted) {
                                detach(task.id, checkNotNull(attached))
                                toast(FeedbackKind.Success, currentLocalization().pageDeveloper.taskResourcesDetached())
                                show(task.id)
                            }
                        }
                    }
                    if (chains.isEmpty()) {
                        empty(texts.taskResourcesEmpty(), texts.taskResourcesEmptyHint())
                    } else {
                        empty(texts.taskResourcesNotFound(), texts.taskResourcesNotFoundHint())
                    }
                    onRowClick(isNavigation = true) { chain -> openDeveloperResource(task.id, chain.latest.versionBucket) }
                }
                resourceFilters(task, apply = { applied -> resourceFilter = applied }) { rows.refresh(toFirstPage = true) }
                val uploading = uploadDialog(task.id)
                val attaching = attachDialog(task, chains.filter { chain -> chain.attachedIn(editable) == null })
                actions {
                    action(texts.taskResourcesUpload()) { onClick { uploading() } }
                    action(texts.attach()) { onClick { attaching() } }
                }
            }
        }
    }

    private fun BlockScope.resourceFilters(task: TaskVo, apply: (ResourceFilter) -> Unit, refresh: () -> Unit) {
        val texts = currentLocalization().pageDeveloper
        val isComparisonAvailable = task.wip != null && task.lastCommitted != null
        val comparisonHint = if (isComparisonAvailable) texts.taskResourcesDifferencesHint() else texts.taskResourcesComparisonUnavailable()
        lateinit var name: ValueInput<String>
        lateinit var kind: ValueInput<ResourceKind?>
        lateinit var usage: ValueInput<ResourceUsage?>
        lateinit var differences: ValueInput<Boolean>
        filters(
            onApply = {
                apply(ResourceFilter(name.value, kind.value, usage.value ?: ResourceUsage.All, differences.value))
                true
            },
            onReset = {
                apply(ResourceFilter())
                name.value = ""
                kind.value = null
                usage.value = ResourceUsage.All
                differences.value = false
            },
            onRefresh = refresh,
        ) {
            row {
                name = textInput(texts.name(), labelSize = 4, size = 8, hint = texts.nameHint())
                kind = select(texts.type(), labelSize = 4, size = 8, items = ResourceKind.entries, itemLabel = ::kindLabelOf)
            }
            row {
                usage = select(
                    texts.taskResourcesUsage(),
                    labelSize = 4,
                    size = 8,
                    items = ResourceUsage.entries,
                    itemLabel = { used -> texts.taskResourcesUsageValue(usage = used.usage) },
                ) {
                    value = ResourceUsage.All
                }
                differences = checkbox(
                    texts.taskResourcesDifferences(),
                    labelSize = 4,
                    size = 8,
                ) {
                    isEnabled = isComparisonAvailable
                }
            }
            row { text(comparisonHint) }
        }
    }

    private fun PageScope.testingBlock(task: TaskVo, requests: List<TaskValidationRequestVo>) {
        val texts = currentLocalization().pageDeveloper
        row {
            block(title = texts.taskTestingTitle()) {
                guide(texts.taskTestingGuide())
                table(key = { request: TaskValidationRequestVo -> request.id }, fetch = { request -> pageOf(requests, request) }) {
                    dateTimeColumn(texts.startedAt(), size = 6) { request -> request.createdAt.toUserDateTime() }
                    column(texts.status(), size = 5) { request ->
                        val (label, tone) = validationStatusOf(request.execution)
                        badge(label, tone)
                    }
                    textColumn(texts.summary()) { request -> summaryOf(request.execution) }
                    empty(texts.taskTestingEmpty(), texts.taskTestingEmptyHint())
                    onRowClick(isNavigation = true) { request -> openDeveloperTesting(task.id, request.id) }
                }
                val committing = commitDialog(task)
                actions {
                    action(texts.taskTestingRun()) {
                        isEnabled = task.wip != null
                        onClick {
                            val current = currentLocalization().pageDeveloper
                            refusable(current.taskTestingRunFailed()) {
                                val request = developerService.testTask(task.id)
                                if (requests.any { existing -> existing.id == request.id }) {
                                    toast(FeedbackKind.Info, current.taskTestingRunning(), current.taskTestingRunningHint())
                                } else {
                                    toast(FeedbackKind.Success, current.taskTestingStarted(), current.taskTestingStartedHint())
                                }
                            }
                            show(task.id)
                        }
                    }
                    action(texts.taskCommit()) {
                        isEnabled = isTested(task, requests)
                        onClick { committing() }
                    }
                    action(texts.taskRevert()) {
                        isEnabled = stateOf(task) == TaskState.Uncommitted
                        onClick {
                            val current = currentLocalization().pageDeveloper
                            confirm(
                                title = current.taskRevertTitle(),
                                text = current.taskRevertText(),
                                action = current.taskRevert(),
                                isDanger = true,
                            ) {
                                developerService.revertTask(task.id)
                                toast(FeedbackKind.Success, currentLocalization().pageDeveloper.taskReverted())
                                show(task.id)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Builds the commit dialog of [task] and returns its opening. */
    private fun commitDialog(task: TaskVo): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        lateinit var regrade: ValueInput<Boolean>
        val committing = dialog(title = texts.taskCommitTitle()) {
            row { text(texts.taskCommitText()) }
            row {
                regrade = checkbox(texts.taskCommitRegrade(), labelSize = 18, size = 6) {
                    value = false
                }
            }
            row { text(texts.taskCommitRegradeHint()) }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.taskCommit()) {
                    onClick {
                        val current = currentLocalization().pageDeveloper
                        dialog.close()
                        refusable(current.taskCommitFailed()) {
                            developerService.commitTask(task.id, regrade.value)
                            toast(FeedbackKind.Success, current.taskCommitted())
                        }
                        show(task.id)
                    }
                }
            }
        }
        return {
            regrade.value = false
            committing.open()
        }
    }

    /** Builds the sharing dialog of [task] for the developer communities without access and returns its opening. */
    private fun shareDialog(task: TaskVo, shared: List<CommunityVo>): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val sharedIds = shared.map { community -> community.id }.toSet()
        val candidates = developerCommunities(multipleRoleUserService).filter { community -> community.id !in sharedIds }
        lateinit var chosen: ValueInput<Set<CommunityVo>>
        val sharing = dialog(title = texts.taskShareTitle()) {
            row {
                chosen = multiSelect(texts.communities(), labelSize = 6, size = 18, items = candidates, itemLabel = CommunityVo::name)
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.shareSubmit()) {
                    onClick {
                        val communities = chosen.value
                        if (communities.isNotEmpty()) {
                            val current = currentLocalization().pageDeveloper
                            dialog.close()
                            confirm(
                                title = current.taskShareConfirmTitle(),
                                text = current.taskShareConfirmText(),
                                action = current.shareSubmit(),
                                isDanger = true,
                            ) {
                                developerService.shareTask(task.id, communities.map { community -> community.id }.toSet())
                                toast(FeedbackKind.Success, currentLocalization().pageDeveloper.taskShareDone())
                                show(task.id)
                            }
                        }
                    }
                }
            }
        }
        return {
            chosen.value = emptySet()
            sharing.open()
        }
    }

    /** Builds the file choice of an upload to [taskId] and returns its opening; the next step describes the files. */
    private fun uploadDialog(taskId: TaskId): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val files = ConcurrentHashMap<String, FileData>()
        lateinit var drop: FileDropHandle
        val choosing = dialog(
            title = texts.taskUploadTitle(),
            subtitle = texts.taskUploadSubtitle(
                maxFiles = UPLOAD_LIMITS.maxFiles,
                maxMebibytes = UPLOAD_LIMITS.maxFileBytes / BYTES_PER_MEBIBYTE,
            ),
        ) {
            row {
                drop = fileDrop(texts.taskUploadFiles(), UPLOAD_LIMITS, consume = { file -> files[file.id] = file.toFileData() })
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.taskUploadNext()) {
                    onClick {
                        val chosen = drop.fileIds.mapNotNull(files::get)
                        if (chosen.isNotEmpty()) {
                            dialog.close()
                            describeUploads(taskId, chosen)
                        }
                    }
                }
            }
        }
        return {
            files.clear()
            drop.clear()
            choosing.open()
        }
    }

    /** Adds the rows describing an uploaded [file] to the dialog and returns the binder of its values. */
    private fun DialogSectionScope.uploadRows(file: FileData): Binder<UploadDraft> {
        val texts = currentLocalization().pageDeveloper
        val draft = Binder<UploadDraft>()
        lateinit var language: ValueInput<TrikSupportedLanguage?>
        lateinit var score: ValueInput<Int?>
        row {
            select(texts.type(), labelSize = 6, size = 18, items = ResourceKind.entries, itemLabel = ::kindLabelOf) {
                draft.forField(this)
                    .asRequired(texts.taskDescribeTypeRequired())
                    .bind({ values -> values.kind }, { values, kind -> values.kind = kind })
                addValueChangeListener { event ->
                    language.isVisible = event.value?.hasLanguage == true
                    score.isVisible = event.value?.hasScore == true
                }
            }
        }
        row {
            textInput(texts.name(), labelSize = 6, size = 18) {
                draft.forField(this)
                    .nameRules(texts.nameRequired())
                    .bind({ values -> values.name }, { values, name -> values.name = name })
            }
        }
        row {
            language = select(texts.language(), labelSize = 6, size = 18, items = LANGUAGES, itemLabel = ::languageLabel) {
                isVisible = false
                draft.forField(this)
                    .asRequired(texts.languageRequired())
                    .bind({ values -> values.language }, { values, chosen -> values.language = chosen })
                    .skipWhenHidden()
            }
        }
        row {
            score = integerInput(texts.expectedScore(), labelSize = 6, size = 18, min = 0) {
                isVisible = false
                draft.forField(this)
                    .asRequired(texts.scoreRequired())
                    .bind({ values -> values.score }, { values, expected -> values.score = expected })
                    .skipWhenHidden()
            }
        }

        draft.readBean(UploadDraft(name = file.uploadedFilename.substringBeforeLast('.')))
        return draft
    }

    /** Builds the attaching dialog of the [candidates] of [task] and returns its opening, which starts with no choice. */
    private fun attachDialog(task: TaskVo, candidates: List<Chain>): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val resources = candidates.map { chain -> chain.latest }
        lateinit var chosen: ValueInput<Set<ResourceVo>>
        val attaching = dialog(title = texts.taskAttachTitle(), size = DialogSize.L) {
            row {
                chosen = lookupMany(
                    texts.taskResourcesTitle(),
                    labelSize = 6,
                    size = 18,
                    fetch = { query, request ->
                        pageOf(resources.filter { resource -> resource.name.contains(query, ignoreCase = true) }, request)
                    },
                    display = ResourceVo::name,
                    columns = {
                        textColumn(texts.name(), size = 14) { resource -> resource.name }
                        textColumn(texts.type()) { resource -> typeOf(resource) }
                    },
                    hint = texts.taskAttachHint(),
                    dialogSize = DialogSize.L,
                )
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.attach()) {
                    onClick {
                        val selected = chosen.value
                        if (selected.isNotEmpty()) {
                            dialog.close()
                            changeState(task, next = TaskState.Uncommitted) {
                                try {
                                    selected.forEach { resource -> attach(task.id, resource) }
                                    val current = currentLocalization().pageDeveloper
                                    toast(FeedbackKind.Success, current.taskAttachDone(), current.taskTotal(count = selected.size))
                                } finally {
                                    show(task.id)
                                }
                            }
                        }
                    }
                }
            }
        }
        return {
            chosen.value = emptySet()
            attaching.open()
        }
    }

    /**
     * Runs [action], asking first to confirm that [task] moves to [next] if that differs from its state; a new task stays
     * new. Returns whether [action] ran without asking.
     */
    private fun changeState(task: TaskVo, next: TaskState?, action: () -> Unit): Boolean {
        val current = stateOf(task)
        if (next == null || next == current || current == TaskState.New) {
            action()
            return true
        }

        val texts = currentLocalization().pageDeveloper
        confirm(
            title = texts.taskStateChangeTitle(
                state = when (next) {
                    TaskState.New -> TaskStateChangeTitleState.NEW
                    TaskState.Uncommitted -> TaskStateChangeTitleState.UNCOMMITTED
                    TaskState.Committed -> TaskStateChangeTitleState.COMMITTED
                },
            ),
            text = texts.taskStateChangeText(),
            action = texts.taskStateChangeAction(),
            onConfirm = action,
        )
        return false
    }

    /** Runs [action] and shows its refusal with the reason as a toast titled [title]; other refusals propagate. */
    private fun refusable(title: String, action: () -> Unit) {
        try {
            action()
        } catch (exception: OperationException) {
            toast(FeedbackKind.Error, title, testingRefusalOf(exception.error) ?: throw exception)
        }
    }

    private fun upload(taskId: TaskId, file: FileData, draft: UploadDraft) {
        when (checkNotNull(draft.kind)) {
            ResourceKind.Statement -> developerService.addStatement(taskId, draft.name, file)
            ResourceKind.Exercise -> developerService.addExercise(taskId, draft.name, file, checkNotNull(draft.language))
            ResourceKind.Test -> developerService.addTest(taskId, draft.name, file)
            ResourceKind.DeveloperSolution -> developerService.addDeveloperSolution(
                taskId = taskId,
                resourceName = draft.name,
                file = file,
                language = checkNotNull(draft.language),
                expectedScore = Score(checkNotNull(draft.score)),
            )
        }
    }

    private fun attach(taskId: TaskId, resource: ResourceVo) {
        when (resource) {
            is StatementVo -> developerService.attachStatement(taskId, resource.id)
            is ExerciseVo -> developerService.attachExercise(taskId, resource.id)
            is TestVo -> developerService.attachTest(taskId, resource.id)
            is DeveloperSolutionVo -> developerService.attachDeveloperSolution(taskId, resource.id)
        }
    }

    private fun detach(taskId: TaskId, resource: ResourceVo) {
        when (resource) {
            is StatementVo -> developerService.detachStatement(taskId, resource.id)
            is ExerciseVo -> developerService.detachExercise(taskId, resource.id)
            is TestVo -> developerService.detachTest(taskId, resource.id)
            is DeveloperSolutionVo -> developerService.detachDeveloperSolution(taskId, resource.id)
        }
    }

    /**
     * Version chain of a resource uploaded to the task: its [latest] version and the known [versions], which hold at least
     * the latest one and every version a revision of the task refers to.
     */
    private class Chain(val latest: ResourceVo, val versions: List<ResourceVo>) {
        /** Returns the version of the chain [revision] refers to, or `null` if the chain is not attached to it. */
        fun attachedIn(revision: TaskRevisionVo?): ResourceVo? {
            val ids = idsOf(revision)
            return versions.firstOrNull { version -> version.id in ids }
        }
    }

    /** Applied filters of the resource table. */
    private data class ResourceFilter(
        val name: String = "",
        val kind: ResourceKind? = null,
        val usage: ResourceUsage = ResourceUsage.All,
        val isDifferencesOnly: Boolean = false,
    )

    private fun ResourceFilter.matches(chain: Chain, task: TaskVo): Boolean {
        val working = chain.attachedIn(task.wip)?.id
        val committed = chain.attachedIn(task.lastCommitted)?.id
        val isUsageMatching = when (usage) {
            ResourceUsage.All -> true
            ResourceUsage.Working -> working != null
            ResourceUsage.Committed -> committed != null
            ResourceUsage.AnyRevision -> working != null || committed != null
            ResourceUsage.Unattached -> working == null && committed == null
        }
        val isDifferent = task.wip != null && task.lastCommitted != null && working != committed
        return chain.latest.name.contains(name, ignoreCase = true) &&
            (kind == null || kindOf(chain.latest) == kind) && isUsageMatching && (!isDifferencesOnly || isDifferent)
    }

    /** Values of the task details form. */
    private class TaskDraft(var name: String = "", var description: String = "", var versions: Set<TrikStudioVersion> = emptySet())

    /** Values describing one uploaded file. */
    private class UploadDraft(
        var kind: ResourceKind? = null,
        var name: String = "",
        var language: TrikSupportedLanguage? = null,
        var score: Int? = null,
    )
}

/**
 * Returns whether a completed request of [requests] without failed submissions tested the working revision of [task];
 * the commit operation makes the final decision.
 */
private fun isTested(task: TaskVo, requests: List<TaskValidationRequestVo>): Boolean {
    val wip = task.wip ?: return false
    return requests.any { request ->
        val execution = request.execution
        val snapshot = request.snapshot
        execution is TaskValidationExecutionVo.Completed &&
            execution.failures.isEmpty() &&
            snapshot.tests.toSet() == wip.tests.toSet() &&
            snapshot.developerSolutions.map { input -> input.developerSolution }.toSet() == wip.developerSolutions.toSet() &&
            snapshot.supportedTrikStudioVersions.toSet() == wip.supportedTrikStudioVersions.toSet()
    }
}
