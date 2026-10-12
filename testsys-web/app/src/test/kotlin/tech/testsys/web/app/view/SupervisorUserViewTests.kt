package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.StudyFixtures
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.service.CommunityVo

@SpringBootTest
class SupervisorUserViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var users: MultipleRoleUserRepository

    @Autowired
    private lateinit var communities: CommunityRepository

    @Autowired
    private lateinit var communityConfig: CommunityConfig

    @Autowired
    private lateinit var study: StudyFixtures

    @Test
    fun `should show an observer for viewing without modification actions`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val accessToken = fixtures.unique("observer-code")
        val observer = fixtures.observer(fixtures.community(), rawAccessToken = accessToken)

        open(observer.id.value)

        assertEquals(SupervisorUserView::class.java, currentView)
        assertEquals(accessToken, field("Код-доступа").value)
        assertTrue(field("Код-доступа").isReadOnly)
        assertTrue(UI.getCurrent()._find<Button> { text = "Включить в сообщество" }.isEmpty())
        assertTrue(UI.getCurrent()._find<Button> { text = "Удалить наблюдателя" }.isEmpty())
        assertTrue(UI.getCurrent()._find<Button> { text = "Назначить администратором" }.isEmpty())
    }

    @Test
    fun `should show the access code of a user with non-fixed roles in a read-only field`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val accessToken = fixtures.unique("member-code")
        val member = fixtures.multipleRoleUser(rawAccessToken = accessToken) {
            roles {
                developer {
                    memberOf(listOf(communityConfig.publicCommunityId.value))
                    data = developerData {}
                }
            }
        }

        open(member.id.value)

        assertEquals(accessToken, field("Код-доступа").value)
        assertTrue(field("Код-доступа").isReadOnly)
    }

    @Test
    fun `should show the access code of a participant in a read-only field`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val participant = study.participant(emptyList())

        open(participant.id.value)

        assertEquals(participant.data.accessTokenHash.value, field("Код-доступа").value)
        assertTrue(field("Код-доступа").isReadOnly)
    }

    @Test
    fun `should grant the administrator role to a user who is only in the public community`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val name = fixtures.unique("Разработчик")
        val member = fixtures.multipleRoleUser(name = name) {
            roles {
                developer {
                    memberOf(listOf(communityConfig.publicCommunityId.value))
                    data = developerData {}
                }
            }
        }
        open(member.id.value)

        UI.getCurrent()._get<Button> { text = "Назначить администратором" }._click()
        openDialog()._get<Button> { text = "Назначить" }._click()

        val administrator = checkNotNull(users.findById(member.id)).data.roles.filterIsInstance<Administrator>().single()
        val owned = communities.findByOwner(member.id)
        assertEquals(listOf("Сообщество $name"), owned.map { community -> community.data.name })
        assertEquals(owned.map { community -> community.id }, administrator.memberOf.ids)
        assertFalse(communityConfig.publicCommunityId in administrator.memberOf.ids)
        assertEquals(SupervisorUserView::class.java, currentView)
    }

    @Test
    fun `should not offer the administrator role to a user who already holds it`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val administrator = fixtures.administrator()
        fixtures.community(owner = administrator)

        open(administrator.id.value)

        assertEquals(1, UI.getCurrent()._find<Button> { text = "Включить в сообщество" }.size)
        assertTrue(UI.getCurrent()._find<Button> { text = "Назначить администратором" }.isEmpty())
    }

    @Test
    fun `should add a user to a foreign community in a newly granted role`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val original = fixtures.community()
        val target = fixtures.community(name = "Целевое сообщество")
        val member = fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(original.id.value))
                    data = developerData {}
                }
            }
        }
        open(member.id.value)

        UI.getCurrent()._get<Button> { text = "Включить в сообщество" }._click()
        choose("Сообщество", CommunityVo::id, target.id)
        choose("Роль", { role: CommunityRole -> role }, CommunityRole.Student)
        UI.getCurrent()._get<Button> { text = "Включить" }._click()

        val updated = checkNotNull(users.findById(member.id))
        val student = updated.data.roles.filterIsInstance<Student>().single()
        assertTrue(target.id in student.memberOf.ids)
        assertTrue(original.id in updated.data.roles.first { it !is Student }.memberOf.ids)
        assertEquals(SupervisorUserView::class.java, currentView)
    }

    @Test
    fun `should refuse a supervisor detail route`() {
        val supervisor = fixtures.userOf(UserKind.SUPERVISOR)
        signIn(supervisor)

        open(supervisor.id.value)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    private fun open(id: Long) {
        UI.getCurrent().navigate(SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, id.toString()))
    }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun field(label: String): TextField = UI.getCurrent()._find<TextField>().single { it.ariaLabel.orElse(null) == label }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, K> choose(label: String, key: (T) -> K, expected: K) {
        val select = UI.getCurrent()._get<Dialog>()._find<Select<*>>().single { it.ariaLabel.orElse(null) == label }
        val typed = checkNotNull(select as? Select<T?>)
        typed.value = typed.listDataView.items.toList().filterNotNull().single { item -> key(item) == expected }
    }
}
