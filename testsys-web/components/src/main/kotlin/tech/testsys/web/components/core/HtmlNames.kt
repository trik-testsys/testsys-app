@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.dom.Element

@InternalComponentsApi
internal enum class HtmlTag(internal val value: String) {
    B("b"),
    Col("col"),
    ColGroup("colgroup"),
    I("i"),
    Mark("mark"),
    Small("small"),
    Span("span"),
    Table("table"),
    Tbody("tbody"),
    Td("td"),
    Th("th"),
    Thead("thead"),
    Tr("tr"),
}

@InternalComponentsApi
internal enum class HtmlAttribute(internal val value: String) {
    AriaActiveDescendant("aria-activedescendant"),
    AriaControls("aria-controls"),
    AriaDescribedBy("aria-describedby"),
    AriaLabel("aria-label"),
    AriaLabelledBy("aria-labelledby"),
    AriaValueMax("aria-valuemax"),
    AriaValueMin("aria-valuemin"),
    AriaValueNow("aria-valuenow"),
    Class("class"),
    ColSpan("colspan"),
    DataHeaderKey("data-header-key"),
    DataKey("data-key"),
    DataTsUploadGeneration("data-ts-upload-generation"),
    Placeholder("placeholder"),
    TabIndex("tabindex"),
    Title("title"),
}

@InternalComponentsApi
internal enum class DomProperty(internal val value: String) {
    AccessibleName("accessibleName"),
    AriaLabel("ariaLabel"),
    Disabled("disabled"),
    ReadOnly("readOnly"),
    Value("value"),
}

@InternalComponentsApi
internal fun htmlElement(tag: HtmlTag): Element = Element(tag.value)

@InternalComponentsApi
internal fun Element.setAttribute(name: HtmlAttribute, value: String): Element = setAttribute(name.value, value)

@InternalComponentsApi
internal fun Element.getAttribute(name: HtmlAttribute): String? = getAttribute(name.value)

@InternalComponentsApi
internal fun Element.removeAttribute(name: HtmlAttribute): Element = removeAttribute(name.value)

@InternalComponentsApi
internal fun Element.hasAttribute(name: HtmlAttribute): Boolean = hasAttribute(name.value)

@InternalComponentsApi
internal fun Element.setProperty(name: DomProperty, value: String): Element = setProperty(name.value, value)

@InternalComponentsApi
internal fun Element.setProperty(name: DomProperty, value: Boolean): Element = setProperty(name.value, value)

@InternalComponentsApi
internal fun Element.getProperty(name: DomProperty): String = getProperty(name.value)

@InternalComponentsApi
internal fun Element.getProperty(name: DomProperty, defaultValue: Boolean): Boolean = getProperty(name.value, defaultValue)
