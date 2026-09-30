package tech.testsys.web.ui.data

/**
 * Width of a table column chosen by what it holds; widths come from the design system.
 *
 * @since %CURRENT_VERSION%
 */
enum class ColumnWidth(internal val cssClass: String?) {
    /** As wide as the content needs. */
    Auto(null),

    /** Short values: identifiers, scores, dates. */
    Narrow("ts-col--narrow"),

    /** Middle-length values: names of people, statuses. */
    Medium("ts-col--medium"),

    /** Long values: titles. */
    Wide("ts-col--wide"),

    /** Takes the rest of the row. */
    Fill("ts-col--fill"),
}
