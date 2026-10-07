package tech.testsys.web.components.texts

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Builds the current Russian design system texts with the standard locale data.
 *
 * @since %CURRENT_VERSION%
 */
fun buildUiTexts(): UiTexts {
    val locale = Locale.forLanguageTag("ru-RU")
    val weekdays = DayOfWeek.entries.sortedBy { day -> day.value % DayOfWeek.entries.size }
    return UiTexts(
        locale = locale,
        brand = "TestSys",
        signIn = "Войти",
        footer = FooterTexts(year = { year -> year.toString() }, links = "Ссылки футтера"),
        tableFilters = TableFiltersTexts(title = "Фильтры", apply = "Применить", reset = "Сбросить"),
        calendar = CalendarTexts(
            monthNames = Month.entries.map { month ->
                month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { letter -> letter.titlecase(locale) }
            },
            weekdays = weekdays.map { day -> day.getDisplayName(TextStyle.FULL, locale) },
            weekdaysShort = weekdays.map { day -> day.getDisplayName(TextStyle.SHORT_STANDALONE, locale) },
            firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek,
            dateFormat = "dd.MM.yyyy",
            today = "Сегодня",
            cancel = "Отменить",
        ),
        fieldErrors = FieldErrorTexts(
            badInput = "Проверьте формат значения",
            belowMin = "Значение меньше допустимого",
            aboveMax = "Значение больше допустимого",
            stepMismatch = "Значение не соответствует шагу",
        ),
        dateRangeReversed = "Дата окончания раньше даты начала",
        editing = EditingTexts(start = "Изменить", save = "Сохранить", cancel = "Отменить"),
        table = TableTexts(
            empty = "Нет данных",
            range = { from, to, total -> "${formatNumber(from)}–${formatNumber(to)} из ${formatNumber(total)}" },
            selectAll = "Выбрать все строки страницы",
            selectRow = "Выбрать строку",
        ),
        pagination = PaginationTexts(
            previous = "Назад",
            next = "Вперёд",
            page = { page -> "Страница ${formatNumber(page)}" },
        ),
        load = LoadTexts(
            failed = "Не удалось загрузить",
            failedHint = "Попробуйте ещё раз",
            retry = "Повторить",
        ),
        dialog = DialogTexts(
            cancel = "Отменить",
            close = "Закрыть",
            typeToConfirm = { name -> "Введите «$name», чтобы подтвердить" },
        ),
        lookup = LookupTexts(
            search = "Поиск",
            open = "Выбрать",
            clear = "Очистить",
            empty = "Ничего не найдено",
            remove = { value -> "Убрать $value" },
            reset = "Сбросить",
            apply = "Применить",
            selectedCount = { count -> "Выбрано: ${formatNumber(count)}" },
        ),
        navigation = NavigationTexts(breadcrumbs = "Навигационная цепочка", sections = "Разделы"),
        menu = MenuTexts(actions = "Действия"),
        dateFields = DateFieldTexts(
            date = "дата",
            time = "время",
            rangeFrom = { label -> "$label: с" },
            rangeTo = { label -> "$label: до" },
            rangeFromPrefix = "С",
            rangeToPrefix = "До",
            rangeRequired = "Укажите хотя бы одну границу периода",
        ),
        components = ComponentTexts(
            selectAll = "Выбрать все",
            previousMonth = "Предыдущий месяц",
            nextMonth = "Следующий месяц",
            calendar = "Открыть календарь",
            drag = "Переместить: пробел, затем стрелки; Enter применяет, Escape отменяет",
            upload = "Выбрать файлы",
            drop = "Перетащите файлы сюда",
            cancel = "Отменить",
            cancelled = "Отменено",
            removeFile = "Убрать файл",
            stalled = "Передача приостановлена",
            preparing = "Подготовка",
            downloading = "Передача",
            done = "Передано сервером",
            failed = "Ошибка передачи",
            retry = "Повторить",
            downloadAgain = "Скачать снова",
            uploadRejected = "Файл не соответствует ограничениям",
            loading = "Загрузка",
            overflow = "Другие участники",
            openCalendar = { label -> "Открыть календарь: $label" },
            uploadLimits = { count, bytes ->
                val files = plural(count, one = "файла", few = "файлов", many = "файлов")
                "До ${formatNumber(count)} $files, до ${formatNumber(bytes)} Б каждый"
            },
            difficultyLabels = listOf("Лёгкая", "Средняя", "Сложная"),
            questionStatus = { number, answered, flagged ->
                buildString {
                    append("Вопрос ${formatNumber(number)}")
                    if (answered) append(", отвечен")
                    if (flagged) append(", отмечен")
                }
            },
            reorderPosition = { label, position, total -> "$label: позиция ${formatNumber(position)} из ${formatNumber(total)}" },
            downloadLabel = { label, state -> "$label: $state" },
            percent = { value -> "${formatNumber(value)}%" },
            byteUnits = listOf(
                "Б",
                "КБ",
                "МБ",
                "ГБ",
            ),
            timerUnits = listOf(
                "дни",
                "часы",
                "минуты",
                "секунды",
            ),
            avatarOverflow = { count ->
                val participants = plural(count, one = "участник", few = "участника", many = "участников")
                "Ещё ${formatNumber(count)} $participants"
            },
            more = { count -> "Ещё ${formatNumber(count)}" },
            transferBytes = { count -> "Передано: ${formatNumber(count)} Б" },
            question = { number -> "Вопрос ${formatNumber(number)}" },
        ),
        header = HeaderTexts(
            search = "Поиск задач, соревнований…",
            searchLoading = "Поиск…",
            searchEmpty = "Ничего не найдено",
            searchFailed = "Не удалось выполнить поиск",
            searchCount = { count -> "Найдено: ${formatNumber(count)}" },
            retry = "Повторить",
            notifications = "Уведомления",
            notificationsEmpty = "Нет уведомлений",
            readAll = "Прочитать все",
            unreadCount = { count -> "Уведомления: непрочитанных ${formatNumber(count)}" },
            unreadItem = { label -> "$label — не прочитано" },
            userMenu = { name -> "Меню пользователя: $name" },
            arrivalOpen = "Открыть",
            arrivalClose = "Закрыть уведомление",
            arrivalCount = { count ->
                val notifications =
                    plural(count, one = "новое уведомление", few = "новых уведомления", many = "новых уведомлений")
                "Получено ${formatNumber(count)} $notifications"
            },
            arrivalBatchHint = "Откройте список уведомлений, чтобы посмотреть новые сообщения.",
        ),
        notFound = NotFoundTexts(
            title = "Страница не найдена",
            description = "Проверьте адрес или вернитесь на предыдущую страницу.",
            back = "Вернуться",
            pageTitle = "Страница не найдена — TestSys",
        ),
    )
}

private fun formatNumber(value: Number): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU")).format(value)

@Suppress("MagicNumber")
private fun plural(count: Int, one: String, few: String, many: String): String {
    val absoluteCount = count.toLong().absoluteValue
    return when {
        absoluteCount % 100 in 11..14 -> many
        absoluteCount % 10 == 1L -> one
        absoluteCount % 10 in 2..4 -> few
        else -> many
    }
}
