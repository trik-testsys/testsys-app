package tech.testsys.web.app.view

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class CabinetHeadersTests : MockSpringVaadinTests() {
    private val logger = assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(CabinetSignIn::class.java))
    private val appender = ListAppender<ILoggingEvent>().apply { start() }

    @AfterEach
    fun detachAppender() {
        logger.detachAppender(appender)
    }

    @Test
    fun `should lead from the main section to the start page of a user with non-fixed roles`() {
        signIn(fixtures.userOf(UserKind.MULTIPLE_ROLE))

        UI.getCurrent().navigate(ProfileView::class.java)

        assertEquals("home", linkHref("Главная"))
    }

    @Test
    fun `should lead from the main section to the page of the fixed role`() {
        signIn(fixtures.userOf(UserKind.OBSERVER))

        UI.getCurrent().navigate(ObserverView::class.java)

        assertEquals("observer", linkHref("Главная"))
    }

    @Test
    fun `should show a menu section with the page of every role the user holds`() {
        signIn(
            fixtures.multipleRoleUser {
                roles {
                    developer { data = developerData {} }
                    student { data = studentData {} }
                }
            },
        )

        UI.getCurrent().navigate(MultiMainView::class.java)

        assertEquals(mapOf("Разработчик" to "developer", "Ученик" to "student"), menuSections())
    }

    @Test
    fun `should show the sections of a role as links of its menu section`() {
        signIn(fixtures.multipleRoleUser { roles { developer { data = developerData {} } } })

        UI.getCurrent().navigate(MultiMainView::class.java)

        val sectionLinks = UI.getCurrent()._find<RouterLink> { classes = "ts-mega__link" }
            .associate { link -> link._get<Span>().text to link.href }
        assertEquals(mapOf("Задачи" to "developer/tasks", "Туры" to "developer/contests"), sectionLinks)
    }

    @Test
    fun `should mark only the section a role page opens without one`() {
        signIn(fixtures.multipleRoleUser { roles { developer { data = developerData {} } } })

        UI.getCurrent().navigate("developer")

        assertEquals(listOf("Задачи"), currentMenuLinks())
    }

    @Test
    fun `should mark only the opened section of a role page`() {
        signIn(fixtures.multipleRoleUser { roles { developer { data = developerData {} } } })

        UI.getCurrent().navigate("developer/tasks")

        assertEquals(listOf("Задачи"), currentMenuLinks())
    }

    @Test
    fun `should show the section of the fixed role in the menu`() {
        signIn(fixtures.userOf(UserKind.PARTICIPANT))

        UI.getCurrent().navigate(ParticipantView::class.java)

        assertEquals(mapOf("Участник" to "participant"), menuSections())
    }

    @Test
    fun `should show the nickname with the profile and signing out to a user with non-fixed roles`() {
        signIn(fixtures.userOf(UserKind.MULTIPLE_ROLE, name = "Анна Петрова"))

        UI.getCurrent().navigate(MultiMainView::class.java)

        assertEquals("profile", linkHref("Профиль"))
        assertEquals(1, UI.getCurrent()._find<NativeButton> { text = "Выйти" }.size)
        assertEquals(1, UI.getCurrent()._find<Span> { text = "Анна Петрова" }.size)
    }

    @Test
    fun `should offer only signing out to a user with a fixed role`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))

        UI.getCurrent().navigate(SupervisorView::class.java)

        assertTrue(UI.getCurrent()._find<RouterLink> { text = "Профиль" }.isEmpty())
        assertEquals(1, UI.getCurrent()._find<NativeButton> { text = "Выйти" }.size)
    }

    @Test
    fun `should sign the user out`() {
        signIn(fixtures.userOf(UserKind.MULTIPLE_ROLE))
        UI.getCurrent().navigate(MultiMainView::class.java)

        UI.getCurrent()._get<NativeButton> { text = "Выйти" }._click()

        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should mark the nickname and offer the return before signing out to a supervisor signed in as a user`() {
        val member = fixtures.userOf(UserKind.MULTIPLE_ROLE, name = "Анна Петрова")
        CabinetSignIn.signIn(CabinetPrincipal.of(member).copy(impersonatedBy = fixtures.supervisor().id.value))

        UI.getCurrent().navigate(MultiMainView::class.java)

        assertEquals(1, UI.getCurrent()._find<Span> { text = "Анна Петрова$NBSP·${NBSP}вход${NBSP}Супервайзера" }.size)
        val menuActions = UI.getCurrent()._find<NativeButton>().map { button -> button.text }
            .filter { label -> label in setOf("Вернуться к Супервайзеру", "Выйти") }
        assertEquals(listOf("Вернуться к Супервайзеру", "Выйти"), menuActions)
    }

    @ParameterizedTest
    @CsvSource("Маша, М", "Анна Петрова, АП")
    fun `should show the initials of the nickname without the supervisor mark in the avatars of a user signed in by a supervisor`(
        name: String,
        initials: String,
    ) {
        val member = fixtures.userOf(UserKind.MULTIPLE_ROLE, name = name)
        CabinetSignIn.signIn(CabinetPrincipal.of(member).copy(impersonatedBy = fixtures.supervisor().id.value))

        UI.getCurrent().navigate(MultiMainView::class.java)

        val avatars = UI.getCurrent()._find<Span> { classes = "ts-header__avatar" }.map { avatar -> avatar.text }
        assertTrue(avatars.isNotEmpty())
        assertEquals(setOf(initials), avatars.toSet())
    }

    @Test
    fun `should return the supervisor to the details of the user they signed in as`() {
        val supervisor = fixtures.supervisor()
        val member = fixtures.userOf(UserKind.MULTIPLE_ROLE)
        CabinetSignIn.signIn(CabinetPrincipal.of(member).copy(impersonatedBy = supervisor.id.value))
        UI.getCurrent().navigate(MultiMainView::class.java)

        UI.getCurrent()._get<NativeButton> { text = "Вернуться к Супервайзеру" }._click()

        assertEquals(CabinetPrincipal.of(supervisor), CabinetSignIn.principal())
        assertEquals("supervisor/users/${member.id.value}", UI.getCurrent().internals.activeViewLocation.path)
    }

    @Test
    fun `should write the supervisor and user ids at INFO when the supervisor returns`() {
        val supervisor = fixtures.supervisor()
        val member = fixtures.userOf(UserKind.MULTIPLE_ROLE)
        CabinetSignIn.signIn(CabinetPrincipal.of(member).copy(impersonatedBy = supervisor.id.value))
        UI.getCurrent().navigate(MultiMainView::class.java)
        logger.addAppender(appender)

        UI.getCurrent()._get<NativeButton> { text = "Вернуться к Супервайзеру" }._click()

        val expected = "Supervisor ${supervisor.id.value} returned from user ${member.id.value}"
        assertEquals(listOf(Level.INFO to expected), appender.list.map { line -> line.level to line.formattedMessage })
    }

    @Test
    fun `should not offer the return to a user who signed in themselves`() {
        signIn(fixtures.userOf(UserKind.MULTIPLE_ROLE))

        UI.getCurrent().navigate(MultiMainView::class.java)

        assertTrue(UI.getCurrent()._find<NativeButton> { text = "Вернуться к Супервайзеру" }.isEmpty())
    }

    /** Labels of the menu links marked as the current page. */
    private fun currentMenuLinks(): List<String> = UI.getCurrent()._find<RouterLink>()
        .filter { link -> link.element.getAttribute("aria-current") == "page" && !link.hasClassName("ts-tab") }
        .map { link -> link.text.ifEmpty { link._get<Span>().text } }

    /** Returns the route of the header link named [label]; the breadcrumbs of the page may hold a link of the same name. */
    private fun linkHref(label: String): String = UI.getCurrent()._find<RouterLink> { text = label }
        .single { link -> link.parent.map { parent -> parent.element.getAttribute("class") }.orElse(null) != "ts-crumbs" }
        .href

    /** Headings of the menu columns with the routes of their links, excluding the start page link «Главная». */
    private fun menuSections(): Map<String, String> = UI.getCurrent()._find<RouterLink>()
        .filter { link -> link.text in SECTION_TITLES }
        .associate { link -> link.text to link.href }

    private companion object {
        val SECTION_TITLES = setOf(
            "Разработчик",
            "Организатор",
            "Администратор",
            "Судья",
            "Ученик",
            "Участник",
            "Наблюдатель",
            "Супервайзер",
        )

        // The supervisor mark is joined to the nickname by no-break spaces, so the avatar takes no initial from it.
        val NBSP = Char(0x00A0).toString()
    }
}
