package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

private const val AVATAR_COMPACT_PIXELS = 28
private const val AVATAR_REGULAR_PIXELS = 32
private const val INITIAL_FONT_RATIO = 0.36
private const val AVATAR_TONE_COUNT = 4

/**
 * Identity presented as initials; [name] remains its accessible name.
 *
 * @property name the full accessible name.
 * @property initials an explicit abbreviation, or null to derive two initials from the name.
 * @property isSquare whether the avatar identifies an organisation rather than a person.
 * @since %CURRENT_VERSION%
 */
data class AvatarData(val name: String, val initials: String? = null, val isSquare: Boolean = false)

/**
 * Adds an avatar of [data], preserving the person's accessible name.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.avatar(data: AvatarData, configure: DataHandle<AvatarData>.() -> Unit = {}): DataHandle<AvatarData> {
    val pixels = if (placement.isCompact) AVATAR_COMPACT_PIXELS else AVATAR_REGULAR_PIXELS
    val root = Span().apply {
        element.style.set("width", "${pixels}px")
        element.style.set("height", "${pixels}px")
        element.style.set("font-size", "${pixels * INITIAL_FONT_RATIO}px")
    }

    fun render(value: AvatarData) {
        root.text = value.initials ?: value.name.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
            .take(2).joinToString("") { word -> word.take(1) }.uppercase(texts.locale)
        root.element.classList.clear()
        root.addClassNames("ts-avatar", "ts-avatar--t${Math.floorMod(value.name.sumOf { char -> char.code }, AVATAR_TONE_COUNT)}")
        root.setClassName("ts-avatar--square", value.isSquare)
        root.element.setAttribute("role", "img")
        root.element.setAttribute("aria-label", value.name)
        root.element.setAttribute("title", value.name)
    }
    render(data)
    add(root)
    return DataHandle(root, data, ::render).apply(configure)
}

/**
 * Adds an avatar on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.avatar(data: AvatarData, size: Int? = null, configure: DataHandle<AvatarData>.() -> Unit = {}): DataHandle<AvatarData> =
    ContentScope(place(size, Div()), texts, Placement.Body).avatar(data, configure)

/**
 * Adds the first [maxVisible] avatars of [data], with an accessible count for the rest.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.avatarGroup(
    data: List<AvatarData>,
    maxVisible: Int = 3,
    configure: DataHandle<List<AvatarData>>.() -> Unit = {},
): DataHandle<List<AvatarData>> {
    require(maxVisible > 0) { "Avatar group visible count must be positive, got $maxVisible" }
    val root = Div().apply { addClassName("ts-avatars") }
    fun render(values: List<AvatarData>) {
        root.removeAll()
        values.take(maxVisible).forEach { value -> ContentScope(root, texts, placement).avatar(value) }
        if (values.size > maxVisible) {
            root.add(
                Span("+${values.size - maxVisible}").apply {
                    addClassNames("ts-avatar", "ts-avatar--t3")
                    element.style.set("width", "32px")
                    element.style.set("height", "32px")
                    element.style.set("font-size", "12px")
                    element.setAttribute("aria-label", texts.components.avatarOverflow(values.size - maxVisible))
                    element.setAttribute(
                        "title",
                        values.drop(maxVisible).joinToString(", ") { value -> value.name },
                    )
                },
            )
        }
    }
    render(data)
    add(root)
    return DataHandle(root, data, ::render).apply(configure)
}

/**
 * Adds an avatar group on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.avatarGroup(
    data: List<AvatarData>,
    size: Int? = null,
    maxVisible: Int = 3,
    configure: DataHandle<List<AvatarData>>.() -> Unit = {},
): DataHandle<List<AvatarData>> = ContentScope(place(size, Div()), texts, Placement.Body).avatarGroup(data, maxVisible, configure)
