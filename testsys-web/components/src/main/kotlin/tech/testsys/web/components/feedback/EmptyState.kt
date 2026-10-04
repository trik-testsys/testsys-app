package tech.testsys.web.components.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

private const val EMPTY_ICON_SIZE = 22
private const val EMPTY_STATE_OWNER = "an empty state"

/** What an empty state shows: its [title], optional [description], [icon] and [actions]. */
internal class EmptyContent(
    val title: String,
    val description: String? = null,
    val icon: IconName = IconName.File,
    val actions: ContentScope.() -> Unit = {},
)

/** Builds the `.ts-empty` markup of [content]; an [isError] state is red and shows the alert icon instead. */
internal fun buildEmptyState(content: EmptyContent, texts: UiTexts, gridColumns: Int, isError: Boolean = false): Div {
    val icon = svgIcon(if (isError) IconName.TriangleAlert else content.icon, EMPTY_ICON_SIZE)
    val state = Div(
        Span(icon).apply { addClassName("ts-empty__icon") },
        Span(content.title).apply { addClassName("ts-empty__title") },
    ).apply {
        addClassName("ts-empty")
        if (isError) addClassName("ts-empty--error")
    }
    content.description?.let { description -> state.add(Span(description).apply { addClassName("ts-empty__desc") }) }
    val actions = Div().apply { addClassName("ts-empty__actions") }
    ContentScope(actions, texts, Placement.Empty, gridColumns).apply(content.actions)
    if (actions.children.findAny().isPresent) state.add(actions)
    return state
}

/**
 * Fills the whole block body with an empty state: [title], optional [description], [icon] and [actions],
 * e.g. a way to where the missing things are made.
 *
 * @throws IllegalStateException if the block already holds rows, a table, an empty state or a load.
 * @since %CURRENT_VERSION%
 */
fun BlockScope.emptyState(
    title: String,
    description: String? = null,
    icon: IconName = IconName.File,
    actions: ContentScope.() -> Unit = {},
): ElementHandle {
    checkWholeBodyPlace(EMPTY_STATE_OWNER)
    val state = buildEmptyState(EmptyContent(title, description, icon, actions), texts, columns)
    placeWhole(state, EMPTY_STATE_OWNER)
    return ElementHandle(state)
}
