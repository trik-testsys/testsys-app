@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Meaning of a tag; the look of the tag follows it.
 *
 * @since %CURRENT_VERSION%
 */
enum class TagKind(internal val cssClass: CssClass?) {
    Topic(null),
    Code(CssClass.TagMono),
    Rating(CssClass.TagDark),
    RatingUp(CssClass.TagUp),
    RatingDown(CssClass.TagDown),
}

/**
 * Adds a small tag, e.g. a topic of a Task or a rating change.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.tag(text: String, kind: TagKind = TagKind.Topic): ElementHandle =
    ElementHandle(buildTag(text, kind).also { tag -> add(tag) })

/**
 * Adds a small tag on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.tag(text: String, kind: TagKind = TagKind.Topic, size: Int? = null): ElementHandle =
    ElementHandle(place(size, buildTag(text, kind)))

private fun buildTag(text: String, kind: TagKind): Span = Span(text).apply {
    addClassName(CssClass.Tag)
    kind.cssClass?.let { modifier -> addClassName(modifier) }
}
