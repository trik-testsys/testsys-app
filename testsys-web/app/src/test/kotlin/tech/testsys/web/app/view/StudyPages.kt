package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._upload
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import com.vaadin.flow.router.RouterLink
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/** Reads and operates the pages of students and participants in the tests. */
internal object StudyPages {
    /** Returns the breadcrumbs of the open page as labels with their addresses. */
    fun crumbs(): Map<String, String> = UI.getCurrent()._get<Nav> { classes = "ts-crumbs" }._find<RouterLink>()
        .associate { link -> link.text to link.href }

    /** Returns the text of the open page. */
    fun pageText(): String = UI.getCurrent().internals.activeRouterTargetsChain.first().element.textRecursively

    fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    fun textField(label: String): TextField = UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    /** Returns the table whose header has the column [column]. */
    fun table(column: String): Table = UI.getCurrent()._find<Table>().single { table -> column in table.element.textRecursively }

    /** Returns the text of each data row of the table with the column [column]. */
    fun rowTexts(column: String): List<String> =
        table(column)._get<TableBody>()._find<TableRow>().map { row -> row.element.textRecursively }

    /** Clicks the data row [index] of the table with the column [column] as the client does when the click passes its filters. */
    fun clickRow(column: String, index: Int = 0) {
        val row = table(column)._get<TableBody>()._find<TableRow>()[index]
        val click = ObjectMapper().createObjectNode().apply {
            row.element.node.getFeature(ElementListenerMap::class.java).getExpressions("click")
                .forEach { expression -> put(expression, true) }
        }
        row._fireDomEvent("click", click)
    }

    /** Uploads [content] as [filename] to the only file drop of the page with the transfer identity its client sends. */
    fun upload(filename: String, content: ByteArray) {
        val upload = UI.getCurrent()._get<Upload>()
        val identity = "${upload.element.getAttribute("data-ts-upload-generation")}:${UUID.randomUUID()}"
        val factory = MockVaadin.mockRequestFactory
        MockVaadin.mockRequestFactory = { session -> factory(session).apply { headers["X-TestSys-Transfer"] = listOf(identity) } }
        try {
            upload._upload(filename, "text/x-python", content)
        } finally {
            MockVaadin.mockRequestFactory = factory
        }
    }
}
