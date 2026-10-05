package tech.testsys.web.devapp.showcase

import com.vaadin.flow.data.binder.Binder
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.forms.checkbox
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateInput
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.decimalInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.forms.timeInput
import tech.testsys.web.components.layout.BlockHandle
import tech.testsys.web.components.layout.PageScope
import java.time.Duration
import java.time.LocalDate

private const val TIME_STEP_MINUTES = 30L

/** Profile edited by the editing section. */
private class ShowcaseProfile(var login: String = "anna", var email: String = "anna@example.org")

internal fun PageScope.fieldSection() {
    row {
        slot(size = 12) {
            row {
                block(title = "Текстовые поля и выбор") {
                    row { textInput("Название", labelSize = 4, size = 8, hint = "Видно участникам") }
                    row {
                        codeInput("Идентификатор", labelSize = 4, size = 4)
                        codeInput("Занятый", labelSize = 2, size = 2) {
                            isInvalid = true
                            errorMessage = "Уже занят"
                        }
                    }
                    row { textArea("Описание", labelSize = 4, size = 8) }
                    row {
                        textArea("Условие", labelSize = 4, size = 8, hint = "Не выше четырёх строк, дальше прокрутка", maxLines = 4) {
                            value = LONG_TEXT
                        }
                    }
                    row { textArea("Заметки", labelSize = 4, size = 8, hint = "Всегда три строки", minLines = 3, maxLines = 3) }
                    row {
                        select(
                            "Язык",
                            labelSize = 4,
                            size = 8,
                            items = listOf("Kotlin", "Python", "C++"),
                            itemLabel = { language -> language },
                        )
                    }
                    row { checkbox("Открытый тур", labelSize = 4, size = 8) }
                    row { textInput("Очень длинная подпись поля, которая переносится на несколько строк", labelSize = 4, size = 8) }
                    row { textInput("Обязательное", labelSize = 4, size = 8) { isRequiredIndicatorVisible = true } }
                    row {
                        textInput("Выключенное", labelSize = 4, size = 8) {
                            value = "Недоступно"
                            isEnabled = false
                        }
                    }
                    row {
                        textInput("Только чтение", labelSize = 4, size = 8) {
                            value = "Можно выделить и скопировать"
                            isEditable = false
                        }
                    }
                    row {
                        field("Теги", labelSize = 4, size = 8) {
                            tag("графы")
                            tag("DP")
                            badge("Идёт", Tone.Success)
                        }
                    }
                }
            }
        }
        slot(size = 12) {
            row {
                block(title = "Даты и числа") {
                    row { dateInput("Дата", labelSize = 4, size = 8) }
                    row { timeInput("Время", labelSize = 4, size = 8, step = Duration.ofMinutes(TIME_STEP_MINUTES)) }
                    row { dateTimeInput("Начало тура", labelSize = 4, size = 8) }
                    row { dateRangeInput("Период регистрации", labelSize = 4, size = 8) }
                    row { integerInput("Длительность", labelSize = 4, size = 8, min = 10, max = 300, step = 5, unit = "минут") }
                    row { decimalInput("Балл", labelSize = 4, size = 8, min = 0.0, max = 100.0, step = 0.5) }
                    row {
                        integerInput("Задач", labelSize = 2, size = 4)
                        integerInput("Попыток", labelSize = 2, size = 4)
                    }
                }
            }
        }
    }
}

internal fun PageScope.editingSection() {
    val profile = ShowcaseProfile()
    val binder = Binder<ShowcaseProfile>()
    lateinit var switched: BlockHandle
    row {
        slot(size = 12) {
            row {
                block(title = "Режим редактирования", subtitle = "Сохранение с пустым логином оставляет режим") {
                    editing(onSave = { binder.writeBeanIfValid(profile) }, onCancel = { binder.readBean(profile) })
                    row {
                        textInput("Логин", labelSize = 4, size = 8) {
                            binder.forField(this)
                                .asRequired("Заполните логин")
                                .bind({ source -> source.login }, { target, value -> target.login = value })
                        }
                    }
                    row {
                        textInput("Почта", labelSize = 4, size = 8) {
                            binder.forField(this).bind({ source -> source.email }, { target, value -> target.email = value })
                        }
                    }
                    row {
                        dateInput("Создан", labelSize = 4, size = 8) {
                            value = LocalDate.parse("2026-09-12")
                            isEditable = false
                        }
                    }
                }
            }
        }
        slot(size = 12) {
            row {
                switched = block(title = "Переключение из кода", subtitle = "BlockHandle.isEditable") {
                    actions { action("Переключить") { onClick { switched.isEditable = !switched.isEditable } } }
                    row { textInput("Название", labelSize = 4, size = 8) { value = "Весенний тур" } }
                }
            }
        }
    }
    binder.readBean(profile)
    switched.isEditable = false
    highlightBlock(title = "Подсветка в режиме редактирования", subtitle = "Поля только для чтения в тёмном блоке") {
        editing(onSave = { true }, onCancel = {})
        row { textInput("Название", labelSize = 4, size = 8) { value = "Весенний тур" } }
        row { dateInput("Дата", labelSize = 4, size = 8) { value = LocalDate.parse("2026-09-12") } }
    }
}
