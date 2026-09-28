package tech.testsys.web.ui

import java.time.DayOfWeek
import java.util.Locale

/**
 * Built-in texts of the design system components, resolved by the application from its localization.
 *
 * @property locale the locale of dates, times and numbers.
 * @property brand the product name in the header.
 * @property signIn the label of the header sign-in link.
 * @property calendar the texts of date pickers.
 * @property fieldErrors the messages of the built-in field constraints.
 * @property dateRangeReversed the error shown when a date range ends before it starts.
 * @property editing the labels of the block editing switch.
 * @property table the texts of data tables.
 * @property load the texts of content that failed to load.
 * @property dialog the texts of dialogs.
 * @property lookup the texts of the lookup field and its dialog.
 * @property navigation the accessible names of navigation landmarks.
 * @property menu the texts of action menus.
 * @since %CURRENT_VERSION%
 */
class UiTexts(
    val locale: Locale,
    val brand: String,
    val signIn: String,
    val calendar: CalendarTexts,
    val fieldErrors: FieldErrorTexts,
    val dateRangeReversed: String,
    val editing: EditingTexts,
    val table: TableTexts,
    val load: LoadTexts,
    val dialog: DialogTexts,
    val lookup: LookupTexts,
    val navigation: NavigationTexts,
    val menu: MenuTexts,
)

/**
 * Texts of the date picker calendar.
 *
 * @property monthNames the twelve month names starting from January.
 * @property weekdays the seven weekday names starting from Sunday.
 * @property weekdaysShort the seven short weekday names starting from Sunday.
 * @property firstDayOfWeek the day a calendar week starts with.
 * @property dateFormat the date pattern, e.g. `dd.MM.yyyy`.
 * @property today the label of the button that picks today.
 * @property cancel the label of the button that closes the calendar.
 * @since %CURRENT_VERSION%
 */
class CalendarTexts(
    val monthNames: List<String>,
    val weekdays: List<String>,
    val weekdaysShort: List<String>,
    val firstDayOfWeek: DayOfWeek,
    val dateFormat: String,
    val today: String,
    val cancel: String,
)

/**
 * Messages of the constraints built into date, time and number fields.
 *
 * @property badInput the message for a value that cannot be parsed.
 * @property belowMin the message for a value below the minimum.
 * @property aboveMax the message for a value above the maximum.
 * @property stepMismatch the message for a number off the step.
 * @since %CURRENT_VERSION%
 */
class FieldErrorTexts(
    val badInput: String,
    val belowMin: String,
    val aboveMax: String,
    val stepMismatch: String,
)

/**
 * Labels of the actions that switch a block between viewing and editing.
 *
 * @property start the label of the action that starts editing.
 * @property save the label of the action that saves the changes.
 * @property cancel the label of the action that drops the changes.
 * @since %CURRENT_VERSION%
 */
class EditingTexts(
    val start: String,
    val save: String,
    val cancel: String,
)

/**
 * Texts of data tables.
 *
 * @property empty the text of a table without rows.
 * @property range the pagination label of shown rows, e.g. "1–20 of 1 412", from `from`, `to` and `total`.
 * @property previous the label of the previous page button.
 * @property next the label of the next page button.
 * @property selectAll the accessible name of the checkbox that selects all rows of a page.
 * @property selectRow the accessible name of the checkbox that selects a row.
 * @since %CURRENT_VERSION%
 */
class TableTexts(
    val empty: String,
    val range: (from: Int, to: Int, total: Int) -> String,
    val previous: String,
    val next: String,
    val selectAll: String,
    val selectRow: String,
)

/**
 * Texts of content that failed to load: a table page or a block loaded in the background.
 *
 * @property failed the title of the load failure.
 * @property failedHint the hint under the title.
 * @property retry the label of the action that loads again.
 * @since %CURRENT_VERSION%
 */
class LoadTexts(
    val failed: String,
    val failedHint: String,
    val retry: String,
)

/**
 * Texts of dialogs.
 *
 * @property cancel the label of the action that closes a confirmation without acting.
 * @property close the label of the close button in a dialog head.
 * @property typeToConfirm the prompt to type a name to confirm a dangerous action, from the name.
 * @since %CURRENT_VERSION%
 */
class DialogTexts(
    val cancel: String,
    val close: String,
    val typeToConfirm: (name: String) -> String,
)

/**
 * Texts of the lookup field and its dialog.
 *
 * @property search the placeholder of the search line.
 * @property open the label of the button that opens the lookup dialog.
 * @property clear the label of the button that clears the value.
 * @property empty the text shown when the search finds nothing.
 * @since %CURRENT_VERSION%
 */
class LookupTexts(
    val search: String,
    val open: String,
    val clear: String,
    val empty: String,
)

/**
 * Accessible names of navigation landmarks.
 *
 * @property breadcrumbs the name of the breadcrumb trail of a page.
 * @property sections the name of the page tabs that switch between the sections of one object.
 * @since %CURRENT_VERSION%
 */
class NavigationTexts(
    val breadcrumbs: String,
    val sections: String,
)

/**
 * Texts of action menus.
 *
 * @property actions the accessible name of the button that opens an action menu.
 * @since %CURRENT_VERSION%
 */
class MenuTexts(
    val actions: String,
)
