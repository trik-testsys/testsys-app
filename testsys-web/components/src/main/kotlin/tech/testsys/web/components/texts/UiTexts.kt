package tech.testsys.web.components.texts

import java.time.DayOfWeek
import java.util.Locale

/**
 * Built-in texts of the design system components, supplied by the application.
 *
 * @property locale the locale of dates, times and numbers.
 * @property brand the accessible product name in the header and footer.
 * @property footer the texts of the page footer.
 * @property tableFilters the texts of the table filter panel.
 * @property signIn the label of the header sign-in link.
 * @property header the texts of interactive header layers.
 * @property calendar the texts of date pickers.
 * @property fieldErrors the messages of the built-in field constraints.
 * @property dateRangeReversed the error shown when a date range ends before it starts.
 * @property editing the labels of the block editing switch.
 * @property table the texts of data tables.
 * @property pagination the texts of page switchers.
 * @property load the texts of content that failed to load.
 * @property dialog the texts of dialogs.
 * @property lookup the texts of the lookup field and its dialog.
 * @property navigation the accessible names of navigation landmarks.
 * @property menu the texts of action menus.
 * @property dateFields the accessible names and instructions of compound date fields.
 * @property components the texts of selections, overlays and transfers.
 * @property notFound the texts of the missing page screen.
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
    val pagination: PaginationTexts,
    val load: LoadTexts,
    val dialog: DialogTexts,
    val lookup: LookupTexts,
    val navigation: NavigationTexts,
    val menu: MenuTexts,
    val dateFields: DateFieldTexts,
    val notFound: NotFoundTexts,
    val components: ComponentTexts,
    val header: HeaderTexts,
    val footer: FooterTexts,
    val tableFilters: TableFiltersTexts,
)

/**
 * Texts of the table filter panel.
 *
 * @property title the disclosure label and accessible panel name.
 * @property apply the label of the action that applies a validated draft.
 * @property reset the label of the action that restores the page defaults.
 * @since %CURRENT_VERSION%
 */
class TableFiltersTexts(val title: String, val apply: String, val reset: String)

/**
 * Texts of the page footer.
 *
 * @property year formats the displayed year without digit grouping.
 * @property links the accessible name of the footer navigation.
 * @since %CURRENT_VERSION%
 */
class FooterTexts(val year: (Int) -> String, val links: String)

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
 * @property selectAll the accessible name of the checkbox that selects all rows of a page.
 * @property selectRow the accessible name of the checkbox that selects a row.
 * @since %CURRENT_VERSION%
 */
class TableTexts(
    val empty: String,
    val range: (from: Int, to: Int, total: Int) -> String,
    val selectAll: String,
    val selectRow: String,
)

/**
 * Texts of page switchers: the full pagination and the compact pager of data tables.
 *
 * @property previous the accessible name of the previous page button.
 * @property next the accessible name of the next page button.
 * @property page the accessible name of the button of a page, from its number.
 * @since %CURRENT_VERSION%
 */
class PaginationTexts(
    val previous: String,
    val next: String,
    val page: (page: Int) -> String,
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
 * @property remove the label of the button that removes one value of a multi-value lookup, from the value.
 * @property reset the label of the action that unchecks all rows of the multi-value lookup dialog.
 * @property apply the label of the action that makes the checked rows the value of a multi-value lookup.
 * @property selectedCount the count of checked rows of the multi-value lookup dialog, from the count.
 * @since %CURRENT_VERSION%
 */
class LookupTexts(
    val search: String,
    val open: String,
    val clear: String,
    val empty: String,
    val remove: (value: String) -> String,
    val reset: String,
    val apply: String,
    val selectedCount: (count: Int) -> String,
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

/**
 * Accessible names, permanent boundary prefixes and instructions of compound date fields.
 *
 * @property date the suffix of the date part of a date-time field.
 * @property time the suffix of the time part of a date-time field.
 * @property rangeFrom the name of the start picker, from the field label.
 * @property rangeTo the name of the end picker, from the field label.
 * @property rangeFromPrefix the permanent visible prefix inside the start picker.
 * @property rangeToPrefix the permanent visible prefix inside the end picker.
 * @property rangeRequired the instruction for a required range with optional ends.
 * @since %CURRENT_VERSION%
 */
class DateFieldTexts(
    val date: String,
    val time: String,
    val rangeFrom: (String) -> String,
    val rangeTo: (String) -> String,
    val rangeFromPrefix: String,
    val rangeToPrefix: String,
    val rangeRequired: String,
)

/**
 * Texts of the missing page screen.
 *
 * @property title the screen heading.
 * @property description the explanation of the missing page.
 * @property back the label of the action returning through browser history.
 * @property pageTitle the browser title including the brand.
 * @since %CURRENT_VERSION%
 */
class NotFoundTexts(
    val title: String,
    val description: String,
    val back: String,
    val pageTitle: String,
)

/**
 * Built-in labels of selections, calendars, displays and file transfers.
 *
 * @property selectAll the action selecting all visible options.
 * @property previousMonth the previous month action.
 * @property nextMonth the next month action.
 * @property calendar the action opening the range calendar.
 * @property upload the action selecting files.
 * @property drop the hint for dropping files.
 * @property cancel the action cancelling a transfer.
 * @property cancelled the status of a cancelled transfer.
 * @property removeFile the action removing a file from the transfer list.
 * @property stalled the status of a transfer that stopped progressing.
 * @property preparing the download preparation label.
 * @property downloading the transfer label.
 * @property done the completed transfer label.
 * @property failed the failed transfer label.
 * @property retry the retry action.
 * @property downloadAgain the action starting another download.
 * @property uploadRejected the error of files outside the limits.
 * @property loading the accessible name of a skeleton.
 * @property openCalendar the contextual accessible name of a calendar trigger.
 * @property uploadLimits the human-readable file count and size limits.
 * @property downloadLabel the contextual accessible label from an action name and state caption.
 * @property percent the progress percentage.
 * @property byteUnits the localized byte-size unit labels.
 * @property timerUnits the labels of days, hours, minutes and seconds.
 * @property more the accessible count of hidden selected values.
 * @property transferBytes the accessible transferred byte count.
 * @since %CURRENT_VERSION%
 */
class ComponentTexts(
    val selectAll: String,
    val previousMonth: String,
    val nextMonth: String,
    val calendar: String,
    val upload: String,
    val drop: String,
    val cancel: String,
    val cancelled: String,
    val removeFile: String,
    val stalled: String,
    val preparing: String,
    val downloading: String,
    val done: String,
    val failed: String,
    val retry: String,
    val downloadAgain: String,
    val uploadRejected: String,
    val loading: String,
    val openCalendar: (String) -> String,
    val uploadLimits: (Int, Long) -> String,
    val downloadLabel: (String, String) -> String,
    val percent: (Int) -> String,
    val byteUnits: List<String>,
    val timerUnits: List<String>,
    val more: (Int) -> String,
    val transferBytes: (Long) -> String,
)

/**
 * Built-in header labels and announcements; application data labels remain with the application.
 *
 * @property search the search field name and placeholder.
 * @property searchEmpty the empty result message.
 * @property notifications the notification popup heading.
 * @property notificationsEmpty the empty notification message.
 * @property readAll the read-all action.
 * @property unreadCount the accessible name of the bell including the unread count.
 * @property unreadItem the accessible name of an unread notification.
 * @property userMenu the accessible name of the user menu trigger.
 * @property arrivalOpen the action of the arrival card.
 * @property arrivalClose the accessible name of its dismiss button.
 * @property arrivalCount the summary of a batch of arrivals.
 * @property arrivalBatchHint the instruction accompanying a batch summary.
 * @since %CURRENT_VERSION%
 */
class HeaderTexts(
    val search: String,
    val searchEmpty: String,
    val notifications: String,
    val notificationsEmpty: String,
    val readAll: String,
    val unreadCount: (Int) -> String,
    val unreadItem: (String) -> String,
    val userMenu: (String) -> String,
    val arrivalOpen: String,
    val arrivalClose: String,
    val arrivalCount: (Int) -> String,
    val arrivalBatchHint: String,
)
