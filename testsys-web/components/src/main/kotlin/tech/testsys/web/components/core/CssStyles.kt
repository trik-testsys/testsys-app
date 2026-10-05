@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.dom.Style

@InternalComponentsApi
internal enum class CssUnit(internal val value: String) {
    Pixels("px"),
    Percent("%"),
}

@InternalComponentsApi
internal enum class CssDisplay(internal val value: String) {
    None("none"),
}

@InternalComponentsApi
internal enum class CssProperty(internal val value: String) {
    Background("background"),
    FontFamily("font-family"),
    FontSize("font-size"),
    FontWeight("font-weight"),
    Padding("padding"),
    BorderRadius("border-radius"),
    BoxShadow("box-shadow"),
    Width("width"),
    Height("height"),
    Display("display"),
    GridColumn("grid-column"),
    GridTemplateColumns("grid-template-columns"),
    TableUsed("--ts-table-used"),
    TableWidth("--ts-table-width"),
    MenuColumns("--ts-menu-columns"),
    DownloadOffset("--ts-download-offset"),
    CodeMinLines("--ts-code-min-lines"),
}

@InternalComponentsApi
internal fun Style.set(property: CssProperty, value: String): Style = set(property.value, value)

@InternalComponentsApi
internal fun Style.setToken(property: CssProperty, tokenName: String): Style = set(property, "var($tokenName)")

@InternalComponentsApi
internal fun Style.setGridColumnSpan(value: Int): Style = set(CssProperty.GridColumn, "span $value")

@InternalComponentsApi
internal fun Style.setGridTemplateColumns(value: Int): Style = set(CssProperty.GridTemplateColumns, "repeat($value, minmax(0, 1fr))")

@InternalComponentsApi
internal fun Style.setWidth(value: Number, unit: CssUnit): Style = set(CssProperty.Width, "$value${unit.value}")

@InternalComponentsApi
internal fun Style.removeWidth(): Style = remove(CssProperty.Width.value)

@InternalComponentsApi
internal fun Style.setHeightPixels(value: Int): Style = set(CssProperty.Height, "${value}px")

@InternalComponentsApi
internal fun Style.setPaddingPixels(value: Int): Style = set(CssProperty.Padding, if (value == 0) "0" else "${value}px")

@InternalComponentsApi
internal fun Style.setDisplay(value: CssDisplay): Style = set(CssProperty.Display, value.value)

@InternalComponentsApi
internal fun Style.setTableUsed(value: Int): Style = set(CssProperty.TableUsed, value.toString())

@InternalComponentsApi
internal fun Style.setTableWidth(value: Number): Style = set(CssProperty.TableWidth, "$value%")

@InternalComponentsApi
internal fun Style.setMenuColumns(value: Int): Style = set(CssProperty.MenuColumns, value.toString())

@InternalComponentsApi
internal fun Style.setDownloadOffset(value: Number): Style = set(CssProperty.DownloadOffset, value.toString())

@InternalComponentsApi
internal fun Style.setCodeMinLines(value: Int): Style = set(CssProperty.CodeMinLines, value.toString())
