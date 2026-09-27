package tech.testsys.web.ui.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.display.CounterKind
import tech.testsys.web.ui.display.Tone
import tech.testsys.web.ui.display.buildBadge
import tech.testsys.web.ui.display.buildCounter
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.Placement

private const val MIN_PAGE_TABS = 2

/**
 * Scope of the page head: breadcrumbs, badges and notes next to the title, actions and section tabs.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageHeadScope internal constructor(private val texts: UiTexts, private val view: Class<out Component>?) {
    private val crumbs = mutableListOf<RouterLink>()
    private val marks = mutableListOf<Component>()
    private var actionsBar: Div? = null
    private var tabsNav: Nav? = null

    /**
     * Adds a parent page named [label] to the breadcrumbs, opened by [target] with [parameters].
     *
     * @since %CURRENT_VERSION%
     */
    fun crumb(label: String, target: Class<out Component>, parameters: RouteParameters = RouteParameters.empty()) {
        crumbs += RouterLink(label, target, parameters)
    }

    /**
     * Adds a badge of [tone] next to the title, e.g. the state of a contest.
     *
     * @since %CURRENT_VERSION%
     */
    fun badge(text: String, tone: Tone) {
        marks += buildBadge(text, tone)
    }

    /**
     * Adds a muted note next to the title, e.g. the format and the time of a contest.
     *
     * @since %CURRENT_VERSION%
     */
    fun meta(text: String) {
        marks += Span(text).apply { addClassName("ts-page-head__meta") }
    }

    /**
     * Fills the right side of the title row with actions of the regular size.
     *
     * @throws IllegalStateException if the head already has actions.
     * @since %CURRENT_VERSION%
     */
    fun actions(content: ContentScope.() -> Unit) {
        check(actionsBar == null) { "Page head already has actions; call actions() once" }
        val bar = Div().apply { addClassName("ts-page-head__actions") }
        ContentScope(bar, texts, Placement.PageHead).content()
        actionsBar = bar
    }

    /**
     * Adds tabs that open the sections of one object; the tab of the built page is marked as current.
     *
     * @throws IllegalArgumentException if there are fewer than two tabs.
     * @throws IllegalStateException if the head already has tabs.
     * @since %CURRENT_VERSION%
     */
    fun tabs(content: PageTabsScope.() -> Unit) {
        check(tabsNav == null) { "Page head already has tabs; call tabs() once" }
        tabsNav = PageTabsScope(view).apply(content).build(texts.navigation.sections)
    }

    internal fun build(title: String): Div {
        val inner = Div().apply { addClassName("ts-page-head__inner") }
        if (crumbs.isNotEmpty()) inner.add(breadcrumbs(title))
        val titleRow = Div(H1(title).apply { addClassName("ts-h1") }).apply { addClassName("ts-page-head__title-row") }
        marks.forEach { mark -> titleRow.add(mark) }
        actionsBar?.let { bar -> titleRow.add(bar) }
        inner.add(titleRow)
        tabsNav?.let { nav -> inner.add(nav) }
        return Div(inner).apply { addClassName("ts-page-head") }
    }

    private fun breadcrumbs(title: String): Nav = Nav().apply {
        addClassName("ts-crumbs")
        element.setAttribute("aria-label", texts.navigation.breadcrumbs)
        crumbs.forEach { link -> add(link, separator()) }
        add(Span(title).apply { element.setAttribute("aria-current", "page") })
    }

    private fun separator(): Span = Span("/").apply {
        addClassName("ts-crumbs__sep")
        element.setAttribute("aria-hidden", "true")
    }
}

/**
 * Scope of page tabs: the sections in order.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageTabsScope internal constructor(private val view: Class<out Component>?) {
    private val tabs = mutableListOf<PageTab>()

    /**
     * Adds a tab named [label] that opens [target] with [parameters]; a [count] above zero shows a counter of [countKind].
     *
     * @throws IllegalArgumentException if [count] is negative.
     * @since %CURRENT_VERSION%
     */
    fun tab(
        label: String,
        target: Class<out Component>,
        parameters: RouteParameters = RouteParameters.empty(),
        count: Int? = null,
        countKind: CounterKind = CounterKind.Neutral,
    ) {
        require(count == null || count >= 0) { "Page tab '$label' count must not be negative, got $count" }
        val link = RouterLink(label, target, parameters).apply { addClassName("ts-tab") }
        if (count != null && count > 0) link.add(buildCounter(count, countKind))
        tabs += PageTab(link, target)
    }

    internal fun build(ariaLabel: String): Nav {
        require(tabs.size >= MIN_PAGE_TABS) { "Page tabs need at least $MIN_PAGE_TABS tabs, got ${tabs.size}" }
        val current = tabs.firstOrNull { tab -> tab.target == view }
        return Nav().apply {
            addClassNames("ts-tabs", "ts-tabs--bare", "ts-tabs--lg")
            element.setAttribute("aria-label", ariaLabel)
            tabs.forEach { tab -> add(mark(tab, isCurrent = tab === current)) }
        }
    }

    private fun mark(tab: PageTab, isCurrent: Boolean): RouterLink = tab.link.apply {
        if (isCurrent) {
            addClassName("ts-tab--active")
            element.setAttribute("aria-current", "page")
        }
    }
}

/** Tab of the page head: its ready-made [link] and the route it opens, used to find the current tab. */
private class PageTab(val link: RouterLink, val target: Class<out Component>)
