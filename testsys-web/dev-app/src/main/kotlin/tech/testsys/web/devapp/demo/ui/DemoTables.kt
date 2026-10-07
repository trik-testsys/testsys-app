package tech.testsys.web.devapp.demo.ui

import com.vaadin.flow.data.binder.Binder
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.devapp.demo.model.DemoCriteria
import tech.testsys.web.devapp.demo.model.DemoRow
import tech.testsys.web.devapp.demo.model.DemoTableState
import tech.testsys.web.devapp.demo.model.demoPage

private const val DEMO_PAGE_SIZE = 10

/** Composes filters from ordinary fields and the existing paged table API. */
internal fun BlockScope.demoTable(
    state: DemoTableState,
    rows: List<DemoRow>,
    gridColumns: Int? = null,
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
        gridColumns = gridColumns,
        fetch = { request -> demoPage(rows, state.applied, request) },
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
        onRefresh = { handle.refresh(toFirstPage = true) },
    ) {
        row {
            textInput("Поиск", labelSize = 4, size = 20) {
                binder.forField(this).bind({ target -> target.query }, { target, value -> target.query = value })
                addValueChangeListener { state.draft.query = value }
            }
        }

        val categories = rows.map(DemoRow::category).filter(String::isNotBlank).distinct()
        if (categories.isNotEmpty()) {
            row {
                select(
                    "Тип / состояние",
                    items = listOf("Все") + categories,
                    itemLabel = { category -> category },
                    labelSize = 4,
                    size = 20,
                ) {
                    binder.forField(this).bind(
                        { target -> target.category ?: "Все" },
                        { target, value ->
                            target.category = value.takeUnless { category -> category == "Все" }
                        },
                    )
                    addValueChangeListener {
                        state.draft.category = value.takeUnless { category -> category == "Все" }
                    }
                }
            }
        }

        if (rows.any { row -> row.date != null }) {
            row {
                dateRangeInput("Период", labelSize = 4, size = 20) {
                    binder.forField(this).bind({ target -> target.period }, { target, value -> target.period = value })
                    addValueChangeListener { state.draft.period = value }
                }
            }
        }
    }

    binder.readBean(state.draft)
}

/** Columns of a generic scoped demonstration projection. */
private fun TableScope<DemoRow>.demoDefaultColumns() {
    codeColumn("ID", size = 4) { row -> row.id }
    textColumn("Название", size = 7) { row -> row.title }
    textColumn("Тип / состояние", size = 5) { row -> row.category }
    textColumn("Сведения") { row -> row.detail }
}
