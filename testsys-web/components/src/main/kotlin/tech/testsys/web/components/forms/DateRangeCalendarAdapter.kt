@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.react.ReactAdapterComponent
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.DomEventData
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addEventData
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.get
import tech.testsys.web.components.texts.UiTexts
import java.time.LocalDate

private const val DAYS_IN_WEEK = 7

@Tag(DATE_RANGE_CALENDAR_TAG)
@JsModule(DATE_RANGE_CALENDAR_MODULE)
internal class DateRangeCalendarAdapter(private val texts: UiTexts) : ReactAdapterComponent() {
    var onPick: (DateRange) -> Unit = {}

    init {
        element.addEventListener(DomEvent.RangePick) { event ->
            val start = event.eventData.get(DomEventData.DetailStart).takeUnless { node -> node.isNull }?.asString()?.let(LocalDate::parse)
            val end = event.eventData.get(DomEventData.DetailEnd).takeUnless { node -> node.isNull }?.asString()?.let(LocalDate::parse)
            onPick(DateRange(from = start, to = end))
        }.addEventData(DomEventData.DetailStart).addEventData(DomEventData.DetailEnd)
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
