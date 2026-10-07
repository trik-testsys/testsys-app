package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.FirstTestView
import tech.testsys.web.components.ItemTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.SecondTestView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.classes
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.text
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.findAllButtons
import tech.testsys.web.components.testTexts

class PageHeadTests : MockVaadinTests() {
    @Test
    fun `should select exact parameterized page tabs without changing class based default`() {
        com.vaadin.flow.component.UI.getCurrent().navigate(tech.testsys.web.components.ItemTestView::class.java, RouteParameters("id", "8"))
        val scope = PageHeadScope(testTexts, ItemTestView::class.java)
        scope.tabs(matchRouteParameters = true) {
            tab("Семь", tech.testsys.web.components.ItemTestView::class.java, RouteParameters("id", "7"))
            tab("Восемь", tech.testsys.web.components.ItemTestView::class.java, RouteParameters("id", "8"))
        }
        val head = scope.build("Кабинет")
        val links = head.findAll("ts-tab")
        assertEquals(listOf(null, "page"), links.map { it.element.getAttribute("aria-current") })
        assertEquals(listOf(false, true), links.map { "ts-tab--active" in it.element.classList })
    }

    @Test
    fun `should fall back to activeOn sections when exact tabs do not target the current page`() {
        val scope = PageHeadScope(testTexts, SecondTestView::class.java)
        scope.tabs(matchRouteParameters = true) {
            tab("Обзор", ItemTestView::class.java, RouteParameters("id", "7"), activeOn = setOf(SecondTestView::class.java))
            tab("Другое", FirstTestView::class.java)
        }

        val head = scope.build("Кабинет")

        val links = head.findAll("ts-tab")
        assertEquals(listOf("location", null), links.map { link -> link.element.getAttribute("aria-current") })
        assertEquals(listOf(true, false), links.map { link -> "ts-tab--active" in link.element.classList })
    }

    @Test
    fun `should put the head between the header and the page body`() {
        val main = buildTestPage { head("Весенний кубок") }

        val app = main.parent.orElseThrow().children.toList()
        assertTrue("ts-header" in app[0].classes())
        assertTrue("ts-page-head" in app[1].classes())
        assertEquals(main, app[2])
    }

    @Test
    fun `should end the breadcrumbs with the title as the current page`() {
        buildTestPage {
            head("Весенний кубок") {
                crumb("Соревнования", FirstTestView::class.java)
                crumb("Туры", SecondTestView::class.java)
            }
        }

        val crumbs = ui().find("ts-crumbs")
        assertEquals(testTexts.navigation.breadcrumbs, crumbs.element.getAttribute("aria-label"))
        val parts = crumbs.children.toList()
        assertEquals(5, parts.size)
        assertEquals("test/first", parts[0].element.getAttribute("href"))
        assertEquals("test/second", parts[2].element.getAttribute("href"))
        assertEquals("Весенний кубок", parts[4].element.text)
        assertEquals("page", parts[4].element.getAttribute("aria-current"))
        assertTrue("ts-crumbs__sep" in parts[1].classes())
        assertEquals("/", parts[1].element.text)
        assertEquals("true", parts[1].element.getAttribute("aria-hidden"))
    }

    @Test
    fun `should leave out the breadcrumbs without crumbs`() {
        buildTestPage { head("Весенний кубок") }

        assertTrue(ui().findAll("ts-crumbs").isEmpty())
        assertEquals("Весенний кубок", ui().find("ts-h1").element.text)
    }

    @Test
    fun `should show a badge next to the title`() {
        buildTestPage { head("Весенний кубок") { badge("Идёт", Tone.Success) } }

        assertEquals("Идёт", ui().find("ts-status").element.textRecursively)
    }

    @Test
    fun `should show a note next to the title`() {
        buildTestPage { head("Весенний кубок") { meta("ICPC · 10:00–15:00") } }

        assertEquals("ICPC · 10:00–15:00", ui().find("ts-page-head__meta").element.text)
    }

    @Test
    fun `should show actions of the regular size in the title row`() {
        buildTestPage { head("Весенний кубок") { actions { action("Объявление") } } }

        val button = findAllButtons(ui().find("ts-page-head__actions")).single()
        assertEquals("md", button.element.getAttribute("data-ts-size"))
    }

    @Test
    fun `should order the title, badges, notes and actions in the title row`() {
        buildTestPage {
            head("Весенний кубок") {
                badge("Идёт", Tone.Success)
                meta("ICPC · 10:00–15:00")
                actions { action("Объявление") }
            }
        }

        val children = ui().find("ts-page-head__title-row").children.toList()
        assertEquals(4, children.size)
        assertTrue("ts-h1" in children[0].classes())
        assertTrue("ts-status" in children[1].classes())
        assertTrue("ts-page-head__meta" in children[2].classes())
        assertTrue("ts-page-head__actions" in children[3].classes())
    }

    @Test
    fun `should link page tabs with route parameters`() {
        buildTestPage {
            head("Тур") {
                tabs {
                    tab("Обзор", ItemTestView::class.java, RouteParameters("id", "42"))
                    tab("Задачи", FirstTestView::class.java)
                }
            }
        }

        assertEquals(listOf("test/item/42", "test/first"), pageTabs().map { tab -> tab.element.getAttribute("href") })
        assertEquals(testTexts.navigation.sections, ui().find("ts-page-head").find("ts-tabs").element.getAttribute("aria-label"))
    }

    @Test
    fun `should mark the tab of the built page as current`() {
        buildTestPage(view = SecondTestView::class.java) { head("Тур") { twoTabs() } }

        assertTrue("ts-tab--active" in pageTabs()[1].classes())
        assertEquals("page", pageTabs()[1].element.getAttribute("aria-current"))
        assertNull(pageTabs()[0].element.getAttribute("aria-current"))
    }

    @Test
    fun `should mark no tab when the page has none`() {
        buildTestPage(view = ItemTestView::class.java) { head("Тур") { twoTabs() } }

        assertTrue(pageTabs().none { tab -> "ts-tab--active" in tab.classes() })
    }

    @Test
    fun `should mark the first of two tabs of the page`() {
        buildTestPage(view = FirstTestView::class.java) {
            head("Тур") {
                tabs {
                    tab("Все", FirstTestView::class.java)
                    tab("Тоже", FirstTestView::class.java)
                }
            }
        }

        assertEquals(listOf(true, false), pageTabs().map { tab -> "ts-tab--active" in tab.classes() })
    }

    @Test
    fun `should show tab counters above zero only`() {
        buildTestPage {
            head("Тур") {
                tabs {
                    tab("Вопросы", FirstTestView::class.java, count = 3, countKind = CounterKind.Attention)
                    tab("Задачи", SecondTestView::class.java, count = 0)
                }
            }
        }

        assertEquals("3", pageTabs()[0].find("ts-counter").element.text)
        assertTrue("ts-counter--danger" in pageTabs()[0].find("ts-counter").classes())
        assertTrue(pageTabs()[1].findAll("ts-counter").isEmpty())
    }

    @Test
    fun `should reject a single page tab`() {
        assertThrows<IllegalArgumentException> { buildTestPage { head("Тур") { tabs { tab("Обзор", FirstTestView::class.java) } } } }
    }

    @Test
    fun `should reject a negative tab count`() {
        assertThrows<IllegalArgumentException> {
            buildTestPage { head("Тур") { tabs { tab("Обзор", FirstTestView::class.java, count = -1) } } }
        }
    }

    @Test
    fun `should reject the head after a row`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {}
                head("Тур")
            }
        }
    }

    @Test
    fun `should reject the head after a block`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block { row { text("x") } }
                head("Тур")
            }
        }
    }

    @Test
    fun `should reject the head after a highlighted block`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                highlightBlock { row { text("x") } }
                head("Тур")
            }
        }
    }

    @Test
    fun `should reject a second head`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                head("Тур")
                head("Тур")
            }
        }
    }

    @Test
    fun `should reject repeated head actions`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                head("Тур") {
                    actions {}
                    actions {}
                }
            }
        }
    }

    @Test
    fun `should reject repeated page tabs`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                head("Тур") {
                    twoTabs()
                    twoTabs()
                }
            }
        }
    }

    @Test
    fun `should mark first matching section while keeping its parameterized link`() {
        buildTestPage(view = SecondTestView::class.java) {
            head("Тур") {
                tabs {
                    tab(
                        "Обзор",
                        ItemTestView::class.java,
                        RouteParameters("id", "42"),
                        activeOn = setOf(FirstTestView::class.java, SecondTestView::class.java),
                    )
                    tab("Другое", FirstTestView::class.java, activeOn = setOf(SecondTestView::class.java))
                }
            }
        }

        assertEquals(listOf("location", null), pageTabs().map { tab -> tab.element.getAttribute("aria-current") })
        assertEquals("test/item/42", pageTabs()[0].element.getAttribute("href"))
    }

    @Test
    fun `should prefer exact target over earlier matching section`() {
        buildTestPage(view = SecondTestView::class.java) {
            head("Тур") {
                tabs {
                    tab("Обзор", FirstTestView::class.java, activeOn = setOf(SecondTestView::class.java))
                    tab("Задачи", SecondTestView::class.java)
                }
            }
        }

        assertEquals(listOf(null, "page"), pageTabs().map { tab -> tab.element.getAttribute("aria-current") })
    }

    private fun PageHeadScope.twoTabs() {
        tabs {
            tab("Первая", FirstTestView::class.java)
            tab("Вторая", SecondTestView::class.java)
        }
    }

    private fun ui(): Component = UI.getCurrent()

    private fun pageTabs(): List<Component> = ui().find("ts-page-head").findAll("ts-tab")
}
