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
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.administrator.AdministratorService
import javax.sql.DataSource

@SpringBootTest
class AdminCommunityViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var managerInvites: ManagerCommunityInviteRepository

    @Autowired
    private lateinit var developerInvites: DeveloperCommunityInviteRepository

    @Autowired
    private lateinit var contests: ContestRepository

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var observers: ObserverRepository

    @Autowired
    private lateinit var administratorService: AdministratorService

    @Autowired
    private lateinit var communities: CommunityRepository

    @Test
    fun `should show the not found screen for a missing community`() {
        signInAdministrator()

        open(CommunityId(Long.MAX_VALUE))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a community of another administrator`() {
        signInAdministrator()

        open(fixtures.community().id)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should lead the breadcrumbs to the cabinet and its communities`() {
        val community = fixtures.community(owner = signInAdministrator())

        open(community.id)

        val crumbs = UI.getCurrent()._get<Nav> { classes = "ts-crumbs" }._find<RouterLink>().associate { link -> link.text to link.href }
        assertEquals(mapOf("Главная" to "home", "Кабинет Администратора" to "admin", "Сообщества" to "admin/communities"), crumbs)
    }

    @Test
    fun `should show both invite codes of the community`() {
        val community = fixtures.community(owner = signInAdministrator())

        open(community.id)

        val text = invitesText()
        assertTrue(managerInvite(community).codeHash.value in text, text)
        assertTrue(developerInvite(community).codeHash.value in text, text)
    }

    @Test
    fun `should extend the invite code from its menu`() {
        val community = fixtures.community(owner = signInAdministrator())
        val before = developerInvite(community)
        open(community.id)

        inviteMenu("Разработчик")._clickItemWithCaption("Продлить")

        val after = developerInvite(community)
        assertEquals(before.codeHash, after.codeHash)
        assertTrue(after.expiresAt < before.expiresAt)
        assertEquals("Срок действия кода-приглашения продлён", lastToastTitle())
    }

    @Test
    fun `should replace the invite code after the confirmation`() {
        val community = fixtures.community(owner = signInAdministrator())
        val before = managerInvite(community).codeHash
        open(community.id)

        inviteMenu("Организатор")._clickItemWithCaption("Заменить")
        UI.getCurrent()._get<Button> { text = "Заменить" }._click()

        val after = managerInvite(community).codeHash
        assertNotEquals(before, after)
        assertTrue(after.value in invitesText())
        assertEquals("Код-приглашение заменён", lastToastTitle())
    }

    @Test
    fun `should create an observer of the chosen contests and show its access code`() {
        val community = fixtures.community(owner = signInAdministrator())
        sharedContest(community)
        open(community.id)
        val contest = administratorService.viewCommunityContests(community.id, Pagination(page = 0, size = 1)).content.single()

        UI.getCurrent()._get<Button> { text = "Создать наблюдателя" }._click()
        textField("Имя")._value = "Наблюдатель Анна"
        contestsField().value = setOf(contest)
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        val observer = observers.findByAccessToken(textField("Код-доступа").value)
        assertEquals("Наблюдатель Анна", observer?.data?.name)
        assertEquals(listOf(contest.id), observer?.data?.contests?.ids)
        assertEquals("Наблюдатель создан", lastToastTitle())
    }

    @Test
    fun `should show the name and description of the community in read-only fields`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")

        open(community.id)

        assertEquals("Кружок", textField("Название").value)
        assertEquals("Community", descriptionField().value)
        assertTrue(textField("Название").isReadOnly)
        assertTrue(descriptionField().isReadOnly)
    }

    @Test
    fun `should save the edited name and description and show the new name in the title`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        open(community.id)

        UI.getCurrent()._get<Button> { text = "Изменить" }._click()
        textField("Название")._value = "Робототехника"
        descriptionField()._value = ""
        UI.getCurrent()._get<Button> { text = "Сохранить" }._click()

        val saved = checkNotNull(communities.findById(community.id)).data
        assertEquals("Робототехника", saved.name)
        assertEquals("", saved.description)
        assertEquals("Сообщество «Робототехника»", UI.getCurrent()._find<H1>().single().text)
        assertEquals("Сообщество изменено", lastToastTitle())
    }

    @Test
    fun `should keep editing without saving if the edited name is blank`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        open(community.id)

        UI.getCurrent()._get<Button> { text = "Изменить" }._click()
        textField("Название")._value = " "
        UI.getCurrent()._get<Button> { text = "Сохранить" }._click()

        assertEquals("Кружок", checkNotNull(communities.findById(community.id)).data.name)
        assertFalse(textField("Название").isReadOnly)
    }

    @Test
    fun `should not show the users of another community of the same administrator`() {
        val administrator = signInAdministrator()
        val community = fixtures.community(owner = administrator)
        developerOf(community, name = "Разработчик Иван")
        developerOf(fixtures.community(owner = administrator), name = "Разработчик Олег")

        open(community.id)

        val text = UI.getCurrent()._find<Table>().single { table -> "Псевдоним" in table.element.textRecursively }.element.textRecursively
        assertTrue("Разработчик Иван" in text, text)
        assertTrue("Разработчик Олег" !in text, text)
    }

    @Test
    fun `should show the roles of each user in this community only`() {
        val administrator = signInAdministrator()
        val community = fixtures.community(owner = administrator)
        val other = fixtures.community(owner = administrator)
        fixtures.multipleRoleUser(name = "Иван") {
            roles {
                developer {
                    memberOf(listOf(community.id.value))
                    data = developerData {}
                }
                judge {
                    memberOf(listOf(community.id.value))
                    data = judgeData {}
                }
                manager {
                    memberOf(listOf(other.id.value))
                    data = managerData {}
                }
            }
        }

        open(community.id)

        val text = UI.getCurrent()._find<Table>().single { table -> "Псевдоним" in table.element.textRecursively }.element.textRecursively
        assertTrue("ИванРазработчикСудья" in text, text)
        assertTrue("Организатор" !in text, text)
    }

    private fun signInAdministrator(): MultipleRoleUser = fixtures.administrator().also { administrator -> signIn(administrator) }

    private fun developerOf(community: Community, name: String): MultipleRoleUser = fixtures.multipleRoleUser(name = name) {
        roles {
            developer {
                memberOf(listOf(community.id.value))
                data = developerData {}
            }
        }
    }

    private fun open(communityId: CommunityId) {
        UI.getCurrent().navigate(AdminCommunityView::class.java, communityParameters(communityId))
    }

    private fun sharedContest(community: Community) {
        // Versions come from the grader; no port stores them, so the test adds the row the contest refers to.
        val tag = fixtures.unique("tsv")
        dataSource.connection.use { connection ->
            val insert = "insert into ts_trik_studio_version (id, tag) select coalesce(max(id), 0) + 1, ? from ts_trik_studio_version"
            connection.prepareStatement(insert).use { statement ->
                statement.setString(1, tag)
                statement.executeUpdate()
            }
        }
        contests.save(
            contestData {
                owner(community.data.owner.id.value)
                name = "Весенний тур"
                description = ""
                trikStudioVersion(tag)
                sharedTo(listOf(community.id.value))
            },
        )
    }

    private fun managerInvite(community: Community): CommunityInviteData =
        checkNotNull(managerInvites.findById(community.data.managerInvite.id)).data

    private fun developerInvite(community: Community): CommunityInviteData =
        checkNotNull(developerInvites.findById(community.data.developerInvite.id)).data

    private fun invitesText(): String = UI.getCurrent()._find<Table>().first().element.textRecursively

    private fun inviteMenu(role: String): ContextMenu {
        val trigger = UI.getCurrent()._find<Button>().single { button ->
            button.element.getAttribute("aria-label") == "Действия с кодом-приглашением для роли $role"
        }
        return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
    }

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun descriptionField(): TextArea =
        UI.getCurrent()._find<TextArea>().single { field -> field.ariaLabel.orElse(null) == "Описание" }

    /** Returns the contests field of the open dialog, the only custom field in it. */
    @Suppress("UNCHECKED_CAST")
    private fun contestsField(): CustomField<Set<ContestVo>> =
        checkNotNull(UI.getCurrent()._get<Dialog>()._get<CustomField<*>>() as? CustomField<Set<ContestVo>>)
}
