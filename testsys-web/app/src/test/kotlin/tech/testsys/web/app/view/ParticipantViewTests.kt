package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.router.HasDynamicTitle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.StudyFixtures
import java.time.Duration

@SpringBootTest
class ParticipantViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var study: StudyFixtures

    @Test
    fun `should title the browser tab of the participant cabinet`() {
        signIn(study.participant(listOf(study.runningContest())))

        UI.getCurrent().navigate(ParticipantView::class.java)

        assertEquals("Кабинет Участника", assertInstanceOf(HasDynamicTitle::class.java, UI.getCurrent().currentView).pageTitle)
        assertEquals("Кабинет Участника", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show contest schedule with a separate remaining time column and open its page`() {
        val contest = study.runningContest(name = "Тур участника")
        val participant = study.participant(listOf(contest))
        study.participantEntry(participant, contest)
        signIn(participant)

        UI.getCurrent().navigate(ParticipantView::class.java)

        val text = StudyPages.table("Название").element.textRecursively
        assertTrue("Тур участника" in text && "Осталось" in text && "Состояние" in text, text)
        StudyPages.clickRow("Название")
        assertEquals(ParticipantContestView::class.java, currentView)
    }

    @Test
    fun `should keep completed contests available for viewing`() {
        val contest = study.contest(startsAt = StudyFixtures.PAST, contestDuration = Duration.ofHours(1))
        signIn(study.participant(listOf(contest)))

        UI.getCurrent().navigate(ParticipantView::class.java)

        assertTrue("Завершён" in StudyPages.rowTexts("Название").single())
    }
}
