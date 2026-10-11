package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.web.app.MockSpringVaadinTests

@SpringBootTest
class ObserverViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var observers: ObserverRepository

    @Test
    fun `should show each current competition and assigned contest pair`() {
        val contest = fixtures.contest(name = "Назначенный Тур")
        val manager = fixtures.multipleRoleUser()
        val first = fixtures.competition(manager, name = "Первое Соревнование", contests = listOf(contest))
        val second = fixtures.competition(manager, name = "Второе Соревнование", contests = listOf(contest))
        val observer = fixtures.observer(fixtures.community())
        signIn(observers.update(observer.withData { contests(listOf(contest.id.value)) }))

        UI.getCurrent().navigate(ObserverView::class.java)

        val rows = StudyPages.rowTexts("Соревнование")
        assertEquals(2, rows.size)
        assertTrue(rows.any { first.data.name in it && contest.data.name in it })
        assertTrue(rows.any { second.data.name in it && contest.data.name in it })
        val download = UI.getCurrent()._find<NativeButton>()
            .single { it.element.getAttribute("aria-label")?.startsWith("Скачать все результаты") == true }
        assertTrue(download.isEnabled)
    }

    @Test
    fun `should filter pairs by competition name without showing another competition`() {
        val contest = fixtures.contest(name = "Общий Тур")
        val manager = fixtures.multipleRoleUser()
        fixtures.competition(manager, name = "Нужное Соревнование", contests = listOf(contest))
        fixtures.competition(manager, name = "Чужое Соревнование", contests = listOf(contest))
        val observer = fixtures.observer(fixtures.community())
        signIn(observers.update(observer.withData { contests(listOf(contest.id.value)) }))
        UI.getCurrent().navigate(ObserverView::class.java)

        StudyPages.textField("Соревнование")._value = "Нужное"
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val rows = StudyPages.rowTexts("Соревнование")
        assertEquals(1, rows.size)
        assertTrue("Нужное Соревнование" in rows.single())
        val download = UI.getCurrent()._find<NativeButton>()
            .single { it.element.getAttribute("aria-label")?.startsWith("Скачать все результаты") == true }
        assertTrue(download.isEnabled)
    }

    @Test
    fun `should preserve a space in the competition name filter`() {
        val contest = fixtures.contest(name = "Общий Тур")
        val manager = fixtures.multipleRoleUser()
        fixtures.competition(manager, name = "С пробелом", contests = listOf(contest))
        fixtures.competition(manager, name = "БезПробела", contests = listOf(contest))
        val observer = fixtures.observer(fixtures.community())
        signIn(observers.update(observer.withData { contests(listOf(contest.id.value)) }))
        UI.getCurrent().navigate(ObserverView::class.java)

        StudyPages.textField("Соревнование")._value = " "
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val rows = StudyPages.rowTexts("Соревнование")
        assertEquals(1, rows.size)
        assertTrue("С пробелом" in rows.single())
    }

    @Test
    fun `should preserve a space in the contest name filter`() {
        val withSpace = fixtures.contest(name = "С пробелом")
        val withoutSpace = fixtures.contest(name = "БезПробела")
        val manager = fixtures.multipleRoleUser()
        fixtures.competition(manager, name = "Соревнование", contests = listOf(withSpace, withoutSpace))
        val observer = fixtures.observer(fixtures.community())
        signIn(observers.update(observer.withData { contests(listOf(withSpace.id.value, withoutSpace.id.value)) }))
        UI.getCurrent().navigate(ObserverView::class.java)

        StudyPages.textField("Тур")._value = " "
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val rows = StudyPages.rowTexts("Соревнование")
        assertEquals(1, rows.size)
        assertTrue("С пробелом" in rows.single())
    }

    @Test
    fun `should show the empty state when no assigned contest belongs to a competition`() {
        signIn(fixtures.observer(fixtures.community()))

        UI.getCurrent().navigate(ObserverView::class.java)

        assertTrue("Туров пока нет" in StudyPages.pageText())
    }
}
