package tech.testsys.web.components

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.router.Route
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.layout.renderPage
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.texts.UiLocalization
import tech.testsys.web.components.texts.bindUiLocalization
import java.time.ZoneOffset

/** Localization of the tests: the Russian messages with dates in UTC, bound to the UI of every test. */
internal val testTexts = UiLocalization(SupportedRegion.RU, ZoneOffset.UTC)

/** Starts a mocked Vaadin UI with the test routes of this package and the test localization before each test. */
abstract class MockVaadinTests {
    @BeforeEach
    fun setUpVaadin() {
        MockVaadin.setup(Routes().autoDiscoverViews("tech.testsys.web.components"))
        bindUiLocalization(UI.getCurrent(), testTexts)
    }

    @AfterEach
    fun tearDownVaadin() = MockVaadin.tearDown()
}

/** Route target of navigation tests. */
@Route("test/first")
class FirstTestView : Div()

/** Second route target of navigation tests. */
@Route("test/second")
class SecondTestView : Div()

/** Route target with a parameter for page tab tests. */
@Route("test/item/:id")
class ItemTestView : Div()

internal fun Component.child(index: Int): Component = children.toList()[index]

internal fun Component.classes(): Set<String> = element.classList

/** All components of the subtree, this one included, that carry [cssClass]. */
internal fun Component.findAll(cssClass: String): List<Component> =
    (listOf(this) + descendants()).filter { component -> cssClass in component.element.classList }

internal fun Component.find(cssClass: String): Component = findAll(cssClass).single()

private fun Component.descendants(): List<Component> = children.toList().flatMap { child -> listOf(child) + child.descendants() }

/** Builds a page of [view] with an empty header, attaches it to the current UI and returns its `main.ts-page`. */
internal fun buildTestPage(view: Class<out Component>? = null, body: PageScope.() -> Unit): Component {
    val root = Div()
    renderPage(root, CabinetHeader(), view, body)
    UI.getCurrent().add(root)
    return root.children.toList().single { child -> child.element.tag == "main" }
}

/** Builds a full-width block with one row filled by [content] and returns the row. */
internal fun buildTestRow(content: BlockRowScope.() -> Unit): Component =
    buildTestPage { row { block { row(content) } } }.find("ts-block__row")

/** Builds a full-width block whose only row holds a vertical group filled by [content] and returns the group. */
internal fun buildTestContent(content: ContentScope.() -> Unit): Component = buildTestRow { vertical { content() } }.child(0)

/** The Vaadin control of the grid field labelled [label] on the current UI. */
internal inline fun <reified C : Component> control(label: String): C {
    val caption = UI.getCurrent().findAll("ts-field__text").single { text -> text.element.text == label }
    val field = caption.parent.orElseThrow().parent.orElseThrow()
    return field.find("ts-field__value").child(0) as C
}

/** All buttons of the subtree of [root], shown or hidden. */
internal fun findAllButtons(root: Component): List<Button> = (listOf(root) + root.descendants()).filterIsInstance<Button>()

/** The button with [text] on the current UI, shown or hidden. */
internal fun button(text: String): Button = findAllButtons(UI.getCurrent()).single { button -> button.text == text }

/** JavaScript calls queued on the current UI since the last call, flushed as the response would do before it is read. */
internal fun pendingJavaScript(): List<PendingJavaScriptInvocation> {
    val internals = UI.getCurrent().internals
    internals.stateTree.runExecutionsBeforeClientResponse()
    return internals.dumpPendingJavaScriptInvocations()
}

/** Dialogs opened on the current UI after the pending round trip attached them. */
internal fun openDialogs(): List<Dialog> {
    MockVaadin.clientRoundtrip()
    return _find<Dialog>().filter { dialog -> dialog.isOpened }
}
