package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class CabinetHeadersTests : MockSpringVaadinTests() {
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
    fun `should mark only the heading of a role section on the page of the role`() {
        signIn(fixtures.multipleRoleUser { roles { developer { data = developerData {} } } })

        UI.getCurrent().navigate("developer")

        assertEquals(listOf("Разработчик"), currentMenuLinks())
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

    /** Labels of the menu links marked as the current page. */
    private fun currentMenuLinks(): List<String> = UI.getCurrent()._find<RouterLink>()
        .filter { link -> link.element.getAttribute("aria-current") == "page" }
        .map { link -> link.text.ifEmpty { link._get<Span>().text } }

    private fun linkHref(label: String): String = UI.getCurrent()._get<RouterLink> { text = label }.href

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
    }
}
