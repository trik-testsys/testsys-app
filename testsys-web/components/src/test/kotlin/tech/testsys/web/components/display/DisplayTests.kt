package tech.testsys.web.components.display

import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll

class DisplayTests : MockVaadinTests() {
    @Nested
    inner class TextTests {
        @Test
        fun `should show the paragraph following a signal`() {
            val signal = ValueSignal("Первый")
            lateinit var handle: TextHandle
            val page = buildTestContent { handle = text(signal) }

            signal.set("Второй")

            assertEquals("Второй", handle.text)
            assertEquals("Второй", page.child(0).element.textRecursively)
        }

        @Test
        fun `should place a signal-bound paragraph on its size in a block row`() {
            val signal = ValueSignal("Значение")

            val paragraph = buildTestRow { text(signal, size = 4) }.child(0)

            assertEquals("Значение", paragraph.element.textRecursively)
            assertEquals("span 4", paragraph.element.style.get("grid-column"))
        }
    }

    @Nested
    inner class TagTests {
        @Test
        fun `should style tag by its kind`() {
            val page = buildTestContent {
                tag("графы")
                tag("#48213", TagKind.Code)
            }

            val tags = page.findAll("ts-tag")
            assertEquals(setOf("ts-tag"), tags[0].classes())
            assertTrue("ts-tag--mono" in tags[1].classes())
        }

        @Test
        fun `should span tag over its size in a block row`() {
            val tag = buildTestRow { tag("графы", size = 4) }.child(0)

            assertTrue("ts-tag" in tag.classes())
            assertEquals("span 4", tag.element.style.get("grid-column"))
        }
    }

    @Nested
    inner class BadgeTests {
        @ParameterizedTest
        @EnumSource(Tone::class)
        fun `should colour badge by its tone`(tone: Tone) {
            val badge = buildTestContent { badge("Идёт", tone) }.find("ts-status")

            assertTrue("ts-status--${tone.name.lowercase()}" in badge.classes())
            assertEquals("Идёт", badge.element.textRecursively)
            assertEquals(1, badge.findAll("ts-status__dot").size)
        }

        @Test
        fun `should place badge on its columns of the row`() {
            val badge = buildTestRow { badge("Регистрация", Tone.Info, size = 6) }.find("ts-status")

            assertEquals("span 6", badge.element.style.get("grid-column"))
        }
    }

    @Nested
    inner class CounterTests {
        @Test
        fun `should update counter through its handle`() {
            lateinit var counter: TextHandle
            val page = buildTestContent { counter = counter(3) }

            counter.text = "4"

            val badge = page.find("ts-counter")
            assertTrue("ts-counter--danger" in badge.classes())
            assertEquals("4", badge.element.textRecursively)
        }

        @Test
        fun `should span counter over its size in a block row`() {
            val counter = buildTestRow { counter(3, size = 2) }.child(0)

            assertTrue("ts-counter" in counter.classes())
            assertEquals("span 2", counter.element.style.get("grid-column"))
        }
    }

    @Nested
    inner class StatCardTests {
        @Test
        fun `should render stat card as a block of the whole slot`() {
            val page = buildTestPage { row { slot(3) { row { statCard("Решено", "42") } } } }

            val block = page.find("ts-block")
            assertEquals("span 3", block.element.style.get("grid-column"))
            assertEquals("Решено", block.find("ts-stat__label").element.textRecursively)
            assertEquals("42", block.find("ts-stat__value").element.textRecursively)
        }

        @Test
        fun `should colour delta by trend`() {
            val page = buildTestPage {
                row { slot(3) { row { statCard("Рейтинг", "1 842", delta = "+38", trend = Trend.Up) } } }
            }

            assertTrue("ts-stat__delta--up" in page.find("ts-stat__delta").classes())
        }

        @Test
        fun `should update stat card value through its handle`() {
            lateinit var card: TextHandle
            val page = buildTestPage { row { slot(3) { row { card = statCard("Решено", "42") } } } }

            card.text = "43"

            assertEquals("43", page.find("ts-stat__value").element.textRecursively)
        }

        @Test
        fun `should render stat card in a block row without its own block`() {
            lateinit var card: TextHandle
            val page = buildTestPage { block { row { card = statCard("Решено", "42", size = 6) } } }

            card.text = "43"

            val stat = page.find("ts-block__row").child(0)
            assertTrue("ts-stat" in stat.classes())
            assertEquals("span 6", stat.element.style.get("grid-column"))
            assertEquals(1, page.findAll("ts-block").size)
            assertEquals("43", stat.find("ts-stat__value").element.textRecursively)
        }
    }
}
