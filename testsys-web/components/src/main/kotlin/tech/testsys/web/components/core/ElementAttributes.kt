@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.dom.Element

@InternalComponentsApi
internal enum class ElementType(internal val attribute: String) {
    Button("button"),
    Search("search"),
}

@InternalComponentsApi
internal enum class ElementRole(internal val attribute: String) {
    Alert("alert"),
    AlertDialog("alertdialog"),
    Dialog("dialog"),
    Group("group"),
    Image("img"),
    Link("link"),
    Menu("menu"),
    MenuItem("menuitem"),
    Presentation("presentation"),
    ProgressBar("progressbar"),
    Region("region"),
    SearchBox("searchbox"),
    Separator("separator"),
    Status("status"),
    Timer("timer"),
}

@InternalComponentsApi
internal enum class ElementScope(internal val attribute: String) {
    Col("col"),
    Row("row"),
}

@InternalComponentsApi
internal enum class AriaCurrent(internal val attribute: String) {
    Page("page"),
    Location("location"),
    Step("step"),
}

@InternalComponentsApi
internal enum class AriaPopup(internal val attribute: String) {
    Dialog("dialog"),
    Menu("menu"),
}

@InternalComponentsApi
internal enum class AriaLive(internal val attribute: String) {
    Polite("polite"),
}

@InternalComponentsApi
internal enum class AriaSort(internal val attribute: String) {
    None("none"),
    Ascending("ascending"),
    Descending("descending"),
}

@InternalComponentsApi
internal enum class Autocomplete(internal val attribute: String) {
    Off("off"),
}

@InternalComponentsApi
internal enum class ElementSize(internal val attribute: String) {
    Small("sm"),
    Medium("md"),
}

@InternalComponentsApi
internal fun Element.setType(value: ElementType): Element = setAttribute("type", value.attribute)

@InternalComponentsApi
internal fun Element.setRole(value: ElementRole?): Element = setOrRemove(name = "role", value = value?.attribute)

@InternalComponentsApi
internal fun Popover.setAriaRole(value: ElementRole) = setAriaRole(value.attribute)

@InternalComponentsApi
internal fun Dialog.setAriaRole(value: ElementRole) = setAriaRole(value.attribute)

@InternalComponentsApi
internal fun Element.setScope(value: ElementScope): Element = setAttribute("scope", value.attribute)

@InternalComponentsApi
internal fun Element.setAriaCurrent(value: AriaCurrent?): Element = setOrRemove(name = "aria-current", value = value?.attribute)

@InternalComponentsApi
internal fun Element.setAriaHasPopup(value: AriaPopup?): Element = setOrRemove(name = "aria-haspopup", value = value?.attribute)

@InternalComponentsApi
internal fun Element.setAriaLive(value: AriaLive): Element = setAttribute("aria-live", value.attribute)

@InternalComponentsApi
internal fun Element.setAriaSort(value: AriaSort): Element = setAttribute("aria-sort", value.attribute)

@InternalComponentsApi
internal fun Element.setAutocomplete(value: Autocomplete): Element = setAttribute("autocomplete", value.attribute)

@InternalComponentsApi
internal fun Element.setSize(value: ElementSize): Element = setAttribute("data-ts-size", value.attribute)

@InternalComponentsApi
internal fun Element.setAriaBusy(value: Boolean?): Element = setOrRemove(name = "aria-busy", value = value?.toString())

@InternalComponentsApi
internal fun Element.setAriaDisabled(value: Boolean?): Element = setOrRemove(name = "aria-disabled", value = value?.toString())

@InternalComponentsApi
internal fun Element.setAriaExpanded(value: Boolean?): Element = setOrRemove(name = "aria-expanded", value = value?.toString())

@InternalComponentsApi
internal fun Element.setAriaHidden(value: Boolean?): Element = setOrRemove(name = "aria-hidden", value = value?.toString())

@InternalComponentsApi
internal fun Element.setAriaInvalid(value: Boolean?): Element = setOrRemove(name = "aria-invalid", value = value?.toString())

@InternalComponentsApi
internal fun Element.setAriaPressed(value: Boolean?): Element = setOrRemove(name = "aria-pressed", value = value?.toString())

@InternalComponentsApi
internal fun Element.setAriaRequired(value: Boolean?): Element = setOrRemove(name = "aria-required", value = value?.toString())

@InternalComponentsApi
internal fun Element.setAnswered(value: Boolean?): Element = setOrRemove(name = "data-answered", value = value?.toString())

@InternalComponentsApi
internal fun Element.setFlagged(value: Boolean?): Element = setOrRemove(name = "data-flagged", value = value?.toString())

@InternalComponentsApi
internal fun Element.setHidden(value: Boolean): Element = setAttribute("hidden", value)

@InternalComponentsApi
internal fun Element.setReadOnly(value: Boolean): Element = setAttribute("readonly", value)

@InternalComponentsApi
internal fun Element.setHighlight(value: Boolean): Element = setAttribute("highlight", value)

@InternalComponentsApi
internal fun Element.setInput(value: Boolean): Element = setAttribute("data-ts-input", value)

@InternalComponentsApi
internal fun Element.setMono(value: Boolean): Element = setAttribute("data-ts-mono", value)

@InternalComponentsApi
internal fun Element.setIconOnly(value: Boolean): Element = setAttribute("data-ts-icon-only", value)

@InternalComponentsApi
internal fun Element.setObscured(value: Boolean): Element = setAttribute("data-ts-obscured", value)

@InternalComponentsApi
internal fun Element.setEditingBody(value: Boolean): Element = setAttribute("data-ts-editing-body", value)

@InternalComponentsApi
internal fun Element.setRangePart(value: Boolean): Element = setAttribute("data-ts-range-part", value)

@InternalComponentsApi
internal fun Element.setSpellcheckPresence(value: Boolean): Element = setAttribute("spellcheck", value)

@InternalComponentsApi
internal fun Element.getAriaExpanded(): Boolean? = getAttribute("aria-expanded")?.toBooleanStrictOrNull()

@InternalComponentsApi
internal fun Element.hasAriaBusy(): Boolean = hasAttribute("aria-busy")

private fun Element.setOrRemove(name: String, value: String?): Element =
    if (value == null) removeAttribute(name) else setAttribute(name, value)
