package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableCell
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouterLink
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.api.service.ServiceTrikStudioVersions
import tech.testsys.infra.database.api.service.TrikStudioVersionDeletion
import tech.testsys.infra.database.api.service.TrikStudioVersionRegistration
import tech.testsys.infra.database.api.service.TrikStudioVersionRejection
import tech.testsys.infra.database.api.service.TrikStudioVersionTagChange
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.security.ServiceSignIn

class ServiceTrikStudioVersionsViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var versions: ServiceTrikStudioVersions

    @Autowired
    private lateinit var signIn: ServiceSignIn

    private val registered = TrikStudioVersion(tag = "trikset/trik-studio:2026.1", version = "2026.1")
    private val added = TrikStudioVersion(tag = "trikset/trik-studio:2026.2", version = "2026.2")

    @BeforeEach
    fun prepareVersions() {
        clearMocks(versions)
        every { versions.findAll() } returns listOf(registered)
        every { versions.findUsedTags() } returns emptySet()
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceTrikStudioVersionsView::class.java)
    }

    @Test
    fun `should open the versions page from the service header`() {
        UI.getCurrent().navigate(ServiceHomeView::class.java)

        UI.getCurrent()._get<RouterLink> { text = "Версии TRIK Studio" }._click()

        assertEquals(ServiceTrikStudioVersionsView::class.java, currentView)
    }

    @Test
    fun `should show the version number and the Docker image tag of each registered version`() {
        assertEquals(listOf(listOf("2026.1", "trikset/trik-studio:2026.1")), rowTexts())
    }

    @Test
    fun `should show an empty state when no version is registered`() {
        every { versions.findAll() } returns emptyList()

        UI.getCurrent().navigate(ServiceHomeView::class.java)
        UI.getCurrent().navigate(ServiceTrikStudioVersionsView::class.java)

        val table = UI.getCurrent()._get<Table>().element.textRecursively
        assertTrue("Версий TRIK Studio нет" in table)
        assertFalse(registered.tag in table)
    }

    @Test
    fun `should register the entered version, close the dialog and show it in the list`() {
        val stored = mutableListOf(registered)
        every { versions.findAll() } answers { stored.toList() }
        every { versions.register(tag = added.tag, version = added.version) } answers {
            stored += added
            TrikStudioVersionRegistration.Registered(added)
        }
        openAddDialog()
        field("Версия")._value = added.version
        field("Тег образа")._value = added.tag

        openDialog()._get<Button> { text = "Добавить" }._click()

        verify(exactly = 1) { versions.register(tag = added.tag, version = added.version) }
        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Версия TRIK Studio добавлена", lastToastTitle())
        assertEquals(listOf(listOf("2026.1", "trikset/trik-studio:2026.1"), listOf("2026.2", "trikset/trik-studio:2026.2")), rowTexts())
    }

    @ParameterizedTest
    @CsvSource(
        "BlankVersion, Версия, Тег образа, Укажите версию",
        "VersionTooLong, Версия, Тег образа, Версия длиннее 255 символов",
        "VersionAlreadyRegistered, Версия, Тег образа, Такая версия уже зарегистрирована",
        "BlankTag, Тег образа, Версия, Укажите тег",
        "TagTooLong, Тег образа, Версия, Тег длиннее 512 символов",
        "TagAlreadyRegistered, Тег образа, Версия, Такой тег уже зарегистрирован",
    )
    fun `should keep the dialog open with an error on the refused field`(
        reason: TrikStudioVersionRejection,
        invalid: String,
        valid: String,
        error: String,
    ) {
        every { versions.register(any(), any()) } returns TrikStudioVersionRegistration.Rejected(reason)
        openAddDialog()

        openDialog()._get<Button> { text = "Добавить" }._click()

        assertTrue(field(invalid).isInvalid)
        assertEquals(error, field(invalid).errorMessage)
        assertFalse(field(valid).isInvalid)
        assertEquals(listOf(listOf("2026.1", "trikset/trik-studio:2026.1")), rowTexts())
    }

    @Test
    fun `should start a new dialog with empty fields`() {
        openAddDialog()
        field("Версия")._value = added.version
        openDialog()._get<Button> { text = "Отменить" }._click()

        openAddDialog()

        assertEquals("", field("Версия").value)
        assertEquals("", field("Тег образа").value)
    }

    @Test
    fun `should change the tag of a version and show it in the list`() {
        val changed = registered.copy(tag = "trikset/trik-studio:2026.1-fix")
        val stored = mutableListOf(registered)
        every { versions.findAll() } answers { stored.toList() }
        every { versions.changeTag(tag = registered.tag, newTag = changed.tag) } answers {
            stored[0] = changed
            TrikStudioVersionTagChange.Changed(changed)
        }
        versionMenu()._clickItemWithCaption("Изменить тег")
        field("Тег образа")._value = changed.tag

        openDialog()._get<Button> { text = "Сохранить" }._click()

        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Тег версии TRIK Studio изменён", lastToastTitle())
        assertEquals(listOf(listOf("2026.1", changed.tag)), rowTexts())
    }

    @Test
    fun `should keep the tag dialog open with the refusal of the new tag`() {
        every { versions.changeTag(any(), any()) } returns
            TrikStudioVersionTagChange.Rejected(TrikStudioVersionRejection.TagAlreadyRegistered)
        versionMenu()._clickItemWithCaption("Изменить тег")

        openDialog()._get<Button> { text = "Сохранить" }._click()

        assertEquals("Такой тег уже зарегистрирован", field("Тег образа").errorMessage)
    }

    @Test
    fun `should close the tag dialog, report the deleted version and reread the list if the version is gone`() {
        val stored = mutableListOf(registered)
        every { versions.findAll() } answers { stored.toList() }
        every { versions.changeTag(any(), any()) } answers {
            stored.clear()
            TrikStudioVersionTagChange.NotFound
        }
        versionMenu()._clickItemWithCaption("Изменить тег")

        openDialog()._get<Button> { text = "Сохранить" }._click()

        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Версия TRIK Studio уже удалена", lastToastTitle())
        assertTrue("Версий TRIK Studio нет" in UI.getCurrent()._get<Table>().element.textRecursively)
    }

    @Test
    fun `should delete an unused version after the confirmation`() {
        val stored = mutableListOf(registered)
        every { versions.findAll() } answers { stored.toList() }
        every { versions.delete(registered.tag) } answers {
            stored.clear()
            TrikStudioVersionDeletion.Deleted
        }
        versionMenu()._clickItemWithCaption("Удалить")

        openDialog()._get<Button> { text = "Удалить" }._click()

        assertEquals("Версия TRIK Studio удалена", lastToastTitle())
        assertTrue("Версий TRIK Studio нет" in UI.getCurrent()._get<Table>().element.textRecursively)
    }

    @ParameterizedTest
    @CsvSource("InUse, Версия TRIK Studio уже используется", "NotFound, Версия TRIK Studio уже удалена")
    fun `should report a refused deletion and reread the list`(refusal: TrikStudioVersionDeletion, title: String) {
        val used = mutableSetOf<String>()
        every { versions.findUsedTags() } answers { used.toSet() }
        every { versions.delete(registered.tag) } answers {
            used += registered.tag
            refusal
        }
        versionMenu()._clickItemWithCaption("Удалить")

        openDialog()._get<Button> { text = "Удалить" }._click()

        assertEquals(title, lastToastTitle())
        assertEquals(listOf("Изменить тег"), versionMenu().items.map { item -> item.element.textRecursively })
    }

    @Test
    fun `should not offer deleting a used version`() {
        every { versions.findUsedTags() } returns setOf(registered.tag)

        UI.getCurrent().navigate(ServiceHomeView::class.java)
        UI.getCurrent().navigate(ServiceTrikStudioVersionsView::class.java)

        assertEquals(listOf("Изменить тег"), versionMenu().items.map { item -> item.element.textRecursively })
    }

    private fun versionMenu(): ContextMenu {
        val trigger = UI.getCurrent()._find<Button>().single { button ->
            button.element.getAttribute("aria-label") == "Действия с версией ${registered.version}"
        }
        return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
    }

    private fun openAddDialog() {
        UI.getCurrent()._get<Button> { text = "Добавить" }._click()
    }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun field(label: String): TextField = openDialog()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    /** Returns the texts of the version and tag cells of each row, without the menu cell. */
    private fun rowTexts(): List<List<String>> = UI.getCurrent()._get<Table>()._get<TableBody>()._find<TableRow>()
        .map { row -> row._find<TableCell>().take(2).map { cell -> cell.element.textRecursively } }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text
}
