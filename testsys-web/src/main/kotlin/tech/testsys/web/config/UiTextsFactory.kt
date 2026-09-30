package tech.testsys.web.config

import com.ibm.icu.text.DateFormatSymbols
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.Localization
import tech.testsys.infra.localization.Ui.ComponentsQuestionStatusAnswered
import tech.testsys.infra.localization.Ui.ComponentsQuestionStatusFlagged
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.web.ui.CalendarTexts
import tech.testsys.web.ui.ComponentTexts
import tech.testsys.web.ui.DateFieldTexts
import tech.testsys.web.ui.DialogTexts
import tech.testsys.web.ui.EditingTexts
import tech.testsys.web.ui.FieldErrorTexts
import tech.testsys.web.ui.HeaderTexts
import tech.testsys.web.ui.LoadTexts
import tech.testsys.web.ui.LookupTexts
import tech.testsys.web.ui.MenuTexts
import tech.testsys.web.ui.NavigationTexts
import tech.testsys.web.ui.NotFoundTexts
import tech.testsys.web.ui.PaginationTexts
import tech.testsys.web.ui.TableTexts
import tech.testsys.web.ui.UiTexts
import java.time.DayOfWeek

/**
 * Builds the design system texts of [region] from the localization bundles and the ICU locale data.
 *
 * @since %CURRENT_VERSION%
 */
fun buildUiTexts(region: SupportedRegion): UiTexts {
    val ui = Localization.forRegion(region).ui
    val uLocale = region.toULocale()
    val locale = uLocale.toLocale()
    val symbols = DateFormatSymbols(uLocale)
    return UiTexts(
        locale = locale,
        brand = ui.brand(),
        signIn = ui.signIn(),
        calendar = CalendarTexts(
            monthNames = symbols.getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE)
                .map { month -> month.replaceFirstChar { letter -> letter.titlecase(locale) } },
            weekdays = symbols.getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.WIDE).drop(1),
            weekdaysShort = symbols.getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.SHORT).drop(1),
            firstDayOfWeek = firstDayOfWeek(uLocale),
            dateFormat = dateFormat(region),
            today = ui.calendarToday(),
            cancel = ui.calendarCancel(),
        ),
        fieldErrors = FieldErrorTexts(
            badInput = ui.fieldBadInput(),
            belowMin = ui.fieldBelowMin(),
            aboveMax = ui.fieldAboveMax(),
            stepMismatch = ui.fieldStepMismatch(),
        ),
        dateRangeReversed = ui.dateRangeReversed(),
        editing = EditingTexts(start = ui.editStart(), save = ui.editSave(), cancel = ui.editCancel()),
        table = TableTexts(
            empty = ui.tableEmpty(),
            range = { from, to, total -> ui.tableRange(from = from, to = to, total = total) },
            selectAll = ui.tableSelectAll(),
            selectRow = ui.tableSelectRow(),
        ),
        pagination = PaginationTexts(
            previous = ui.paginationPrevious(),
            next = ui.paginationNext(),
            page = { page -> ui.paginationPage(page = page) },
        ),
        load = LoadTexts(
            failed = ui.loadFailed(),
            failedHint = ui.loadFailedHint(),
            retry = ui.loadRetry(),
        ),
        dialog = DialogTexts(
            cancel = ui.dialogCancel(),
            close = ui.dialogClose(),
            typeToConfirm = { name -> ui.dialogTypeToConfirm(name = name) },
        ),
        lookup = LookupTexts(
            search = ui.lookupSearch(),
            open = ui.lookupOpen(),
            clear = ui.lookupClear(),
            empty = ui.lookupEmpty(),
            remove = { value -> ui.lookupRemove(value = value) },
            reset = ui.lookupReset(),
            apply = ui.lookupApply(),
            selectedCount = { count -> ui.lookupSelectedCount(count = count) },
        ),
        navigation = NavigationTexts(breadcrumbs = ui.navBreadcrumbs(), sections = ui.navSections()),
        menu = MenuTexts(actions = ui.menuActions()),
        dateFields = DateFieldTexts(
            date = ui.dateTimeDate(),
            time = ui.dateTimeTime(),
            rangeFrom = { label -> ui.dateRangeFrom(label = label) },
            rangeTo = { label -> ui.dateRangeTo(label = label) },
            rangeFromPrefix = ui.dateRangeFromPrefix(),
            rangeToPrefix = ui.dateRangeToPrefix(),
            rangeRequired = ui.dateRangeRequired(),
        ),
        components = ComponentTexts(
            selectAll = ui.componentsSelectAll(),
            previousMonth = ui.componentsPreviousMonth(),
            nextMonth = ui.componentsNextMonth(),
            calendar = ui.componentsCalendar(),
            drag = ui.componentsDrag(),
            upload = ui.componentsUpload(),
            drop = ui.componentsDrop(),
            cancel = ui.componentsCancel(),
            preparing = ui.componentsPreparing(),
            downloading = ui.componentsDownloading(),
            done = ui.componentsDone(),
            failed = ui.componentsFailed(),
            retry = ui.componentsRetry(),
            downloadAgain = ui.componentsDownloadAgain(),
            uploadRejected = ui.componentsUploadRejected(),
            loading = ui.componentsLoading(),
            overflow = ui.componentsOverflow(),
            openCalendar = { label -> ui.componentsOpenCalendar(label = label) },
            uploadLimits = { count, bytes -> ui.componentsUploadLimits(count = count, bytes = bytes) },
            difficultyLabels = listOf(ui.componentsDifficultyEasy(), ui.componentsDifficultyMedium(), ui.componentsDifficultyHard()),
            questionStatus = { number, answered, flagged ->
                ui.componentsQuestionStatus(
                    number = number,
                    answered = if (answered) ComponentsQuestionStatusAnswered.TRUE else ComponentsQuestionStatusAnswered.OTHER,
                    flagged = if (flagged) ComponentsQuestionStatusFlagged.TRUE else ComponentsQuestionStatusFlagged.OTHER,
                )
            },
            reorderPosition = { label, position, total -> ui.componentsReorderPosition(label = label, position = position, total = total) },
            downloadLabel = { label, state -> ui.componentsDownloadLabel(label = label, state = state) },
            percent = { value -> ui.componentsPercent(value = value) },
            byteUnits = listOf(
                ui.componentsByteUnit(),
                ui.componentsKilobyteUnit(),
                ui.componentsMegabyteUnit(),
                ui.componentsGigabyteUnit(),
            ),
            timerUnits = listOf(
                ui.componentsTimerDays(),
                ui.componentsTimerHours(),
                ui.componentsTimerMinutes(),
                ui.componentsTimerSeconds(),
            ),
            avatarOverflow = { count -> ui.componentsAvatarOverflow(count = count) },
            transferBytes = { count -> ui.componentsTransferBytes(count = count) },
            question = { number -> ui.componentsQuestion(number = number) },
        ),
        header = HeaderTexts(
            search = ui.headerSearch(),
            searchLoading = ui.headerSearchLoading(),
            searchEmpty = ui.headerSearchEmpty(),
            searchFailed = ui.headerSearchFailed(),
            searchCount = { count -> ui.headerSearchCount(count = count) },
            retry = ui.headerRetry(),
            notifications = ui.headerNotifications(),
            notificationsEmpty = ui.headerNotificationsEmpty(),
            readAll = ui.headerReadAll(),
            unreadCount = { count -> ui.headerUnreadCount(count = count) },
            unreadItem = { label -> ui.headerUnreadItem(label = label) },
            userMenu = { name -> ui.headerUserMenu(name = name) },
            arrivalOpen = ui.headerArrivalOpen(),
            arrivalClose = ui.headerArrivalClose(),
            arrivalCount = { count -> ui.headerArrivalCount(count = count) },
            arrivalBatchHint = ui.headerArrivalBatchHint(),
        ),
        notFound = NotFoundTexts(
            title = ui.notFoundTitle(),
            description = ui.notFoundDescription(),
            back = ui.notFoundBack(),
            pageTitle = ui.notFoundPageTitle(brand = ui.brand()),
        ),
    )
}

// ICU numbers weekdays from Sunday = 1; java.time starts from Monday.
private fun firstDayOfWeek(locale: ULocale): DayOfWeek =
    DayOfWeek.SUNDAY.plus((Calendar.getInstance(locale).firstDayOfWeek - Calendar.SUNDAY).toLong())

private fun dateFormat(region: SupportedRegion): String = when (region) {
    SupportedRegion.RU -> "dd.MM.yyyy"
}
