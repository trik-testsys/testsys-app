@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.AriaCurrent
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.set
import tech.testsys.web.components.core.setAriaCurrent
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.buildBadge
import tech.testsys.web.components.display.buildCounter
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.navigation.header.configureExactRoute
import tech.testsys.web.components.texts.UiTexts

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
        marks += Span(text).apply { addClassName(CssClass.PageHeadMeta) }
    }

    /**
     * Fills the right side of the title row with actions of the regular size.
     *
     * @throws IllegalStateException if the head already has actions.
     * @since %CURRENT_VERSION%
     */
    fun actions(content: ContentScope.() -> Unit) {
        check(actionsBar == null) { "Page head already has actions; call actions() once" }
        val bar = Div().apply { addClassName(CssClass.PageHeadActions) }
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
        tabs(matchRouteParameters = false, content = content)
    }

    /**
     * Adds page tabs with optional exact route matching, including route parameters; the default overload matches classes.
     *
     * @throws IllegalArgumentException if there are fewer than two tabs.
     * @throws IllegalStateException if the head already has tabs.
     * @since %CURRENT_VERSION%
     */
    fun tabs(matchRouteParameters: Boolean, content: PageTabsScope.() -> Unit) {
        check(tabsNav == null) { "Page head already has tabs; call tabs() once" }
        tabsNav = PageTabsScope(view, texts, matchRouteParameters).apply(content).build(texts.navigation.sections)
    }

    internal fun build(title: String): Div {
        val inner = Div().apply { addClassName(CssClass.PageHeadInner) }
        if (crumbs.isNotEmpty()) inner.add(breadcrumbs(title))

        val titleRow = Div(H1(title).apply { addClassName(CssClass.H1) }).apply { addClassName(CssClass.PageHeadTitleRow) }
        marks.forEach { mark -> titleRow.add(mark) }
        actionsBar?.let { bar -> titleRow.add(bar) }
        inner.add(titleRow)
        tabsNav?.let { nav -> inner.add(nav) }
        return Div(inner).apply { addClassName(CssClass.PageHead) }
    }

    private fun breadcrumbs(title: String): Nav = Nav().apply {
        addClassName(CssClass.Crumbs)
        element.setAttribute(HtmlAttribute.AriaLabel, texts.navigation.breadcrumbs)
        crumbs.forEach { link -> add(link, separator()) }
        add(Span(title).apply { element.setAriaCurrent(AriaCurrent.Page) })
    }

    private fun separator(): Span = Span("/").apply {
        addClassName(CssClass.CrumbsSep)
        element.setAriaHidden(true)
    }
}

/**
 * Scope of page tabs: the sections in order.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageTabsScope internal constructor(
    private val view: Class<out Component>?,
    private val texts: UiTexts,
    private val isRouteExactMatch: Boolean = false,
) {
    private val tabs = mutableListOf<PageTab>()

    /**
     * Adds a tab named [label] that opens [target] with [parameters]; a [count] above zero shows a counter of [countKind].
     * [activeOn] lists additional pages of this section; an exact target match takes priority.
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
        activeOn: Set<Class<out Component>> = emptySet(),
    ) {
        require(count == null || count >= 0) { "Page tab '$label' count must not be negative, got $count" }
        val link = RouterLink(label, target, parameters).apply { addClassName(CssClass.Tab) }
        if (count != null && count > 0) link.add(buildCounter(count, countKind, texts))
        tabs += PageTab(link, target, activeOn.toSet())
    }

    internal fun build(ariaLabel: String): Nav {
        require(tabs.size >= MIN_PAGE_TABS) { "Page tabs need at least $MIN_PAGE_TABS tabs, got ${tabs.size}" }

        val current = tabs.firstOrNull { tab -> tab.target == view }
            ?: tabs.firstOrNull { tab -> view in tab.activeOn }
        return Nav().apply {
            addClassNames(CssClass.Tabs, CssClass.TabsBare, CssClass.TabsLg)
            element.setAttribute(HtmlAttribute.AriaLabel, ariaLabel)
            tabs.forEach { tab ->
                if (isRouteExactMatch && tabs.any { candidate -> candidate.target == view }) {
                    configureExactRoute(tab.link) { link, isCurrent ->
                        link.element.classList.set(CssClass.TabActive, isCurrent)
                        if (isCurrent) link.element.setAriaCurrent(AriaCurrent.Page) else link.element.setAriaCurrent(null)
                    }
                    add(tab.link)
                } else {
                    add(mark(tab, isCurrent = tab === current))
                }
            }
        }
    }

    private fun mark(tab: PageTab, isCurrent: Boolean): RouterLink = tab.link.apply {
        if (isCurrent) {
            addClassName(CssClass.TabActive)
            element.setAriaCurrent(if (tab.target == view) AriaCurrent.Page else AriaCurrent.Location)
        }
    }
}

/** Tab of the page head: its ready-made [link] and the route it opens, used to find the current tab. */
private class PageTab(val link: RouterLink, val target: Class<out Component>, val activeOn: Set<Class<out Component>>)
