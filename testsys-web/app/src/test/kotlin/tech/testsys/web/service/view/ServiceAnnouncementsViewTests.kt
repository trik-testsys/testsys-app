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
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.infra.database.api.announcement.Announcement
import tech.testsys.infra.database.api.announcement.AnnouncementId
import tech.testsys.infra.database.api.announcement.AnnouncementKind
import tech.testsys.infra.database.api.announcement.AnnouncementRejection
import tech.testsys.infra.database.api.announcement.AnnouncementSave
import tech.testsys.infra.database.api.announcement.Announcements
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.error.ServiceAccessDeniedView
import tech.testsys.web.service.security.ServiceSignIn
import java.time.Instant
import java.time.LocalDateTime

class ServiceAnnouncementsViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var announcements: Announcements

    @Autowired
    private lateinit var signIn: ServiceSignIn

    // The service UI shows and reads dates in the fallback zone Europe/Moscow (UTC+3) of the tests.
    private val stored = Announcement(
        id = AnnouncementId(1),
        text = "Плановые работы в субботу",
        kind = AnnouncementKind.Warning,
        shownFrom = Instant.parse("2026-03-09T09:00:00Z"),
        shownUntil = Instant.parse("2026-03-14T21:00:00Z"),
    )

    @BeforeEach
    fun prepareAnnouncements() {
        clearMocks(announcements)
        every { announcements.findAll() } returns listOf(stored)
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceAnnouncementsView::class.java)
    }

    @Test
    fun `should deny the announcements to a main cabinet principal without service authority`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            CabinetPrincipal(userId = 1, kind = UserKind.MULTIPLE_ROLE),
            null,
            listOf(SimpleGrantedAuthority("ROLE_MULTIPLE_ROLE")),
        )

        UI.getCurrent().navigate(ServiceAnnouncementsView::class.java)

        assertEquals(ServiceAccessDeniedView::class.java, currentView)
    }

    @Test
    fun `should open the announcements page from the service header`() {
        UI.getCurrent().navigate(ServiceHomeView::class.java)

        UI.getCurrent()._get<RouterLink> { text = "Объявления" }._click()

        assertEquals(ServiceAnnouncementsView::class.java, currentView)
    }

    @Test
    fun `should show the period in the user time zone, the kind and the text of each announcement`() {
        assertEquals(listOf(listOf("09.03.2026 12:00", "15.03.2026 00:00", "Предупреждение", "Плановые работы в субботу")), rowTexts())
    }

    @Test
    fun `should show an empty state when there are no announcements`() {
        every { announcements.findAll() } returns emptyList()

        UI.getCurrent().navigate(ServiceHomeView::class.java)
        UI.getCurrent().navigate(ServiceAnnouncementsView::class.java)

        val table = UI.getCurrent()._get<Table>().element.textRecursively
        assertTrue("Объявлений нет" in table)
        assertFalse(stored.text in table)
    }

    @Test
    fun `should keep the title and adding and show a load error if the announcements fail to load`() {
        every { announcements.findAll() } throws IllegalStateException("Announcement storage unavailable")

        UI.getCurrent().navigate(ServiceHomeView::class.java)
        UI.getCurrent().navigate(ServiceAnnouncementsView::class.java)

        assertEquals("Объявления", UI.getCurrent()._get<H1>().text)
        assertTrue(UI.getCurrent()._get<Button> { text = "Добавить" }.isEnabled)
        val page = UI.getCurrent()._get<ServiceAnnouncementsView>().element.textRecursively
        assertEquals(1, Regex("Не удалось загрузить").findAll(page).count())
    }

    @Test
    fun `should create the entered announcement with the period read in the user time zone and show it in the list`() {
        val added = Announcement(
            id = AnnouncementId(2),
            text = "Новый сезон",
            kind = AnnouncementKind.Info,
            shownFrom = Instant.parse("2026-04-01T06:00:00Z"),
            shownUntil = Instant.parse("2026-04-08T06:00:00Z"),
        )
        every { announcements.findAll() } returns listOf(added, stored)
        every {
            announcements.create(
                text = added.text,
                kind = AnnouncementKind.Info,
                shownFrom = added.shownFrom,
                shownUntil = added.shownUntil,
            )
        } returns AnnouncementSave.Saved(added)
        openAddDialog()
        textField()._value = added.text
        startPicker().value = LocalDateTime.of(2026, 4, 1, 9, 0)
        endPicker().value = LocalDateTime.of(2026, 4, 8, 9, 0)

        openDialog()._get<Button> { text = "Добавить" }._click()

        verify(exactly = 1) {
            announcements.create(
                text = added.text,
                kind = AnnouncementKind.Info,
                shownFrom = added.shownFrom,
                shownUntil = added.shownUntil,
            )
        }
        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Объявление добавлено", lastToastTitle())
        assertEquals(listOf("Новый сезон", stored.text), rowTexts().map { row -> row[3] })
    }

    @Test
    fun `should require both moments of the period before saving`() {
        openAddDialog()
        textField()._value = "Текст"

        openDialog()._get<Button> { text = "Добавить" }._click()

        assertEquals("Укажите начало показа", startPicker().errorMessage)
        assertEquals("Укажите окончание показа", endPicker().errorMessage)
        verify(exactly = 0) { announcements.create(any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @CsvSource("BlankText, Укажите текст", "TextTooLong, Текст длиннее 300 символов")
    fun `should keep the dialog open with an error on the text if the text is refused`(reason: AnnouncementRejection, error: String) {
        every { announcements.create(any(), any(), any(), any()) } returns AnnouncementSave.Rejected(reason)
        openFilledAddDialog()

        openDialog()._get<Button> { text = "Добавить" }._click()

        assertEquals(error, textField().errorMessage)
        assertFalse(endPicker().isInvalid)
    }

    @Test
    fun `should keep the dialog open with an error on the end if the period is empty`() {
        every { announcements.create(any(), any(), any(), any()) } returns AnnouncementSave.Rejected(AnnouncementRejection.EmptyPeriod)
        openFilledAddDialog()

        openDialog()._get<Button> { text = "Добавить" }._click()

        assertEquals("Окончание показа должно быть позже начала", endPicker().errorMessage)
        assertFalse(textField().isInvalid)
    }

    @Test
    fun `should change an announcement starting from its current values`() {
        val changed = stored.copy(text = "Работы перенесены")
        every { announcements.findAll() } returns listOf(changed)
        every {
            announcements.update(
                stored.id,
                text = changed.text,
                kind = AnnouncementKind.Warning,
                shownFrom = stored.shownFrom,
                shownUntil = stored.shownUntil,
            )
        } returns AnnouncementSave.Saved(changed)
        announcementMenu()._clickItemWithCaption("Изменить")
        assertEquals(LocalDateTime.of(2026, 3, 9, 12, 0), startPicker().value)
        textField()._value = changed.text

        openDialog()._get<Button> { text = "Сохранить" }._click()

        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Объявление изменено", lastToastTitle())
        assertEquals(listOf(listOf("09.03.2026 12:00", "15.03.2026 00:00", "Предупреждение", changed.text)), rowTexts())
    }

    @Test
    fun `should close the dialog, report the deleted announcement and reread the list if it is gone`() {
        every { announcements.findAll() } returns emptyList()
        every { announcements.update(any(), any(), any(), any(), any()) } returns AnnouncementSave.NotFound
        announcementMenu()._clickItemWithCaption("Изменить")

        openDialog()._get<Button> { text = "Сохранить" }._click()

        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Объявление уже удалено", lastToastTitle())
        assertTrue("Объявлений нет" in UI.getCurrent()._get<Table>().element.textRecursively)
    }

    @Test
    fun `should delete an announcement after the confirmation`() {
        every { announcements.findAll() } returns emptyList()
        every { announcements.delete(stored.id) } returns true
        announcementMenu()._clickItemWithCaption("Удалить")

        openDialog()._get<Button> { text = "Удалить" }._click()

        verify(exactly = 1) { announcements.delete(stored.id) }
        assertEquals("Объявление удалено", lastToastTitle())
        assertTrue("Объявлений нет" in UI.getCurrent()._get<Table>().element.textRecursively)
    }

    @Test
    fun `should report an already deleted announcement`() {
        every { announcements.delete(stored.id) } returns false
        announcementMenu()._clickItemWithCaption("Удалить")

        openDialog()._get<Button> { text = "Удалить" }._click()

        assertEquals("Объявление уже удалено", lastToastTitle())
    }

    private fun announcementMenu(): ContextMenu {
        val trigger = UI.getCurrent()._find<Button>().single { button ->
            button.element.getAttribute("aria-label") == "Действия с объявлением «${stored.text}»"
        }
        return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
    }

    private fun openAddDialog() {
        UI.getCurrent()._get<Button> { text = "Добавить" }._click()
    }

    private fun openFilledAddDialog() {
        openAddDialog()
        textField()._value = "Текст"
        startPicker().value = LocalDateTime.of(2026, 4, 1, 9, 0)
        endPicker().value = LocalDateTime.of(2026, 4, 8, 9, 0)
    }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun textField(): TextField = openDialog()._get<TextField>()

    private fun startPicker(): DateTimePicker = openDialog()._find<DateTimePicker>().first()

    private fun endPicker(): DateTimePicker = openDialog()._find<DateTimePicker>().last()

    /** Returns the texts of the cells of each row, without the menu cell. */
    private fun rowTexts(): List<List<String>> = UI.getCurrent()._get<Table>()._get<TableBody>()._find<TableRow>()
        .map { row -> row._find<TableCell>().take(4).map { cell -> cell.element.textRecursively } }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text
}
