package tech.testsys.web.components.display

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.find

internal class ContestCardTests : MockVaadinTests() {
    @Test
    fun `should separate card and CTA callbacks`() {
        var cardClicks = 0
        var actionClicks = 0
        val root = buildTestContent {
            contestCard(
                ContestCardData(
                    "Title",
                    "Format",
                    "Status",
                    Tone.Info,
                    "Today",
                    actionLabel = "Join",
                ),
            ) {
                onClick { cardClicks++ }
                onAction { actionClicks++ }
            }
        }
        val button = requireNotNull(root.find("ts-btn") as? NativeButton)

        button._click()

        assertEquals(1, actionClicks)
        assertEquals(0, cardClicks)
    }
}
