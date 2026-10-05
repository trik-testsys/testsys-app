package tech.testsys.web.devapp.showcase

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10.expectView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Switch
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tech.testsys.web.devapp.MockSpringVaadinTests
import tech.testsys.web.devapp.error.NotFoundView

class ShowcasePagesTests {
    @Nested
    @SpringBootTest
    @ActiveProfiles("dev")
    inner class DevTests : MockSpringVaadinTests() {
        @Test
        fun `should build foundations from packaged tokens and brand`() {
            UI.getCurrent().navigate("dev/showcase/foundations")
            expectView<ShowcaseFoundationsView>()
            assertEquals(
                listOf("Палитра", "Типографика", "Отступы, радиусы, тени и движение", "Состояния движения", "Бренд"),
                _find<com.vaadin.flow.component.html.H3>().filter { "ts-block__title" in it.element.classList }.map { it.text },
            )
            assertTrue(_find<Div>().any { it.text == "--accent" })
            assertTrue(_find<com.vaadin.flow.component.html.Image>().any { it.src.contains("brand/") })
        }

        @Test
        fun `should retain all columns and pagination in the wide matrix example`() {
            UI.getCurrent().navigate("dev/showcase/states")
            val table = _find<com.vaadin.flow.component.html.Table>().single { candidate ->
                candidate.element.style.get("--ts-table-used") == "66"
            }

            val group = table.element.children.toList().single { child -> child.tag == "colgroup" }
            assertEquals(31, group.childCount)
            assertTrue(table.element.textRecursively.contains("Задача 30"))
            assertTrue(table.element.textRecursively.contains("Группа 1"))
            val block = table.parent.orElseThrow().parent.orElseThrow().parent.orElseThrow()
            val next = block._find<NativeButton>().single { button -> button.element.getAttribute("aria-label") == "Вперёд" }
            next._click()
            assertTrue(table.element.textRecursively.contains("Группа 2"))
        }

        @Test
        fun `should expose all new field and transfer examples in forms route`() {
            UI.getCurrent().navigate("dev/showcase/forms")

            expectView<ShowcaseFormsView>()
            assertTrue(_find<Switch>().any { field -> field.ariaLabel.orElse("") == "Публикация" })
        }

        @Test
        fun `should expose obscured values and retain informative field actions`() {
            UI.getCurrent().navigate("dev/showcase/forms")
            val obscuredText = _find<com.vaadin.flow.component.textfield.TextField>().single { field ->
                field.ariaLabel.orElse("") == "Скрытый текст"
            }
            val valueArea = obscuredText.parent.orElseThrow()

            assertTrue(valueArea.element.hasAttribute("data-ts-obscured"))
            assertEquals("Подсказка остаётся читаемой", obscuredText.helperText)
            assertTrue(_find<Button>().any { action -> action.text == "Проверить действие" && action.isEnabled })
            assertTrue(
                _find<Div>().any { value ->
                    value.element.hasAttribute("data-ts-obscured") &&
                        value.element.getAttribute("aria-label") == "Скрытая информация"
                },
            )
        }

        @Test
        fun `should expose reusable overlay examples in overlays route`() {
            UI.getCurrent().navigate("dev/showcase/overlays")

            expectView<ShowcaseOverlaysView>()
        }

        @Test
        fun `should show each background action result in overlays route`() {
            UI.getCurrent().navigate("dev/showcase/overlays")
            val action = _find<Button>().single { button -> button.text == "Действие за попапом" }

            action._click()

            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Действие выполнено: 1" })
            action._click()
            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Действие выполнено: 2" })
        }

        @Test
        fun `should expose header route and observable notification controls`() {
            UI.getCurrent().navigate("dev/showcase/header")
            val action = _find<Button>().single { button -> button.text == "Добавить уведомление" }

            action._click()

            expectView<ShowcaseHeaderView>()
            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Добавлено уведомление 1" })
        }

        @Test
        fun `should expose neutral data and display examples in display route`() {
            UI.getCurrent().navigate("dev/showcase/display")

            expectView<ShowcaseDisplayView>()
            assertTrue(_find<NativeButton>().any { button -> "ts-qopt" in button.element.classList })
        }

        @Test
        fun `should render numeric zero and legacy compatibility separately`() {
            UI.getCurrent().navigate("dev/showcase/display")

            val scores = _find<Span>().filter { span -> "ts-verdict--score" in span.element.classList }
            assertEquals(listOf("0", "65баллов", "0баллов"), scores.map { score -> score.element.textRecursively })
            assertEquals(
                7,
                _find<Span>().count { span -> "ts-verdict" in span.element.classList && "ts-verdict--score" !in span.element.classList },
            )
        }

        @Test
        fun `should show selected step and reset it`() {
            UI.getCurrent().navigate("dev/showcase/display")
            val first = _find<NativeButton>().single { button -> button.element.getAttribute("aria-label") == "Начало" }

            first._click()

            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Выбран шаг: 1" })
            _find<Button>().single { button -> button.text == "Сбросить выборы" }._click()
            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Выбран шаг: 2" })
        }

        @Test
        fun `should show selected answer and reset it`() {
            UI.getCurrent().navigate("dev/showcase/display")
            val answer = _find<NativeButton>().single { button ->
                "ts-qopt" in button.element.classList && button.element.textRecursively.contains(
                    "Обычный ответ",
                )
            }

            answer._click()

            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Ответ выбран: true" })
            _find<Button>().single { button -> button.text == "Сбросить выборы" }._click()
            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Ответ выбран: false" })
        }

        // Reset must restore the initially selected multiple answer as well as the ordinary answer.
        @Test
        @Tag("regression")
        fun `should restore the initial multiple answer selection on reset`() {
            UI.getCurrent().navigate("dev/showcase/display")
            val answer = _find<NativeButton>().single { button ->
                "ts-qopt" in button.element.classList && button.element.textRecursively.contains("Несколько ответов")
            }
            answer._click()
            assertEquals("false", answer.element.getAttribute("aria-pressed"))

            _find<Button>().single { button -> button.text == "Сбросить выборы" }._click()

            assertEquals("true", answer.element.getAttribute("aria-pressed"))
            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Ответ выбран: false" })
        }

        @Test
        fun `should show selected question and keep unavailable question disabled`() {
            UI.getCurrent().navigate("dev/showcase/display")
            val question = _find<NativeButton>().single { button -> "ts-qnav__cell" in button.element.classList && button.text == "4" }

            question._click()

            assertTrue(_find<Div>().any { paragraph -> paragraph.text == "Текущий вопрос: 4" })
            assertTrue(
                _find<NativeButton>().single { button ->
                    "ts-qnav__cell" in button.element.classList && button.text == "10"
                }.isEnabled.not(),
            )
        }
    }

    @Nested
    @SpringBootTest
    inner class OutsideDevTests : MockSpringVaadinTests() {
        @ParameterizedTest
        @ValueSource(
            strings = [
                "dev/showcase/forms",
                "dev/showcase/overlays",
                "dev/showcase/display",
                "dev/showcase/header",
                "dev/showcase/foundations",
            ],
        )
        fun `should reject remaining showcase routes outside dev profile`(route: String) {
            UI.getCurrent().navigate(route)

            expectView<NotFoundView>()
        }
    }
}
