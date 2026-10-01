package tech.testsys.web.components.forms

import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.react.ReactAdapterComponent
import tech.testsys.web.components.UiTexts
import java.time.LocalDate

private const val DAYS_IN_WEEK = 7

@Tag("testsys-date-range-calendar")
@JsModule("./testsys-ui/date-range-calendar.tsx")
internal class DateRangeCalendarAdapter(private val texts: UiTexts) : ReactAdapterComponent() {
    var onPick: (DateRange) -> Unit = {}

    init {
        element.addEventListener("range-pick") { event ->
            val start = event.eventData.get("event.detail.start").takeUnless { node -> node.isNull }?.asString()?.let(LocalDate::parse)
            val end = event.eventData.get("event.detail.end").takeUnless { node -> node.isNull }?.asString()?.let(LocalDate::parse)
            onPick(DateRange(from = start, to = end))
        }.addEventData("event.detail.start").addEventData("event.detail.end")
        present(DateRange())
    }

    fun present(value: DateRange) {
        val calendar = texts.calendar
        val firstDay = calendar.firstDayOfWeek.value % DAYS_IN_WEEK
        setState(
            "calendar",
            mapOf(
                "start" to value.from?.toString(),
                "end" to value.to?.toString(),
                "today" to LocalDate.now().toString(),
                "months" to calendar.monthNames,
                "weekdays" to (0 until DAYS_IN_WEEK).map { day -> calendar.weekdaysShort[(day + firstDay) % DAYS_IN_WEEK] },
                "firstDay" to firstDay,
                "locale" to texts.locale.toLanguageTag(),
                "previousLabel" to texts.components.previousMonth,
                "nextLabel" to texts.components.nextMonth,
            ),
        )
    }
}
