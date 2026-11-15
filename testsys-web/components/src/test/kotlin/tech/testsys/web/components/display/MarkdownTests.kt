package tech.testsys.web.components.display

import com.vaadin.flow.component.markdown.Markdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child

class MarkdownTests : MockVaadinTests() {
    @Test
    fun `should place the markdown content on its size in a block row`() {
        val markdown = buildTestRow { markdown("- Первый пункт", size = 12) }.child(0) as Markdown

        assertEquals("- Первый пункт", markdown.content)
        assertEquals("span 12", markdown.element.style.get("grid-column"))
    }
}
