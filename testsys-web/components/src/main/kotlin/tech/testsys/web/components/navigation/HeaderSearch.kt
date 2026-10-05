@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import org.slf4j.LoggerFactory
import tech.testsys.web.components.Background
import tech.testsys.web.components.HeaderTexts
import tech.testsys.web.components.core.AriaAutocomplete
import tech.testsys.web.components.core.AriaLive
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setAriaAutocomplete
import tech.testsys.web.components.core.setAriaBusy
import tech.testsys.web.components.core.setAriaExpanded
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.core.setAriaLive
import tech.testsys.web.components.core.setAriaRole
import tech.testsys.web.components.core.setAriaSelected
import tech.testsys.web.components.core.setRole
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Search provider run in the background after a trimmed query of at least one character, with a 300 ms debounce.
 *
 * @property fetch the provider returning application-authorized results; late answers are discarded after closing or detach.
 * @since %CURRENT_VERSION%
 */
class HeaderSearch(val fetch: (String) -> List<HeaderSearchResult>)

/**
 * Search suggestion with a stable key and an application-provided destination.
 *
 * @property key the unique key within a result list.
 * @property label the primary text.
 * @property destination the route or action opened when chosen.
 * @property description the optional explanatory text.
 * @since %CURRENT_VERSION%
 */
data class HeaderSearchResult(
    val key: String,
    val label: String,
    val destination: HeaderDestination,
    val description: String? = null,
)

/** Owns the search lifecycle in Flow; the client only debounces input and forwards keyboard commands. */
internal class HeaderSearchController(
    private val search: HeaderSearch,
    private val texts: HeaderTexts,
    interactions: HeaderInteractions,
) {
    private val generation = AtomicLong()
    private val listId = "ts-header-results-${UUID.randomUUID()}"
    private var query = ""
    private var state: SearchState = SearchState.Hidden
    private var activeIndex = -1
    private val input = headerSearchField(texts.search)
    val field = input.field.apply {
        element.setRole(ElementRole.ComboBox)
        element.setAriaAutocomplete(AriaAutocomplete.List)
        element.setAttribute("aria-controls", listId)
    }
    val component = input.component
    val popup = interactions.popup(component, label = texts.search, theme = "ts-header-search-popup").apply {
        isOpenOnClick = false
        isTabFocusEnabled = true
        setAriaRole(ElementRole.Presentation)
    }
    val status = Span().apply {
        addClassName("ts-header-search-status")
        element.setRole(ElementRole.Status)
        element.setAriaLive(AriaLive.Polite)
    }
    private val results = Div().apply {
        setId(listId)
        addClassName("ts-header-results")
        element.setRole(ElementRole.ListBox)
        element.setAttribute("aria-label", texts.search)
    }
    private val retryButton = NativeButton(texts.retry).apply {
        addClassNames("ts-btn", "ts-btn--secondary", "ts-btn--sm")
        addClickListener { retry() }
        isVisible = false
    }

    init {
        component.add(popup)
        field.element.setAriaHasPopup(AriaPopup.ListBox)
        field.element.setAriaExpanded(false)
        field.element.setAriaBusy(false)
        popup.add(Div(status, results, retryButton).apply { addClassName("ts-header-search-body") })
        field.element.addEventListener("header-input") { event -> inputChanged(event.eventData.get("event.detail.query").asString()) }
            .addEventData("event.detail.query")
        field.element.addEventListener("header-query") { event -> fetchPending(event.eventData.get("event.detail.query").asString()) }
            .addEventData("event.detail.query")
        field.element.addEventListener("header-key") { event -> key(event.eventData.get("event.detail.key").asString()) }
            .addEventData("event.detail.key")
        popup.addOpenedChangeListener { event -> if (!event.isOpened) invalidate() }
        component.addAttachListener {
            field.element.executeJs("window.testsysHeader.searchAttach(this, $0)", popup.element)
        }
        component.addDetachListener {
            close()
            field.element.executeJs("window.testsysHeader.searchDetach(this)")
        }
    }

    fun inputChanged(value: String) {
        query = value.trim()
        generation.incrementAndGet()
        activeIndex = -1
        if (query.isEmpty()) {
            close()
        } else {
            state = SearchState.Loading
            showStatus(texts.searchLoading)
            popup.open()
        }
    }

    fun fetchPending(value: String) {
        if (state != SearchState.Loading || value.trim() != query || !component.isAttached) return
        val requested = query
        val token = generation.get()
        val ui = component.ui.orElseThrow()
        Background.executor().execute {
            val outcome = runCatching { search.fetch(requested) }
            if (outcome.exceptionOrNull() is InterruptedException) Thread.currentThread().interrupt()
            Background.inUi(ui) {
                if (component.isAttached && generation.get() == token && state == SearchState.Loading) show(outcome)
            }
        }
    }

    fun retry() {
        inputChanged(query)
        fetchPending(query)
    }

    fun close() {
        invalidate()
        popup.close()
    }

    fun key(key: String) {
        when (key) {
            "Escape", "Tab" -> close()
            "ArrowDown" -> move(1)
            "ArrowUp" -> move(-1)
            "Enter" -> selected()?.let { result ->
                close()
                result.destination.open()
            }
        }
    }

    private fun invalidate() {
        generation.incrementAndGet()
        state = SearchState.Hidden
        activeIndex = -1
        results.removeAll()
        field.element.removeAttribute("aria-activedescendant")
        field.element.setAriaExpanded(false)
        field.element.setAriaBusy(false)
    }

    private fun show(outcome: Result<List<HeaderSearchResult>>) {
        outcome.fold(onSuccess = { values ->
            require(values.map { result -> result.key }.distinct().size == values.size) { "Header search result keys must be unique" }
            state = SearchState.Results(values)
            showStatus(if (values.isEmpty()) texts.searchEmpty else texts.searchCount(values.size))
            values.forEachIndexed { index, result -> results.add(resultItem(index, result)) }
            if (values.isNotEmpty()) status.addClassName("ts-sr-only")
        }, onFailure = { failure ->
            LoggerFactory.getLogger(HeaderSearchController::class.java).error("Header search failed", failure)
            state = SearchState.Failed
            showStatus(texts.searchFailed)
            retryButton.isVisible = true
        })
    }

    private fun resultItem(index: Int, result: HeaderSearchResult): Component =
        destinationLink(result.label, result.destination) { close() }.apply {
            element.classList.add("ts-header-result")
            setId("$listId-$index")
            element.setRole(ElementRole.Option)
            element.setAriaSelected(false)
            element.setAttribute("tabindex", "-1")
            result.description?.let { description -> element.appendChild(Span(description).element) }
        }

    private fun showStatus(text: String) {
        results.removeAll()
        status.removeClassName("ts-sr-only")
        status.text = text
        retryButton.isVisible = false
        field.element.removeAttribute("aria-activedescendant")
        field.element.setAriaExpanded(true)
        field.element.setAriaBusy(state == SearchState.Loading)
    }

    private fun move(direction: Int) {
        val values = (state as? SearchState.Results)?.values.orEmpty()
        if (values.isEmpty() || !popup.isOpened) return
        activeIndex = if (activeIndex < 0 && direction < 0) values.lastIndex else Math.floorMod(activeIndex + direction, values.size)
        field.element.setAttribute("aria-activedescendant", "$listId-$activeIndex")
        results.children.toList().forEachIndexed { index, item ->
            item.element.setAriaSelected(index == activeIndex)
        }
        results.element.executeJs("this.children[$0]?.scrollIntoView({block:'nearest'})", activeIndex)
    }

    private fun selected(): HeaderSearchResult? = (state as? SearchState.Results)?.values?.getOrNull(activeIndex)

    private sealed interface SearchState {
        data object Hidden : SearchState
        data object Loading : SearchState
        data object Failed : SearchState
        class Results(val values: List<HeaderSearchResult>) : SearchState
    }
}
