package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableCell
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
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
import tech.testsys.infra.database.api.service.TrikStudioVersionRegistration
import tech.testsys.infra.database.api.service.TrikStudioVersionRejection
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
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceTrikStudioVersionsView::class.java)
    }

    @Test
    fun `should open the versions page from the service home`() {
        UI.getCurrent().navigate(ServiceHomeView::class.java)

        UI.getCurrent()._get<Button> { text = "Версии TRIK Studio" }._click()

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

    private fun openAddDialog() {
        UI.getCurrent()._get<Button> { text = "Добавить" }._click()
    }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun field(label: String): TextField = openDialog()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun rowTexts(): List<List<String>> = UI.getCurrent()._get<Table>()._get<TableBody>()._find<TableRow>()
        .map { row -> row._find<TableCell>().map { cell -> cell.element.textRecursively } }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text
}
