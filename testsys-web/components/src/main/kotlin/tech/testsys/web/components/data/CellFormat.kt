package tech.testsys.web.components.data

import tech.testsys.web.components.texts.UiTexts
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Text of a cell without a value. */
internal const val EMPTY_CELL: String = "—"

private const val TIME_PATTERN = "HH:mm"

internal fun formatNumber(value: Number?, texts: UiTexts): String =
    value?.let { number -> NumberFormat.getInstance(texts.locale).format(number) } ?: EMPTY_CELL

internal fun formatDate(value: LocalDate?, texts: UiTexts): String =
    value?.format(DateTimeFormatter.ofPattern(texts.calendar.dateFormat, texts.locale)) ?: EMPTY_CELL

internal fun formatDateTime(value: LocalDateTime?, texts: UiTexts): String =
    value?.format(DateTimeFormatter.ofPattern("${texts.calendar.dateFormat} $TIME_PATTERN", texts.locale)) ?: EMPTY_CELL
