package tech.testsys.web.components.data

import tech.testsys.web.components.texts.UiLocalization
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Text of a cell without a value. */
internal const val EMPTY_CELL: String = "—"

internal fun formatNumber(value: Number?, texts: UiLocalization): String =
    value?.let { number -> NumberFormat.getInstance(texts.locale).format(number) } ?: EMPTY_CELL

internal fun formatDate(value: LocalDate?, texts: UiLocalization): String =
    value?.format(DateTimeFormatter.ofPattern(texts.dateFormat, texts.locale)) ?: EMPTY_CELL

internal fun formatDateTime(value: LocalDateTime?, texts: UiLocalization): String =
    value?.format(DateTimeFormatter.ofPattern("${texts.dateFormat} ${texts.timeFormat}", texts.locale)) ?: EMPTY_CELL
