package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
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
import tech.testsys.web.components.actions.action
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
import tech.testsys.web.components.texts.UiTexts
import java.util.concurrent.ConcurrentHashMap

/** Columns of the task details block; the access block takes the rest of the row beside it. */
private const val DETAILS_COLUMNS = 14

/** Limits of one upload of resource files. */
private val UPLOAD_LIMITS =
    UploadLimits(maxFiles = 20, maxFileBytes = MAX_RESOURCE_BYTES, maxMemoryBytes = 5L * MAX_RESOURCE_BYTES)

/**
 * Type of an uploaded resource.
 *
 * @property label the label of the type.
 * @property hasLanguage whether the resource has a language.
 * @property hasScore whether the resource has an expected score.
 */
internal enum class ResourceKind(val label: String, val hasLanguage: Boolean, val hasScore: Boolean) {
    /** Statement of the task. */
    Statement("Условие", hasLanguage = false, hasScore = false),

    /** Exercise of a language. */
    Exercise("Упражнение", hasLanguage = true, hasScore = false),

    /** Polygon. */
    Test("Полигон", hasLanguage = false, hasScore = false),

    /** Developer solution with its expected score. */
    DeveloperSolution("Авторское решение", hasLanguage = true, hasScore = true),
}

/** Revisions in which a resource chain is used. */
internal enum class ResourceUsage(val label: String) {
    All("Все"),
    Working("В рабочей версии"),
    Committed("В зафиксированной версии"),
    AnyRevision("В любой версии"),
    Unattached("Нигде не прикреплены"),
}

/**
 * Task of a Developer (testsys.web.page.developer.task): its details with their editing, its resources with uploading,
 * attaching and detaching, its testing with committing and reverting, and its access of communities. Before an action
 * that changes the state of the task, the page asks to confirm the new state.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/tasks/:taskId([0-9]+)")
@PageTitle("Задача")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperTaskView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val multipleRoleUserService: MultipleRoleUserService,
) : TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        show(TaskId(event.routeParameters.getLong(TASK_ID_PARAMETER).orElseThrow()))
    }

    /**
     * Opens the dialog that describes each of [files] uploaded to [taskId]: its type, name, language and expected score;
     * saving it uploads the files one by one and shows the task again.
     */
    internal fun describeUploads(taskId: TaskId, files: List<FileData>) {
        val uploads = mutableListOf<Pair<FileData, Binder<UploadDraft>>>()
        val describing = dialog(
            title = "Новые ресурсы",
            subtitle = "Ресурсы загрузятся в задачу без прикрепления",
            size = DialogSize.L,
        ) {
            files.forEachIndexed { index, file ->
                section("Ресурс ${index + 1} · ${file.uploadedFilename}") { uploads.add(file to uploadRows(file)) }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Загрузить") {
                    onClick {
                        val drafts = uploads.map { (file, binder) ->
                            file to UploadDraft().takeIf { values -> binder.writeBeanIfValid(values) }
                        }
                        if (drafts.all { (_, values) -> values != null }) {
                            dialog.close()
                            try {
                                drafts.forEach { (file, values) -> upload(taskId, file, checkNotNull(values)) }
                                toast(FeedbackKind.Success, "Ресурсы загружены", "Всего: ${drafts.size}.")
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
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Задача «${task.name}»") {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Разработчика", DeveloperView::class.java)
                crumb("Задачи", DeveloperView::class.java, developerSectionParameters(TASKS_SECTION))
                badge(state.label, state.tone)
            }
            row {
                detailsBlock(task, versions)
                accessBlock(task, shared)
            }
            resourcesBlock(task, chains)
            testingBlock(task, requests)
        }
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
        val draft = Binder<TaskDraft>()
        val supported = (task.wip ?: task.lastCommitted)?.supportedTrikStudioVersions.orEmpty().toSet()
        val initial = TaskDraft(task.name, task.description, supported)
        block(size = DETAILS_COLUMNS, title = "Сведения") {
            editing(
                onSave = {
                    val values = TaskDraft()
                    draft.writeBeanIfValid(values) && saveDetails(task, values, supported)
                },
                onCancel = { draft.readBean(initial) },
            )
            row {
                codeInput("ID", labelSize = 6, size = 18) {
                    value = task.id.value.toString()
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
            row {
                textArea("Описание", labelSize = 6, size = 18, maxLines = 4) {
                    draft.forField(this)
                        .bind({ values -> values.description }, { values, description -> values.description = description })
                }
            }
            row {
                multiSelect("Версии TRIK Studio", labelSize = 6, size = 18, items = versions, itemLabel = TrikStudioVersion::version) {
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
            toast(FeedbackKind.Success, "Задача изменена")
            show(task.id)
        }
    }

    private fun PageRowScope.accessBlock(task: TaskVo, shared: List<CommunityVo>) {
        block(title = "Доступ") {
            table(key = { community: CommunityVo -> community.id }, fetch = { request -> pageOf(shared, request) }) {
                textColumn("Сообщество") { community -> community.name }
                empty("Доступ не предоставлен", "Сообщества получат доступ к последней зафиксированной версии.")
            }
            val sharing = shareDialog(task, shared)
            actions {
                action("Предоставить доступ") {
                    isEnabled = task.lastCommitted != null
                    onClick { sharing() }
                }
            }
        }
    }

    private fun PageScope.resourcesBlock(task: TaskVo, chains: List<Chain>) {
        val editable = task.wip ?: task.lastCommitted
        row {
            block(title = "Ресурсы", subtitle = "Условие, упражнения, полигоны и авторские решения") {
                var resourceFilter = ResourceFilter()
                val rows = table(
                    key = { chain: Chain -> chain.latest.versionBucket },
                    fetch = { request -> pageOf(chains.filter { chain -> resourceFilter.matches(chain, task) }, request) },
                ) {
                    textColumn("Название", size = 6) { chain -> chain.latest.name }
                    textColumn("Тип", size = 3) { chain -> typeOf(chain.latest) }
                    column("Прикреплено") { chain ->
                        if (chain.attachedIn(task.lastCommitted) != null) badge("Прикреплено в зафиксированной", tone = Tone.Success)
                        if (chain.attachedIn(task.wip) != null) badge("Прикреплено в рабочей", tone = Tone.Warning)
                    }
                    menuColumn(ariaLabel = { chain -> "Действия с ресурсом «${chain.latest.name}»" }) { chain ->
                        val attached = chain.attachedIn(editable)
                        item("Открепить", isEnabled = attached != null) {
                            changeState(task, next = TaskState.Uncommitted) {
                                detach(task.id, checkNotNull(attached))
                                toast(FeedbackKind.Success, "Ресурс откреплён")
                                show(task.id)
                            }
                        }
                    }
                    if (chains.isEmpty()) {
                        empty("Ресурсов пока нет", "Загрузите условие, упражнения, полигоны и авторские решения.")
                    } else {
                        empty("Ресурсы не найдены", "Измените или сбросьте фильтры.")
                    }
                    onRowClick(isNavigation = true) { chain -> openDeveloperResource(task.id, chain.latest.versionBucket) }
                }
                resourceFilters(task, apply = { applied -> resourceFilter = applied }) { rows.refresh(toFirstPage = true) }
                val uploading = uploadDialog(task.id)
                val attaching = attachDialog(task, chains.filter { chain -> chain.attachedIn(editable) == null })
                actions {
                    action("Загрузить ресурсы") { onClick { uploading() } }
                    action("Прикрепить") { onClick { attaching() } }
                }
            }
        }
    }

    private fun BlockScope.resourceFilters(task: TaskVo, apply: (ResourceFilter) -> Unit, refresh: () -> Unit) {
        val isComparisonAvailable = task.wip != null && task.lastCommitted != null
        val comparisonHint = if (isComparisonAvailable) {
            "Различия версий: добавленные, откреплённые или заменённые ресурсы."
        } else {
            "Для сравнения нужны рабочая и зафиксированная версии задачи."
        }
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
                name = textInput("Название", labelSize = 4, size = 8, hint = "Часть названия, без учёта регистра")
                kind = select("Тип", labelSize = 4, size = 8, items = ResourceKind.entries, itemLabel = ResourceKind::label)
            }
            row {
                usage = select("Использование", labelSize = 4, size = 8, items = ResourceUsage.entries, itemLabel = ResourceUsage::label) {
                    value = ResourceUsage.All
                }
                differences = checkbox(
                    "Различия версий",
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
        row {
            block(title = "Тестирование") {
                table(key = { request: TaskValidationRequestVo -> request.id }, fetch = { request -> pageOf(requests, request) }) {
                    dateTimeColumn("Запущено", size = 6) { request -> request.createdAt.toServerDateTime() }
                    column("Статус", size = 5) { request ->
                        val (label, tone) = validationStatusOf(request.execution)
                        badge(label, tone)
                    }
                    textColumn("Итог") { request -> summaryOf(request.execution) }
                    empty("Тестирований пока не было", "Протестируйте рабочую версию, чтобы её можно было зафиксировать.")
                    onRowClick(isNavigation = true) { request -> openDeveloperTesting(task.id, request.id) }
                }
                val committing = commitDialog(task)
                actions {
                    action("Протестировать") {
                        isEnabled = task.wip != null
                        onClick {
                            refusable("Не удалось запустить тестирование") {
                                val request = developerService.testTask(task.id)
                                if (requests.any { existing -> existing.id == request.id }) {
                                    toast(FeedbackKind.Info, "Тестирование уже идёт", "Дождитесь результата текущего запроса.")
                                } else {
                                    toast(FeedbackKind.Success, "Тестирование запущено", "Результат появится в истории тестирований.")
                                }
                            }
                            show(task.id)
                        }
                    }
                    action("Зафиксировать") {
                        isEnabled = isTested(task, requests)
                        onClick { committing() }
                    }
                    action("Откатить") {
                        isEnabled = stateOf(task) == TaskState.Uncommitted
                        onClick {
                            confirm(
                                title = "Откатить изменения задачи?",
                                text = "Задача вернётся к последней зафиксированной версии и перейдёт в состояние " +
                                    "«${TaskState.Committed.label}». Незафиксированные изменения пропадут.",
                                action = "Откатить",
                                isDanger = true,
                            ) {
                                developerService.revertTask(task.id)
                                toast(FeedbackKind.Success, "Изменения задачи отменены")
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
        lateinit var regrade: ValueInput<Boolean>
        val committing = dialog(title = "Зафиксировать задачу?") {
            row {
                text(
                    "Задача перейдёт в состояние «${TaskState.Committed.label}». Туры с задачей начнут использовать новую версию.",
                )
            }
            row {
                regrade = checkbox("Перепроверить посылки", labelSize = 18, size = 6) {
                    value = false
                }
            }
            row { text("Посылки в турах будут проверены заново, если изменился набор полигонов.") }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Зафиксировать") {
                    onClick {
                        dialog.close()
                        refusable("Не удалось зафиксировать задачу") {
                            developerService.commitTask(task.id, regrade.value)
                            toast(FeedbackKind.Success, "Задача зафиксирована")
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
        val sharedIds = shared.map { community -> community.id }.toSet()
        val candidates = developerCommunities(multipleRoleUserService).filter { community -> community.id !in sharedIds }
        lateinit var chosen: ValueInput<Set<CommunityVo>>
        val sharing = dialog(title = "Доступ к задаче") {
            row {
                chosen = multiSelect("Сообщества", labelSize = 6, size = 18, items = candidates, itemLabel = CommunityVo::name)
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Предоставить") {
                    onClick {
                        val communities = chosen.value
                        if (communities.isNotEmpty()) {
                            dialog.close()
                            confirm(
                                title = "Предоставить доступ к задаче?",
                                text = "Отозвать доступ нельзя: сообщества сохранят доступ к задаче.",
                                action = "Предоставить",
                                isDanger = true,
                            ) {
                                developerService.shareTask(task.id, communities.map { community -> community.id }.toSet())
                                toast(FeedbackKind.Success, "Доступ к задаче предоставлен")
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
        val files = ConcurrentHashMap<String, FileData>()
        lateinit var drop: FileDropHandle
        val choosing = dialog(title = "Загрузка ресурсов", subtitle = "До 20 файлов, каждый до 10 МиБ") {
            row {
                drop = fileDrop("Файлы", UPLOAD_LIMITS, consume = { file -> files[file.id] = file.toFileData() })
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Далее") {
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
        val draft = Binder<UploadDraft>()
        lateinit var language: ValueInput<TrikSupportedLanguage?>
        lateinit var score: ValueInput<Int?>
        row {
            select("Тип", labelSize = 6, size = 18, items = ResourceKind.entries, itemLabel = ResourceKind::label) {
                draft.forField(this)
                    .asRequired("Выберите тип")
                    .bind({ values -> values.kind }, { values, kind -> values.kind = kind })
                addValueChangeListener { event ->
                    language.isVisible = event.value?.hasLanguage == true
                    score.isVisible = event.value?.hasScore == true
                }
            }
        }
        row {
            textInput("Название", labelSize = 6, size = 18) {
                draft.forField(this)
                    .nameRules("Укажите название")
                    .bind({ values -> values.name }, { values, name -> values.name = name })
            }
        }
        row {
            val languages = LANGUAGE_LABELS.keys.toList()
            language = select("Язык", labelSize = 6, size = 18, items = languages, itemLabel = LANGUAGE_LABELS::getValue) {
                isVisible = false
                draft.forField(this)
                    .asRequired("Выберите язык")
                    .bind({ values -> values.language }, { values, chosen -> values.language = chosen })
                    .skipWhenHidden()
            }
        }
        row {
            score = integerInput("Ожидаемый балл", labelSize = 6, size = 18, min = 0) {
                isVisible = false
                draft.forField(this)
                    .asRequired("Укажите балл")
                    .bind({ values -> values.score }, { values, expected -> values.score = expected })
                    .skipWhenHidden()
            }
        }

        draft.readBean(UploadDraft(name = file.uploadedFilename.substringBeforeLast('.')))
        return draft
    }

    /** Builds the attaching dialog of the [candidates] of [task] and returns its opening, which starts with no choice. */
    private fun attachDialog(task: TaskVo, candidates: List<Chain>): () -> Unit {
        val resources = candidates.map { chain -> chain.latest }
        lateinit var chosen: ValueInput<Set<ResourceVo>>
        val attaching = dialog(title = "Прикрепление ресурсов", size = DialogSize.L) {
            row {
                chosen = lookupMany(
                    "Ресурсы",
                    labelSize = 6,
                    size = 18,
                    fetch = { query, request ->
                        pageOf(resources.filter { resource -> resource.name.contains(query, ignoreCase = true) }, request)
                    },
                    display = ResourceVo::name,
                    columns = {
                        textColumn("Название", size = 14) { resource -> resource.name }
                        textColumn("Тип") { resource -> typeOf(resource) }
                    },
                    hint = "Прикрепляется последняя версия ресурса",
                    dialogSize = DialogSize.L,
                )
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Прикрепить") {
                    onClick {
                        val selected = chosen.value
                        if (selected.isNotEmpty()) {
                            dialog.close()
                            changeState(task, next = TaskState.Uncommitted) {
                                try {
                                    selected.forEach { resource -> attach(task.id, resource) }
                                    toast(FeedbackKind.Success, "Ресурсы прикреплены", "Всего: ${selected.size}.")
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

        confirm(
            title = "Задача перейдёт в состояние «${next.label}»",
            text = "Изменения попадут в рабочую версию. Туры продолжат использовать последнюю зафиксированную версию, " +
                "пока задачу не зафиксируют снова.",
            action = "Продолжить",
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
            (kind == null || typeOf(chain.latest) == kind.label) && isUsageMatching && (!isDifferencesOnly || isDifferent)
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
