package tech.testsys.web.devapp.demo.model

import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.forms.DateRange
import java.time.LocalDate

/** Row projection of a scoped demonstration list. */
internal data class DemoRow(
    val id: String,
    val title: String,
    val category: String = "",
    val date: LocalDate? = null,
    val detail: String = "",
)

/** Mutable form draft; the accepted criteria are stored separately. */
internal class DemoCriteria(
    var query: String = "",
    var category: String? = null,
    var period: DateRange = DateRange(),
)

/** Filter state retained independently of panel visibility and selection. */
internal class DemoTableState {
    var draft: DemoCriteria = DemoCriteria()
    var applied: DemoCriteria = DemoCriteria()
}

/** Applies criteria after role scoping, retaining original row identities. */
internal fun filteredDemoRows(rows: List<DemoRow>, criteria: DemoCriteria): List<DemoRow> = rows.filter { row ->
    val query = criteria.query.trim()
    val isQueryMatch = listOf(row.id, row.title, row.detail).any { text -> text.contains(query, ignoreCase = true) }
    val isCategoryMatch = criteria.category.isNullOrEmpty() || row.category == criteria.category
    val from = criteria.period.from
    val to = criteria.period.to
    isQueryMatch && isCategoryMatch &&
        (from == null || (row.date != null && row.date >= from)) &&
        (to == null || (row.date != null && row.date <= to))
}

/** Returns a real page and the full filtered count. */
internal fun demoPage(rows: List<DemoRow>, criteria: DemoCriteria, request: PageRequest): Page<DemoRow> {
    val filtered = filteredDemoRows(rows, criteria)
    return Page(filtered.drop(request.offset).take(request.limit), filtered.size)
}
