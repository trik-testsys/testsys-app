@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.router.HighlightAction
import com.vaadin.flow.router.HighlightCondition
import com.vaadin.flow.router.HighlightConditions
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.core.AriaCurrent
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.setType

/**
 * Application-provided destination of a header item.
 *
 * @since 1.0.0-beta.1
 */
sealed interface HeaderDestination {
    /**
     * Route with optional parameters.
     *
     * @property target the route to open.
     * @property parameters the parameters of the route.
     * @since 1.0.0-beta.1
     */
    data class Route(val target: Class<out Component>, val parameters: RouteParameters = RouteParameters()) : HeaderDestination

    /**
     * Action executed in the UI thread.
     *
     * @property onSelect the handler supplied by the application.
     * @since 1.0.0-beta.1
     */
    class Action(val onSelect: () -> Unit) : HeaderDestination
}

internal fun HeaderDestination.open() {
    when (this) {
        is HeaderDestination.Route -> UI.getCurrent().navigate(target, parameters)
        is HeaderDestination.Action -> onSelect()
    }
}

internal fun destinationLink(label: String, destination: HeaderDestination, beforeOpen: () -> Unit): Component = when (destination) {
    is HeaderDestination.Route -> RouterLink(label, destination.target, destination.parameters).apply {
        element.addEventListener(DomEvent.Click) { beforeOpen() }
    }
    is HeaderDestination.Action -> NativeButton(label).apply {
        element.setType(ElementType.Button)
        addClickListener {
            beforeOpen()
            destination.onSelect()
        }
    }
}

/** Applies exact route highlighting and initializes freshly attached links from the active UI location. */
internal fun configureExactRoute(link: RouterLink, mark: (RouterLink, Boolean) -> Unit) {
    link.highlightCondition = HighlightConditions.sameLocation()
    link.highlightAction = HighlightAction<RouterLink> { target, isCurrent -> mark(target, isCurrent) }
    mark(link, UI.getCurrent()?.internals?.activeViewLocation?.path == link.href)
    link.addAttachListener { event -> mark(link, event.ui.internals.activeViewLocation.path == link.href) }
}

/**
 * Marks [link] if its route is the closest menu route to the opened location: the location itself is the page, and a
 * location nested in the route is its section. The longest of [hrefs] wins, so a section takes the mark from its role page.
 */
internal fun configureNearestRoute(link: RouterLink, hrefs: Set<String>, mark: (RouterLink, AriaCurrent?) -> Unit) {
    var current: AriaCurrent? = null
    fun resolve(path: String): Boolean {
        current = nearestRouteMark(path, link.href, hrefs)
        return current != null
    }
    link.highlightCondition = HighlightCondition<RouterLink> { _, event -> resolve(event.location.path) }
    link.highlightAction = HighlightAction<RouterLink> { target, isCurrent -> mark(target, current.takeIf { isCurrent }) }
    UI.getCurrent()?.internals?.activeViewLocation?.let { location -> mark(link, current.takeIf { resolve(location.path) }) }
    link.addAttachListener { event -> mark(link, current.takeIf { resolve(event.ui.internals.activeViewLocation.path) }) }
}

/** Returns how [href] relates to the opened [path] among the menu [hrefs], or `null` if another route or none is closer. */
internal fun nearestRouteMark(path: String, href: String, hrefs: Set<String>): AriaCurrent? {
    val nearest = hrefs.filter { candidate -> path == candidate || path.startsWith("$candidate/") }
        .maxByOrNull { candidate -> candidate.length }
    return when {
        nearest != href -> null
        path == href -> AriaCurrent.Page
        else -> AriaCurrent.Location
    }
}
