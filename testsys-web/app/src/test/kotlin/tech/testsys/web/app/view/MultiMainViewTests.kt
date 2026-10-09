package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.Manager
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.MockSpringVaadinTests
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import java.time.Instant

@SpringBootTest
class MultiMainViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var managerInvites: ManagerCommunityInviteRepository

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var communityConfig: CommunityConfig

    @Test
    fun `should list the roles of the user and the communities with the roles in each`() {
        val shared = fixtures.community(name = "Общее сообщество")
        val studentOnly = fixtures.community(name = "Сообщество учеников")
        signIn(
            fixtures.multipleRoleUser {
                roles {
                    developer {
                        memberOf(listOf(shared.id.value))
                        data = developerData {}
                    }
                    student {
                        memberOf(listOf(shared.id.value, studentOnly.id.value))
                        data = studentData {}
                    }
                }
            },
        )

        UI.getCurrent().navigate(MultiMainView::class.java)

        val (roles, communities) = UI.getCurrent()._find<Table>().map { table -> table.element.textRecursively }
        assertTrue("Разработчик" in roles && "Ученик" in roles)
        assertTrue("Общее сообщество" in communities && "Разработчик, Ученик" in communities)
        assertTrue("Сообщество учеников" in communities)
    }

    @Test
    fun `should join the community by the invite code and show it`() {
        val community = fixtures.community(name = "Новое сообщество")
        val user = fixtures.multipleRoleUser()
        signIn(user)
        UI.getCurrent().navigate(MultiMainView::class.java)

        inviteCodeField()._value = checkNotNull(managerInvites.findById(community.data.managerInvite.id)).data.codeHash.value
        UI.getCurrent()._get<Button> { text = "Присоединиться" }._click()

        val manager = checkNotNull(multipleRoleUsers.findById(user.id)).data.roles.filterIsInstance<Manager>().single()
        assertEquals(setOf(communityConfig.publicCommunityId, community.id), manager.memberOf.ids.toSet())
        assertEquals(MultiMainView::class.java, currentView)
        assertTrue("Организатор" in UI.getCurrent()._find<Table>().first().element.textRecursively)
        assertTrue("Новое сообщество" in UI.getCurrent()._find<Table>().last().element.textRecursively)
        assertTrue("Организатор" in menuColumnHeadings())
    }

    @Test
    fun `should tell that the invite code is not valid`() {
        signIn(fixtures.multipleRoleUser())
        UI.getCurrent().navigate(MultiMainView::class.java)

        inviteCodeField()._value = fixtures.unique("code")
        UI.getCurrent()._get<Button> { text = "Присоединиться" }._click()

        assertEquals("Код-приглашение недействителен", lastToastTitle())
    }

    @Test
    fun `should tell that the invite code has expired`() {
        val community = fixtures.community(name = "Сообщество с истёкшим кодом")
        val invite = checkNotNull(managerInvites.findById(community.data.managerInvite.id))
        managerInvites.update(invite.withData { expiresAt = Instant.EPOCH })
        signIn(fixtures.multipleRoleUser())
        UI.getCurrent().navigate(MultiMainView::class.java)

        inviteCodeField()._value = invite.data.codeHash.value
        UI.getCurrent()._get<Button> { text = "Присоединиться" }._click()

        assertEquals("Срок Кода-приглашения истёк", lastToastTitle())
    }

    @Test
    fun `should open the page of a role from its row`() {
        signIn(fixtures.multipleRoleUser { roles { developer { data = developerData {} } } })
        UI.getCurrent().navigate(MultiMainView::class.java)
        val row = UI.getCurrent()._find<Table>().first()._get<TableBody>()._get<TableRow>()

        row._fireDomEvent("click", acceptedClick(row))

        assertEquals(DeveloperView::class.java, currentView)
    }

    @Test
    fun `should show empty roles and communities of a user without roles`() {
        signIn(fixtures.multipleRoleUser())

        UI.getCurrent().navigate(MultiMainView::class.java)

        val (roles, communities) = UI.getCurrent()._find<Table>().map { table -> table.element.textRecursively }
        assertTrue("Ролей пока нет" in roles)
        assertTrue("Сообществ пока нет" in communities)
    }

    private fun inviteCodeField(): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == "Код-приглашение" }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    /** Headings of the columns of the header menu. */
    private fun menuColumnHeadings(): List<String> = UI.getCurrent()._find<Div> { classes = "ts-mega__col" }
        .map { column -> column.children.findFirst().orElseThrow().element.textRecursively }

    /** Data of a click on [row] as the client sends it when the click passes every filter of the row. */
    private fun acceptedClick(row: TableRow): ObjectNode = ObjectMapper().createObjectNode().apply {
        row.element.node.getFeature(ElementListenerMap::class.java).getExpressions("click")
            .forEach { expression -> put(expression, true) }
    }
}
