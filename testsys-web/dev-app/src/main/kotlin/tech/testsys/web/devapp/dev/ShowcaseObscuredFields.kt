package tech.testsys.web.devapp.dev

import com.vaadin.flow.signals.local.ValueSignal
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.components.forms.MultiSelectDisplay
import tech.testsys.web.components.forms.checkbox
import tech.testsys.web.components.forms.codeEditor
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateInput
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.decimalInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.lookup
import tech.testsys.web.components.forms.lookupMany
import tech.testsys.web.components.forms.multiSelect
import tech.testsys.web.components.forms.radio
import tech.testsys.web.components.forms.segmentedControl
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.switchInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.forms.timeInput
import tech.testsys.web.components.layout.PageScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private object ObscuredSamples {
    const val LABEL = 4
    const val HALF_VALUE = 8
    const val FULL_VALUE = 20
    const val EDITOR_LINES = 3
    const val NUMBER = 42
    const val DECIMAL = 3.14
    val DAY: LocalDate = LocalDate.parse("2026-10-05")
    val LAST_DAY: LocalDate = LocalDate.parse("2026-10-12")
    val TIME: LocalTime = LocalTime.parse("10:30")
    val OPTIONS = listOf("Практика", "Контроль", "Проект")
}

/** Shows obscuring across the existing field families using one shared signal and ordinary field configuration. */
internal fun PageScope.obscuredFields() {
    val obscured = ValueSignal(true)
    block(title = "Скрытые значения", subtitle = "Наведите указатель на значение или перейдите к нему клавишей Tab") {
        actions { action("Переключить скрытие") { onClick { obscured.set(!obscured.peek()) } } }
        row {
            textInput("Обычное значение", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) { value = "Открытый текст" }
            textInput(
                "Скрытый текст",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                hint = "Подсказка остаётся читаемой",
            ) {
                value = "Секретное значение"
                bindObscured(obscured)
            }
        }
        row {
            textInput("Скрытое только чтение", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = "КД-1234"
                isEditable = false
                bindObscured(obscured)
            }
            textInput("Скрытое выключенное", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = "Недоступно для правки"
                isEnabled = false
                bindObscured(obscured)
            }
        }
        row {
            codeInput("Скрытый код", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = "ACCESS-1234"
                bindObscured(obscured)
            }
            textArea("Скрытое описание", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = "Первая строка\nВторая строка"
                bindObscured(obscured)
            }
        }
        row {
            integerInput("Скрытое целое", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = ObscuredSamples.NUMBER
                bindObscured(obscured)
            }
            decimalInput("Скрытое число", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = ObscuredSamples.DECIMAL
                bindObscured(obscured)
            }
        }
        row {
            dateInput("Скрытая дата", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = ObscuredSamples.DAY
                bindObscured(obscured)
            }
            timeInput("Скрытое время", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = ObscuredSamples.TIME
                bindObscured(obscured)
            }
        }
        row {
            dateTimeInput("Скрытая дата и время", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.FULL_VALUE) {
                value = LocalDateTime.of(ObscuredSamples.DAY, ObscuredSamples.TIME)
                bindObscured(obscured)
            }
        }
        row {
            dateRangeInput("Скрытый диапазон", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.FULL_VALUE) {
                value = DateRange(from = ObscuredSamples.DAY, to = ObscuredSamples.LAST_DAY)
                bindObscured(obscured)
            }
        }
        row {
            checkbox("Скрытый флажок", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = true
                bindObscured(obscured)
            }
            switchInput("Скрытый переключатель", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                value = true
                bindObscured(obscured)
            }
        }
        row {
            radio(
                "Скрытый выбор",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                items = ObscuredSamples.OPTIONS,
                itemLabel = { item -> item },
            ) {
                value = ObscuredSamples.OPTIONS.first()
                bindObscured(obscured)
            }
            segmentedControl(
                "Скрытые сегменты",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                items = ObscuredSamples.OPTIONS,
                itemLabel = { item -> item },
            ) {
                value = ObscuredSamples.OPTIONS.first()
                bindObscured(obscured)
            }
        }
        row {
            select(
                "Скрытый список",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                items = ObscuredSamples.OPTIONS,
                itemLabel = { item -> item },
            ) {
                value = ObscuredSamples.OPTIONS.first()
                bindObscured(obscured)
            }
            multiSelect(
                "Скрытые теги",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                items = ObscuredSamples.OPTIONS,
                itemLabel = { item -> item },
            ) {
                value = ObscuredSamples.OPTIONS.toSet()
                bindObscured(obscured)
            }
        }
        row {
            multiSelect(
                "Скрытый счётчик",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                items = ObscuredSamples.OPTIONS,
                itemLabel = { item -> item },
                display = MultiSelectDisplay.Count,
            ) {
                value = ObscuredSamples.OPTIONS.toSet()
                bindObscured(obscured)
            }
            field("Скрытая информация", labelSize = ObscuredSamples.LABEL, size = ObscuredSamples.HALF_VALUE) {
                horizontal {
                    text("КД-5678")
                    vertical {
                        tag("Доступ")
                        action("Проверить действие") { onClick { toast(FeedbackKind.Success, "Действие доступно") } }
                    }
                }
            }.bindObscured(obscured)
        }
        row {
            codeEditor(
                "Скрытый редактор",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.FULL_VALUE,
                minLines = ObscuredSamples.EDITOR_LINES,
                hint = "Ошибка и подсказка видны независимо от размытия",
            ) {
                value = "print('TestSys')\nreturn 42"
                errorMessage = "Пример ошибки проверки"
                isInvalid = true
                bindObscured(obscured)
            }
        }
        row {
            lookup(
                "Скрытый объект",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                fetch = { _, _ -> Page(ObscuredSamples.OPTIONS, ObscuredSamples.OPTIONS.size) },
                display = { item -> item },
                columns = { textColumn("Название") { item -> item } },
            ) {
                value = ObscuredSamples.OPTIONS.first()
                bindObscured(obscured)
            }
            lookupMany(
                "Скрытые объекты",
                labelSize = ObscuredSamples.LABEL,
                size = ObscuredSamples.HALF_VALUE,
                fetch = { _, _ -> Page(ObscuredSamples.OPTIONS, ObscuredSamples.OPTIONS.size) },
                display = { item -> item },
                columns = { textColumn("Название") { item -> item } },
            ) {
                value = ObscuredSamples.OPTIONS.toSet()
                bindObscured(obscured)
            }
        }
    }
    unlabeledObscuredFields(obscured)
}

private fun PageScope.unlabeledObscuredFields(obscured: ValueSignal<Boolean>) {
    val enabled = ValueSignal(false)
    block(title = "Скрытие без подписи", subtitle = "Компактные списки сохраняют собственный порядок фокуса") {
        actions {
            action("Переключить скрытие списков") { onClick { obscured.set(!obscured.peek()) } }
            action("Переключить доступность списков") { onClick { enabled.set(!enabled.peek()) } }
        }
        row {
            horizontal {
                vertical {
                    text("Доступный список")
                    select("Скрытый список без подписи", items = ObscuredSamples.OPTIONS, itemLabel = { item -> item }) {
                        value = ObscuredSamples.OPTIONS.first()
                        bindObscured(obscured)
                    }
                }
                vertical {
                    text("Скрытие перед выключением")
                    select("Сначала скрытый, затем выключенный", items = ObscuredSamples.OPTIONS, itemLabel = { item -> item }) {
                        value = ObscuredSamples.OPTIONS.first()
                        bindObscured(obscured)
                        bindEnabled(enabled)
                    }
                }
                vertical {
                    text("Выключение перед скрытием")
                    select("Сначала выключенный, затем скрытый", items = ObscuredSamples.OPTIONS, itemLabel = { item -> item }) {
                        value = ObscuredSamples.OPTIONS.first()
                        bindEnabled(enabled)
                        bindObscured(obscured)
                    }
                }
                text(enabled.map { value -> if (value) "Списки доступны" else "Списки выключены" })
            }
        }
    }
}
