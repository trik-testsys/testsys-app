package tech.testsys.web.devapp.demo.ui

import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope

private const val SHORT_FIELD_COLUMNS = 12
private const val INFO_LABEL_COLUMNS = 4
private const val FULL_FIELD_COLUMNS = 24
private const val FIELDS_PER_ROW = 2

/** Common composition of selectable information fields, pairing short values without reordering them. */
internal fun PageScope.demoInfo(title: String, fields: List<Pair<String, String>>, wideLabels: Set<String> = emptySet()) {
    row {
        block(title = title) {
            demoInfoRows(fields, wideLabels).forEach { fieldsInRow ->
                row {
                    fieldsInRow.forEach { (label, content) ->
                        val columns = if (label in wideLabels) FULL_FIELD_COLUMNS else SHORT_FIELD_COLUMNS
                        textInput(label, labelSize = INFO_LABEL_COLUMNS, size = columns - INFO_LABEL_COLUMNS) {
                            value = content
                            isEditable = false
                        }
                    }
                }
            }
        }
    }
}

/** Groups existing fields while preserving their order and full-width boundaries. */
internal fun demoInfoRows(fields: List<Pair<String, String>>, wideLabels: Set<String>): List<List<Pair<String, String>>> {
    val rows = mutableListOf<List<Pair<String, String>>>()
    val pending = mutableListOf<Pair<String, String>>()

    fun flush() {
        if (pending.isNotEmpty()) {
            rows.add(pending.toList())
            pending.clear()
        }
    }

    fields.forEach { field ->
        if (field.first in wideLabels) {
            flush()
            rows.add(listOf(field))
        } else {
            pending.add(field)
            if (pending.size == FIELDS_PER_ROW) flush()
        }
    }

    flush()
    return rows
}
