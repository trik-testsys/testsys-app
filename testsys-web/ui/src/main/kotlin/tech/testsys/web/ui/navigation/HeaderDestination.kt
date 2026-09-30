package tech.testsys.web.ui.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink

/**
 * Application-provided destination of a header item.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface HeaderDestination {
    /**
     * Route with optional parameters.
     *
     * @property target the route to open.
     * @property parameters the parameters of the route.
     * @since %CURRENT_VERSION%
     */
    data class Route(val target: Class<out Component>, val parameters: RouteParameters = RouteParameters()) : HeaderDestination

    /**
     * Action executed in the UI thread.
     *
     * @property onSelect the handler supplied by the application.
     * @since %CURRENT_VERSION%
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
        element.addEventListener("click") { beforeOpen() }
    }
    is HeaderDestination.Action -> NativeButton(label).apply {
        element.setAttribute("type", "button")
        addClickListener {
            beforeOpen()
            destination.onSelect()
        }
    }
}
