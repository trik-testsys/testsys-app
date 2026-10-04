package tech.testsys.web.devapp.demo

import com.vaadin.flow.data.binder.Binder
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import java.time.LocalDate

private const val DEMO_PAGE_SIZE = 10

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

/** Composes filters from ordinary fields and the existing paged table API. */
internal fun BlockScope.demoTable(
    state: DemoTableState,
    rows: List<DemoRow>,
    columns: TableScope<DemoRow>.() -> Unit = { demoDefaultColumns() },
    onSelect: (DemoRow) -> Unit = {},
) {
    val binder = Binder<DemoCriteria>()
    lateinit var handle: TableHandle<DemoRow>
    fun restoreDefaults() {
        state.draft = DemoCriteria()
        state.applied = DemoCriteria()
        binder.readBean(state.draft)
    }
    handle = table(
        key = DemoRow::id,
        pageSize = DEMO_PAGE_SIZE,
        fetch = {
            demoPage(rows, state.applied, it)
        },
    ) {
        columns()
        if (rows.isNotEmpty()) {
            empty("Ничего не найдено", description = "Измените или сбросьте фильтры, чтобы увидеть записи.") {
                action("Сбросить фильтры") {
                    onClick {
                        restoreDefaults()
                        handle.refresh(toFirstPage = true)
                    }
                }
            }
        } else {
            empty("Нет доступных объектов", description = "В этом демонстрационном контексте список пуст.")
        }
        onRowClick(onSelect)
    }
    filters(
        onApply = {
            val candidate = DemoCriteria()
            binder.writeBeanIfValid(candidate).also { valid ->
                if (valid) {
                    state.applied = candidate
                }
            }
        },
        onReset = { restoreDefaults() },
        onRefresh = {
            handle.refresh(toFirstPage = true)
        },
    ) {
        row {
            textInput("Поиск", labelSize = 4, size = 20) {
                binder.forField(this).bind(
                    {
                        it.query
                    },
                    { target, value ->
                        target.query = value
                    },
                )
                addValueChangeListener {
                    state.draft.query = value
                }
            }
        }
        val categories = rows.map {
            it.category
        }.filter {
            it.isNotBlank()
        }.distinct()
        if (categories.isNotEmpty()) {
            row {
                select(
                    "Тип / состояние",
                    items = listOf("Все") + categories,
                    itemLabel = {
                        it
                    },
                    labelSize = 4,
                    size = 20,
                ) {
                    binder.forField(this).bind(
                        {
                            it.category ?: "Все"
                        },
                        { target, value ->
                            target.category = value.takeUnless {
                                it == "Все"
                            }
                        },
                    )
                    addValueChangeListener {
                        state.draft.category = value.takeUnless { category ->
                            category == "Все"
                        }
                    }
                }
            }
        }
        if (
            rows.any {
                it.date != null
            }
        ) {
            row {
                dateRangeInput("Период", labelSize = 4, size = 20) {
                    binder.forField(this).bind(
                        {
                            it.period
                        },
                        { target, value ->
                            target.period = value
                        },
                    )
                    addValueChangeListener {
                        state.draft.period = value
                    }
                }
            }
        }
    }
    binder.readBean(state.draft)
}

/** Columns of a generic scoped demonstration projection. */
private fun TableScope<DemoRow>.demoDefaultColumns() {
    codeColumn("ID") { row -> row.id }
    textColumn("Название") { row -> row.title }
    textColumn("Тип / состояние") { row -> row.category }
    textColumn("Сведения") { row -> row.detail }
}
