@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

/** Defaults shared by avatar composition and initial derivation. */
private object AvatarDefaults {
    const val MAX_VISIBLE = 3
    const val INITIALS_LENGTH = 2
    const val TONE_COUNT = 4
    val WORD_SEPARATOR = Regex("\\s+")
}

/** Derives two initials from [name] using the explicitly supplied [locale]. */
internal fun avatarInitials(name: String, locale: java.util.Locale): String = name.trim()
    .split(AvatarDefaults.WORD_SEPARATOR).filter(String::isNotEmpty).take(AvatarDefaults.INITIALS_LENGTH)
    .joinToString("") { word -> word.take(1) }.uppercase(locale)

/** Selects the common geometry for ordinary and overflow avatars. */
private fun Span.avatarGeometry(placement: Placement) {
    setClassName("ts-avatar--compact", placement.isCompact)
}

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
    val root = Span()

    fun render(value: AvatarData) {
        root.text = value.initials ?: avatarInitials(value.name, texts.locale)
        root.element.classList.clear()
        root.addClassNames("ts-avatar", "ts-avatar--t${Math.floorMod(value.name.sumOf { char -> char.code }, AvatarDefaults.TONE_COUNT)}")
        root.avatarGeometry(placement)
        root.setClassName("ts-avatar--square", value.isSquare)
        root.element.setRole(ElementRole.Image)
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
    placeContent(size, Div()).avatar(data, configure)

/**
 * Adds the first [maxVisible] avatars of [data], with an accessible count for the rest.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.avatarGroup(
    data: List<AvatarData>,
    maxVisible: Int = AvatarDefaults.MAX_VISIBLE,
    configure: DataHandle<List<AvatarData>>.() -> Unit = {},
): DataHandle<List<AvatarData>> {
    require(maxVisible > 0) { "Avatar group visible count must be positive, got $maxVisible" }
    val root = Div().apply { addClassName("ts-avatars") }
    fun render(values: List<AvatarData>) {
        root.removeAll()
        values.take(maxVisible).forEach { value -> ContentScope(root, texts, placement, gridColumns).avatar(value) }
        if (values.size > maxVisible) {
            root.add(
                Span("+${values.size - maxVisible}").apply {
                    addClassNames("ts-avatar", "ts-avatar--t3")
                    avatarGeometry(placement)
                    element.setRole(ElementRole.Image)
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
    maxVisible: Int = AvatarDefaults.MAX_VISIBLE,
    configure: DataHandle<List<AvatarData>>.() -> Unit = {},
): DataHandle<List<AvatarData>> = placeContent(size, Div()).avatarGroup(data, maxVisible, configure)
