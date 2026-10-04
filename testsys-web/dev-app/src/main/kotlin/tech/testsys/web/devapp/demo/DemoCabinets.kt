package tech.testsys.web.devapp.demo

import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.display.text
import tech.testsys.web.components.layout.PageScope
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val DISPLAY_DATE_LENGTH = 10

/** Existing role-scoped demonstration sections, without additional creation scenarios. */
internal fun PageScope.demoCabinet(context: DemoContext, cabinet: DemoCabinet, actor: DemoUser, section: String) {
    val objects = context.state.objects(actor)
    if (cabinet.key == "organizer") {
        demoOrganizer(context, actor, section)
        return
    }
    if (section in listOf("overview", "profile", "user", "purpose", "capabilities")) {
        demoInfo("Сведения о пользователе", actor.infoFields())
    }
    if (cabinet.key in listOf("student", "participant") && section in listOf("overview", "study", "tours", "solutions")) {
        demoStudy(context, actor, objects, section)
    }
    if (section in listOf("overview", "resources") && cabinet.key == "developer") demoResources(context, actor, objects)
    if (cabinet.key == "developer" && section in listOf("overview", "tasks")) {
        demoList(
            context = context,
            actor = actor,
            key = "tasks",
            title = "Задачи",
            rows = objects.tasks.map {
                DemoRow(id = it.id, title = it.name, category = demoTaskStateLabel(it.state), detail = it.description)
            },
        ) { selected ->
            val task = objects.tasks.first { it.id == selected.id }
            demoInfo(
                "Сведения о задаче",
                listOf(
                    "ID" to task.id,
                    "Название" to task.name,
                    "Описание" to task.description,
                    "Состояние" to demoTaskStateLabel(task.state),
                    "TRIK Studio" to task.trikVersions.joinToString(", "),
                    "Виды авторских решений" to task.authorSolutionKinds.joinToString(", ", transform = ::demoKindLabel),
                ),
                wideLabels = setOf("Описание", "Виды авторских решений"),
            )
        }
    }
    if (cabinet.key in listOf("developer", "observer") && section in listOf("overview", "tours")) {
        demoList(context = context, actor = actor, key = "tours", title = "Туры", rows = objects.tours.map { it.row() }) { selected ->
            demoTourMaterials(context, objects.tours.first { it.id == selected.id })
        }
    }
    if (cabinet.key == "administrator" && section in listOf("overview", "users", "user")) {
        demoList(
            context = context,
            actor = actor,
            key = "users",
            title = "Пользователи",
            rows = objects.users.map { DemoRow(id = it.id, title = it.alias, category = it.role, detail = it.email.orEmpty()) },
        ) { selected -> demoInfo("Сведения о выбранном пользователе", objects.users.first { it.id == selected.id }.infoFields()) }
    }
    if (cabinet.key == "judge") demoJudge(context, actor, objects, section)
    if (cabinet.key == "observer" && section == "results") demoObserverResults(context, actor, objects)
    if (cabinet.key == "supervisor") {
        block("Назначение роли") {
            row { text("Управление пользователями в пределах одного сообщества. Новые операции в демонстрации не добавлены.") }
        }
    }
}

private fun DemoUser.infoFields(): List<Pair<String, String>> = listOf(
    "ID" to id,
    "Псевдоним" to alias,
    "Роль" to role,
    "Код-доступа" to accessCode,
    "Почта" to email.orEmpty(),
    "Последний вход" to lastLogin,
)

private fun PageScope.demoResources(context: DemoContext, actor: DemoUser, objects: DemoObjects) {
    demoList(
        context = context,
        actor = actor,
        key = "resources",
        title = "Ресурсы",
        rows = objects.resources.map {
            DemoRow(id = it.id, title = it.name, category = it.category, date = demoDate(it.modifiedAt), detail = it.fileName)
        },
    ) { selected ->
        val resource = objects.resources.first { it.id == selected.id }
        demoInfo(
            "Сведения о ресурсе",
            listOf("ID" to resource.id, "Название" to resource.name, "Тип" to resource.category, "Файл" to resource.fileName),
        )
        block("История изменений") {
            val revisions = resource.history.map {
                DemoRow(id = it.modifiedAt, title = it.fileName, date = demoDate(it.modifiedAt), detail = it.comment)
            }
            demoTable(
                state = context.table("${actor.id}:${resource.id}:history"),
                rows = revisions,
                columns = {
                    textColumn("Изменён") { it.id }
                    textColumn("Файл") { it.title }
                    textColumn("Комментарий") { it.detail }
                    column("Версия") { revision ->
                        demoDownload(filename = revision.title, content = "Демонстрационный ресурс: ${resource.name}")
                    }
                },
            )
        }
    }
}

/** Projects a tour for ordinary filters and table columns. */
internal fun DemoTour.row(): DemoRow = DemoRow(id = id, title = name, category = "Тур", date = demoDate(startsAt), detail = startsAt)

/** Parses the fixed display dates of the sample records. */
internal fun demoDate(value: String): LocalDate = LocalDate.parse(
    value.take(DISPLAY_DATE_LENGTH),
    DateTimeFormatter.ofPattern("dd.MM.yyyy"),
)

/** Selection is retained outside filtering and pagination. */
private fun PageScope.demoList(
    context: DemoContext,
    actor: DemoUser,
    key: String,
    title: String,
    rows: List<DemoRow>,
    details: PageScope.(DemoRow) -> Unit,
) {
    val contextKey = "${actor.id}:$key"
    block(title) {
        demoTable(context.table(contextKey), rows) { context.select(key = contextKey, id = it.id) }
    }
    rows.firstOrNull { it.id == context.selection(key = contextKey, fallback = rows.firstOrNull()?.id) }?.let { details(it) }
}

/** Real download of explicitly labelled mock content. */
internal fun tech.testsys.web.components.layout.ContentScope.demoDownload(filename: String, content: String) {
    downloadAction(
        "Скачать $filename",
        produce = {
            val bytes = content.toByteArray(Charsets.UTF_8)
            DownloadContent(filename = filename, contentType = "text/plain;charset=utf-8", length = bytes.size.toLong()) {
                ByteArrayInputStream(bytes)
            }
        },
    )
}

/** Tour information and existing example task materials. */
internal fun PageScope.demoTourMaterials(context: DemoContext, tour: DemoTour) {
    demoInfo(
        "Сведения о туре",
        listOf(
            "ID" to tour.id,
            "Название" to tour.name,
            "Описание" to tour.description,
            "Начало" to tour.startsAt,
            "Конец" to tour.endsAt,
            "Длительность" to "${tour.durationMinutes} минут",
            "Осталось" to "${tour.remainingSeconds / SECONDS_PER_MINUTE} минут",
            "TRIK Studio" to tour.trikVersion,
        ),
        wideLabels = setOf("Описание"),
    )
    val tasks = tour.taskIds.map { id -> context.state.tasks.first { it.id == id } }
    block("Задачи тура") {
        demoTable(
            context.table("${context.screen}:${tour.id}:materials"),
            tasks.map { task -> DemoRow(id = task.id, title = task.name, detail = task.description) },
        )
    }
}

private const val SECONDS_PER_MINUTE = 60
