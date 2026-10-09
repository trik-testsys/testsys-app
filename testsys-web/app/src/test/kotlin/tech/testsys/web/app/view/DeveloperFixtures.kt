package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.AppFixtures
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.StatementVo
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import javax.sql.DataSource

/**
 * Developers with their tasks, resources and contests. Content is created through the service of the signed-in developer
 * where an operation exists; a committed task and TRIK Studio versions are stored directly, since tests have no grader.
 */
class DeveloperFixtures(
    private val fixtures: AppFixtures,
    private val developerService: DeveloperService,
    private val tasks: TaskRepository,
    private val dataSource: DataSource,
) {
    /** Returns a new developer, a member of [communities] in the developer role. */
    fun developer(vararg communities: Community, name: String = fixtures.unique("Developer")): MultipleRoleUser =
        fixtures.multipleRoleUser(name = name) {
            roles {
                developer {
                    memberOf(communities.map { community -> community.id.value })
                    data = developerData {}
                }
            }
        }

    /** Signs in a new developer, a member of [communities], and returns it. */
    fun signInDeveloper(vararg communities: Community): MultipleRoleUser = developer(*communities).also { developer ->
        CabinetSignIn.signIn(CabinetPrincipal.of(developer))
    }

    /** Registers a new TRIK Studio version; versions come from the grader, so no port stores them. */
    fun trikStudioVersion(tag: String = fixtures.unique("tsv")): TrikStudioVersion {
        dataSource.connection.use { connection ->
            val insert = "insert into ts_trik_studio_version (id, tag) select coalesce(max(id), 0) + 1, ? from ts_trik_studio_version"
            connection.prepareStatement(insert).use { statement ->
                statement.setString(1, tag)
                statement.executeUpdate()
            }
        }
        return TrikStudioVersion(tag)
    }

    /** Creates a new task of the signed-in developer. */
    fun task(name: String = fixtures.unique("Task")): TaskVo = developerService.createTask(taskName = name, taskDescription = "")

    /** Uploads a statement named [name] to [taskId] of the signed-in developer. */
    fun statement(taskId: TaskId, name: String = "Условие"): StatementVo =
        developerService.addStatement(taskId, name, FileData(uploadedFilename = "statement.pdf", content = "statement".toByteArray()))

    /**
     * Stores a committed task of the signed-in developer supporting [version], with an uploaded statement and a Python
     * exercise as its committed content.
     */
    fun committedTask(version: TrikStudioVersion, name: String = fixtures.unique("Task")): TaskId {
        val draft = task(name)
        val statement = statement(draft.id)
        val exercise = developerService.addExercise(
            taskId = draft.id,
            resourceName = "Упражнение",
            file = FileData(uploadedFilename = "exercise.qrs", content = "exercise".toByteArray()),
            language = TrikSupportedLanguage.Python,
        )
        val stored = checkNotNull(tasks.findById(draft.id))
        tasks.update(
            stored.withData {
                content.committed {
                    this.statement = statement.id
                    exercises = mutableListOf(exercise.id)
                    supportedTrikStudioVersions = mutableListOf(version)
                }
            },
        )
        return draft.id
    }

    /** Creates a new contest of the signed-in developer in [version]. */
    fun contest(version: TrikStudioVersion, name: String = fixtures.unique("Contest")): ContestVo =
        developerService.createContest(contestName = name, trikStudioVersion = version)
}

/** Returns the open dialog. */
internal fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

/** Returns the components of type [T] of the open dialog, or of the page outside dialogs without one. */
internal inline fun <reified T : Component> inScope(): List<T> {
    val dialog = UI.getCurrent()._find<Dialog>().singleOrNull { candidate -> candidate.isOpened }
    return dialog?._find<T>() ?: UI.getCurrent()._find<T>().filterNot { component ->
        generateSequence<Component>(component) { parent -> parent.parent.orElse(null) }.any { ancestor -> ancestor is Dialog }
    }
}

/** Returns the text field named [label] of the open dialog, or of the page without one. */
internal fun textField(label: String): TextField = inScope<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

/** Clicks the button named [label] of the open dialog, or of the page without one. */
internal fun clickButton(label: String) {
    inScope<Button>().single { button -> button.text == label }._click()
}

/** Returns the button named [label] of the page. */
internal fun pageButton(label: String): Button = UI.getCurrent()._find<Button>().single { button -> button.text == label }

/** Returns the text of the table whose text contains [marker]. */
internal fun tableText(marker: String): String =
    UI.getCurrent()._find<Table>().map { table -> table.element.textRecursively }.first { text -> marker in text }

/** Returns the menu of the row whose menu button is named [label]. */
internal fun rowMenu(label: String): ContextMenu {
    val trigger = UI.getCurrent()._find<Button>().single { button -> button.element.getAttribute("aria-label") == label }
    return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
}

/** Returns the descriptions of the alerts of the page. */
internal fun alertDescriptions(): List<String> = UI.getCurrent()._find<Span> { classes = "ts-alert__desc" }.map { span -> span.text }

/** Returns the title of the last toast. */
internal fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

/** Returns the description of the last toast. */
internal fun lastToastDescription(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text

/** Returns the path of the open page. */
internal fun currentPath(): String = UI.getCurrent().internals.activeViewLocation.path

/** Data of a click on [row] as the client sends it when the click passes every filter of the row. */
internal fun acceptedClick(row: TableRow): ObjectNode = ObjectMapper().createObjectNode().apply {
    row.element.node.getFeature(ElementListenerMap::class.java).getExpressions("click")
        .forEach { expression -> put(expression, true) }
}
