package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._setValue
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.router.HasDynamicTitle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.AccessTokenHasher
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.components.texts.currentLocalization

@SpringBootTest
class ManagerCompetitionViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var competitions: CompetitionRepository

    @Autowired
    private lateinit var participants: ParticipantRepository

    @Autowired
    private lateinit var accessTokenHasher: AccessTokenHasher

    @Autowired
    private lateinit var managerService: ManagerService

    @Test
    fun `should export the created participants with their issued codes and escaped nicknames`() {
        val competition = fixtures.competition(owner = signInManager())
        participantOf(competition)
        val issued = managerService.createParticipants(competition.id, participantCount = 1).single()
        val renamed = issued.copy(participant = issued.participant.copy(name = "A;\"B\"\r\nC"))

        val csv = participantsCsv(listOf(renamed), currentLocalization().pageManager)

        assertEquals(
            "${Char(0xFEFF)}ID;Псевдоним;Код-доступа\r\n${issued.participant.id.value};\"A;\"\"B\"\"\r\nC\";${issued.accessToken}\r\n",
            csv,
        )
        assertEquals(issued.participant.id, participants.findByAccessToken(issued.accessToken)?.id)
    }

    @Test
    fun `should save the name and description through the details editor`() {
        val entity = fixtures.competition(owner = signInManager())
        open(entity.id)
        clickButton("Изменить")
        textField("Название")._value = "Новое название"
        UI.getCurrent()._get<TextArea>()._value = "Описание группы"

        clickButton("Сохранить")

        val saved = checkNotNull(competitions.findById(entity.id))
        assertEquals("Новое название", saved.data.name)
        assertEquals("Описание группы", saved.data.description)
        assertTrue(textField("Название").isReadOnly)
        assertEquals("Соревнование изменено", lastToastTitle())
    }

    @Test
    fun `should show the not found screen for a missing competition`() {
        signInManager()

        open(CompetitionId(Long.MAX_VALUE))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a competition of another manager`() {
        signInManager()

        open(fixtures.competition(owner = fixtures.multipleRoleUser()).id)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the participants with their nicknames and without access codes`() {
        val competition = fixtures.competition(owner = signInManager(), name = "Кубок")
        val participant = participantOf(competition)

        open(competition.id)

        val text = participantsText()
        assertTrue(participant.data.name in text, text)
        assertFalse(participant.data.accessTokenHash.value in pageText())
        assertFalse("Код-доступа" in text, text)
        assertEquals("Соревнование «Кубок»", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should title the browser tab of the competition`() {
        open(fixtures.competition(owner = signInManager(), name = "Кубок").id)

        assertEquals("Соревнование", assertInstanceOf(HasDynamicTitle::class.java, UI.getCurrent().currentView).pageTitle)
    }

    @Test
    fun `should create the given number of participants and show them`() {
        val competition = fixtures.competition(owner = signInManager())
        open(competition.id)

        UI.getCurrent()._get<Button> { text = "Создать Участников" }._click()
        UI.getCurrent()._get<Dialog>()._get<IntegerField>()._value = 3
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        val created = checkNotNull(competitions.findById(competition.id)).data.participants.ids
        assertEquals(3, created.size)
        assertTrue(created.all { id -> "st${id.value}" in participantsText() })
        assertEquals("Участники созданы", lastToastTitle())
    }

    @Test
    fun `should warn to download the created participants in a dialog right after the creation and only then`() {
        val competition = fixtures.competition(owner = signInManager())
        open(competition.id)

        UI.getCurrent()._get<Button> { text = "Создать Участников" }._click()
        UI.getCurrent()._get<Dialog>()._get<IntegerField>()._value = 2
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        val issued = UI.getCurrent()._get<Dialog>()
        val shown = issued.element.textRecursively
        assertTrue("Созданные Участники" in shown, shown)
        assertTrue("Скачайте файл сейчас" in shown, shown)
        assertTrue("Скачать CSV" in shown, shown)
        assertFalse("Скачайте файл сейчас" in pageText())

        issued._get<Button> { text = "Закрыть" }._click()

        assertTrue(UI.getCurrent()._find<Dialog>().isEmpty())
    }

    @Test
    fun `should close the dialog of the created participants with a destructive action after a cooldown`() {
        val competition = fixtures.competition(owner = signInManager())
        open(competition.id)
        UI.getCurrent()._get<Button> { text = "Создать Участников" }._click()
        UI.getCurrent()._get<Dialog>()._get<IntegerField>()._value = 2

        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        // Read before the next lookup: every Karibu lookup runs a round trip that drops the pending calls.
        val calls = UI.getCurrent().internals.let { internals ->
            internals.stateTree.runExecutionsBeforeClientResponse()
            internals.dumpPendingJavaScriptInvocations()
        }
        val close = UI.getCurrent()._get<Dialog>()._get<Button> { text = "Закрыть" }
        val cooldowns = calls.filter { call -> call.owner == close.element.node && "data-ts-cooldown" in call.invocation.expression }
        assertEquals("destructive", close.element.getAttribute("data-ts-role"))
        assertEquals(listOf<Any?>(3), cooldowns.map { call -> call.invocation.parameters.first() })
    }

    @Test
    fun `should delete the selected participants after the confirmation with one notification`() {
        val competition = fixtures.competition(owner = signInManager())
        val first = participantOf(competition)
        val second = participantOf(competition)
        val kept = participantOf(competition)
        open(competition.id)

        rowCheckboxes()[0]._setValue(true)
        rowCheckboxes()[1]._setValue(true)
        deleteSelectedButton()._click()
        UI.getCurrent()._get<Dialog>()._get<Button> { text = "Удалить" }._click()

        assertEquals(null, participants.findById(first.id))
        assertEquals(null, participants.findById(second.id))
        assertEquals(kept.id, participants.findById(kept.id)?.id)
        assertEquals(listOf("2 Участника удалены"), toastTitles())
    }

    @Test
    fun `should keep the selected participants if the batch deletion is cancelled`() {
        val competition = fixtures.competition(owner = signInManager())
        val participant = participantOf(competition)
        open(competition.id)

        rowCheckboxes().single()._setValue(true)
        deleteSelectedButton()._click()
        UI.getCurrent()._get<Button> { text = "Отменить" }._click()

        assertEquals(participant.id, participants.findById(participant.id)?.id)
        assertTrue(toastTitles().isEmpty())
    }

    @Test
    fun `should disable the batch deletion until a participant is selected`() {
        val competition = fixtures.competition(owner = signInManager())
        participantOf(competition)
        open(competition.id)

        val wasEnabled = deleteSelectedButton().isEnabled
        rowCheckboxes().single()._setValue(true)

        assertFalse(wasEnabled)
        assertTrue(deleteSelectedButton().isEnabled)
    }

    @Test
    fun `should delete a participant after the confirmation`() {
        val competition = fixtures.competition(owner = signInManager())
        val participant = participantOf(competition)
        open(competition.id)

        participantMenu(participant)._clickItemWithCaption("Удалить")
        UI.getCurrent()._get<Button> { text = "Удалить" }._click()

        assertEquals(null, participants.findById(participant.id))
        assertTrue(participant.data.name !in participantsText())
        assertEquals("Участник удалён", lastToastTitle())
    }

    @Test
    fun `should keep a participant if the deletion is cancelled`() {
        val competition = fixtures.competition(owner = signInManager())
        val participant = participantOf(competition)
        open(competition.id)

        participantMenu(participant)._clickItemWithCaption("Удалить")
        UI.getCurrent()._get<Button> { text = "Отменить" }._click()

        assertEquals(participant.id, participants.findById(participant.id)?.id)
        assertEquals(0, UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.size)
    }

    @Test
    fun `should add a contest shared to a community of the manager`() {
        val community = fixtures.community()
        val manager = fixtures.grantRole(fixtures.multipleRoleUser(), CommunityRole.Manager, community).also { manager -> signIn(manager) }
        fixtures.contest(name = "Финал", sharedTo = listOf(community))
        val competition = fixtures.competition(owner = manager)
        open(competition.id)
        val contest = managerService.viewAvailableContests(Pagination(page = 0, size = 1)).content.single()

        UI.getCurrent()._get<Button> { text = "Добавить Тур" }._click()
        contestField().value = contest
        UI.getCurrent()._get<Button> { text = "Добавить" }._click()

        assertEquals(listOf(contest.id), checkNotNull(competitions.findById(competition.id)).data.contests.ids)
        assertTrue("Финал" in UI.getCurrent()._find<Table>().last().element.textRecursively)
        assertEquals("Тур добавлен в Соревнование", lastToastTitle())
    }

    private fun signInManager(): MultipleRoleUser =
        fixtures.grantRole(fixtures.multipleRoleUser(), CommunityRole.Manager).also { manager -> signIn(manager) }

    private fun participantOf(competition: Competition): Participant {
        val code = accessTokenHasher.hash(fixtures.unique("code"))
        return participants.saveToCompetition(competitionId = competition.id, accessTokenHashes = listOf(code)) { id -> "st${id.value}" }
            .single()
    }

    private fun open(competitionId: CompetitionId) {
        UI.getCurrent().navigate(ManagerCompetitionView::class.java, competitionParameters(competitionId))
    }

    private fun participantsTable(): Table =
        UI.getCurrent()._find<Table>().single { table -> "Последний вход" in table.element.textRecursively }

    private fun participantsText(): String = participantsTable().element.textRecursively

    private fun pageText(): String = UI.getCurrent().currentView.element.textRecursively

    /** Returns the row checkboxes of the participants table in row order, without the header checkbox. */
    private fun rowCheckboxes(): List<Checkbox> = participantsTable()._find<Checkbox>().drop(1)

    private fun deleteSelectedButton(): Button = UI.getCurrent()._get<Button> { text = "Удалить выбранных" }

    private fun toastTitles(): List<String> = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.map { title -> title.text }

    private fun participantMenu(participant: Participant): ContextMenu {
        val trigger = UI.getCurrent()._find<Button>().single { button ->
            button.element.getAttribute("aria-label") == "Действия с Участником «${participant.data.name}»"
        }
        return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
    }

    /** Returns the contest field of the open dialog, the only custom field in it. */
    @Suppress("UNCHECKED_CAST")
    private fun contestField(): CustomField<ContestVo?> =
        checkNotNull(UI.getCurrent()._get<Dialog>()._get<CustomField<*>>() as? CustomField<ContestVo?>)
}
