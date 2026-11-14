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
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class ServiceAnnouncementsViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var announcements: Announcements

    @Autowired
    private lateinit var signIn: ServiceSignIn

    @Autowired
    private lateinit var headers: ServiceHeaders

    // The service UI shows and reads dates in the fallback zone Europe/Moscow (UTC+3) of the tests. The page splits
    // the lists by the fixed moment NOW: current announcements end after it, finished ones end by it or are closed.
    private val stored = Announcement(
        id = AnnouncementId(1),
        text = "Плановые работы в субботу",
        kind = AnnouncementKind.Warning,
        shownFrom = Instant.parse("2099-03-09T09:00:00Z"),
        shownUntil = Instant.parse("2099-03-14T21:00:00Z"),
    )

    private val running = Announcement(
        id = AnnouncementId(5),
        text = "Идёт регистрация на кубок",
        kind = AnnouncementKind.Info,
        shownFrom = Instant.parse("2026-03-11T09:00:00Z"),
        shownUntil = Instant.parse("2099-03-14T21:00:00Z"),
    )

    private val closedManually = Announcement(
        id = AnnouncementId(3),
        text = "Сбой проверки устранён",
        kind = AnnouncementKind.Info,
        shownFrom = Instant.parse("2026-03-09T09:00:00Z"),
        shownUntil = Instant.parse("2099-03-14T21:00:00Z"),
        closedAt = Instant.parse("2026-03-10T07:30:00Z"),
    )

    private val expired = Announcement(
        id = AnnouncementId(4),
        text = "Весенний кубок открыт",
        kind = AnnouncementKind.Warning,
        shownFrom = Instant.parse("2026-03-01T09:00:00Z"),
        shownUntil = Instant.parse("2026-03-05T21:00:00Z"),
    )

    @BeforeEach
    fun prepareAnnouncements() {
        clearMocks(announcements)
        every { announcements.findAll() } returns listOf(stored)
        signIn.signIn(SERVICE_TEST_SECRET)
        openPage()
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

        UI.getCurrent()._get<ServiceHomeView>()._get<RouterLink> { text = "Объявления" }._click()

        assertEquals(ServiceAnnouncementsView::class.java, currentView)
    }

    @Test
    fun `should show the period in the user time zone, the kind and the text of each current announcement`() {
        assertEquals(listOf(listOf("09.03.2099 12:00", "15.03.2099 00:00", "Предупреждение", "Плановые работы в субботу")), currentRows())
    }

    @Test
    fun `should list only the not closed announcements whose showing has not ended as current`() {
        every { announcements.findAll() } returns listOf(stored, running, closedManually, expired)

        openPage()

        assertEquals(listOf(stored.text, running.text), currentRows().map { row -> row[3] })
        assertEquals(listOf(closedManually.text, expired.text), historyRows().map { row -> row[4] })
    }

    @Test
    fun `should move an announcement whose showing ends at the current moment to the history`() {
        every { announcements.findAll() } returns listOf(expired.copy(shownUntil = NOW))

        openPage()

        assertTrue("Объявлений нет" in currentTable().element.textRecursively)
        assertEquals(listOf("По времени"), historyRows().map { row -> row[2] })
    }

    @Test
    fun `should show the reason and the moment of finishing of each finished announcement`() {
        every { announcements.findAll() } returns listOf(closedManually, expired)

        openPage()

        val expected = listOf(
            listOf("09.03.2026 12:00", "10.03.2026 10:30", "Вручную", "Информация", closedManually.text),
            listOf("01.03.2026 12:00", "06.03.2026 00:00", "По времени", "Предупреждение", expired.text),
        )
        assertEquals(expected, historyRows())
    }

    @Test
    fun `should offer no actions for the finished announcements`() {
        every { announcements.findAll() } returns listOf(closedManually, expired)

        openPage()

        val menus = UI.getCurrent()._find<Button>().filter { button ->
            button.element.getAttribute("aria-label").orEmpty().startsWith("Действия с объявлением")
        }
        assertEquals(emptyList<Button>(), menus)
    }

    @Test
    fun `should show an empty state when there are no announcements`() {
        every { announcements.findAll() } returns emptyList()

        openPage()

        val table = currentTable().element.textRecursively
        assertTrue("Объявлений нет" in table)
        assertFalse(stored.text in table)
    }

    @Test
    fun `should show an empty history when no announcement is finished`() {
        val table = historyTable().element.textRecursively

        assertTrue("Завершённых объявлений нет" in table)
        assertTrue("Сюда попадут объявления после окончания показа или закрытия." in table)
        assertFalse(stored.text in table)
    }

    @Test
    fun `should keep the title and adding and show a load error in both lists if the announcements fail to load`() {
        every { announcements.findAll() } throws IllegalStateException("Announcement storage unavailable")

        openPage()

        assertEquals("Объявления", UI.getCurrent()._get<ServiceAnnouncementsView>()._get<H1>().text)
        assertTrue(UI.getCurrent()._get<Button> { text = "Добавить" }.isEnabled)
        val page = UI.getCurrent()._get<ServiceAnnouncementsView>().element.textRecursively
        assertEquals(2, Regex("Не удалось загрузить").findAll(page).count())
    }

    @Test
    fun `should create the entered announcement with the period read in the user time zone and show it in the list`() {
        val added = Announcement(
            id = AnnouncementId(2),
            text = "Новый сезон",
            kind = AnnouncementKind.Info,
            shownFrom = Instant.parse("2099-04-01T06:00:00Z"),
            shownUntil = Instant.parse("2099-04-08T06:00:00Z"),
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
        startPicker().value = LocalDateTime.of(2099, 4, 1, 9, 0)
        endPicker().value = LocalDateTime.of(2099, 4, 8, 9, 0)

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
        assertEquals(listOf("Новый сезон", stored.text), currentRows().map { row -> row[3] })
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
        assertEquals(LocalDateTime.of(2099, 3, 9, 12, 0), startPicker().value)
        textField()._value = changed.text

        openDialog()._get<Button> { text = "Сохранить" }._click()

        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Объявление изменено", lastToastTitle())
        assertEquals(listOf(listOf("09.03.2099 12:00", "15.03.2099 00:00", "Предупреждение", changed.text)), currentRows())
    }

    @Test
    fun `should close the dialog, report the missing announcement and reread the list if it is not found`() {
        every { announcements.findAll() } returns emptyList()
        every { announcements.update(any(), any(), any(), any(), any()) } returns AnnouncementSave.NotFound
        announcementMenu()._clickItemWithCaption("Изменить")

        openDialog()._get<Button> { text = "Сохранить" }._click()

        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        assertEquals("Объявление не найдено", lastToastTitle())
        assertTrue("Объявлений нет" in currentTable().element.textRecursively)
    }

    @Test
    fun `should close an announcement now after the confirmation and move it to the history`() {
        every { announcements.findAll() } returns listOf(stored.copy(closedAt = NOW))
        every { announcements.close(stored.id, NOW) } returns true
        announcementMenu()._clickItemWithCaption("Закрыть")

        openDialog()._get<Button> { text = "Закрыть" }._click()

        verify(exactly = 1) { announcements.close(stored.id, NOW) }
        assertEquals("Объявление закрыто", lastToastTitle())
        assertTrue("Объявлений нет" in currentTable().element.textRecursively)
        assertEquals(listOf("Вручную"), historyRows().map { row -> row[2] })
    }

    @Test
    fun `should report an already finished announcement`() {
        every { announcements.close(stored.id, any()) } returns false
        announcementMenu()._clickItemWithCaption("Закрыть")

        openDialog()._get<Button> { text = "Закрыть" }._click()

        assertEquals("Объявление уже завершено", lastToastTitle())
    }

    private fun openPage() {
        // UI.removeAll keeps the route target, here the sign-in page shown before signing in; lookups of its
        // components are scoped to this page.
        UI.getCurrent().removeAll()
        UI.getCurrent().add(ServiceAnnouncementsView(headers, announcements, Clock.fixed(NOW, ZoneOffset.UTC)))
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

    private fun currentTable(): Table = UI.getCurrent()._find<Table>().first()

    private fun historyTable(): Table = UI.getCurrent()._find<Table>().last()

    /** Returns the texts of the cells of each current row, without the menu cell. */
    private fun currentRows(): List<List<String>> = rowTexts(currentTable()).map { row -> row.take(4) }

    /** Returns the texts of the cells of each history row. */
    private fun historyRows(): List<List<String>> = rowTexts(historyTable())

    private fun rowTexts(table: Table): List<List<String>> = table._get<TableBody>()._find<TableRow>()
        .map { row -> row._find<TableCell>().map { cell -> cell.element.textRecursively } }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    companion object {
        private val NOW = Instant.parse("2026-03-12T10:00:00Z")
    }
}
