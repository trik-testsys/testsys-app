package tech.testsys.web.app.view

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
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class SupervisorCommunityViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var communities: CommunityRepository

    @Autowired
    private lateinit var managerInvites: ManagerCommunityInviteRepository

    @Test
    fun `should edit a community created by another user`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val community = fixtures.community(name = "Исходное сообщество")
        openSupervisorCommunity(community.id)

        UI.getCurrent()._get<Button> { text = "Изменить" }._click()
        field("Название")._value = "Новое название"
        UI.getCurrent()._get<Button> { text = "Сохранить" }._click()

        assertEquals("Новое название", communities.findById(community.id)?.data?.name)
        assertEquals(SupervisorCommunityView::class.java, currentView)
        assertTrue("Новое название" in UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show a not found screen for an unknown community`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))

        openSupervisorCommunity(CommunityId(Long.MAX_VALUE))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should replace an invite code in a community created by another user`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val community = fixtures.community()
        val inviteId = community.data.managerInvite.id
        val before = checkNotNull(managerInvites.findById(inviteId)).data.codeHash
        openSupervisorCommunity(community.id)

        val trigger = UI.getCurrent()._find<Button>().single { button ->
            button.element.getAttribute("aria-label") == "Действия с Кодом-приглашением для Роли Организатор"
        }
        val menu = checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
        menu._clickItemWithCaption("Заменить")
        UI.getCurrent()._get<Button> { text = "Заменить" }._click()

        val after = checkNotNull(managerInvites.findById(inviteId)).data.codeHash
        assertNotEquals(before, after)
    }

    private fun field(label: String): TextField = UI.getCurrent()._find<TextField>().single { it.ariaLabel.orElse(null) == label }
}
