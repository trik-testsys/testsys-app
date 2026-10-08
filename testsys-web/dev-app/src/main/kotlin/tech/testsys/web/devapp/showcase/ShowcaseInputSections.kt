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
import tech.testsys.web.components.forms.passwordInput
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
        block(size = 12, title = "Текстовые поля и выбор") {
            row { textInput("Название", labelSize = 8, size = 16, hint = "Видно участникам") }
            row {
                codeInput("Идентификатор", labelSize = 8, size = 8)
                codeInput("Занятый", labelSize = 4, size = 4) {
                    isInvalid = true
                    errorMessage = "Уже занят"
                }
            }
            row { passwordInput("Код-доступа", labelSize = 8, size = 16) }
            row { textArea("Описание", labelSize = 8, size = 16) }
            row {
                textArea("Условие", labelSize = 8, size = 16, hint = "Не выше четырёх строк, дальше прокрутка", maxLines = 4) {
                    value = LONG_TEXT
                }
            }
            row { textArea("Заметки", labelSize = 8, size = 16, hint = "Всегда три строки", minLines = 3, maxLines = 3) }
            row {
                select(
                    "Язык",
                    labelSize = 8,
                    size = 16,
                    items = listOf("Kotlin", "Python", "C++"),
                    itemLabel = { language -> language },
                )
            }
            row { checkbox("Открытый тур", labelSize = 8, size = 16) }
            row { textInput("Очень длинная подпись поля, которая переносится на несколько строк", labelSize = 8, size = 16) }
            row { textInput("Обязательное", labelSize = 8, size = 16) { isRequiredIndicatorVisible = true } }
            row {
                textInput("Выключенное", labelSize = 8, size = 16) {
                    value = "Недоступно"
                    isEnabled = false
                }
            }
            row {
                textInput("Только чтение", labelSize = 8, size = 16) {
                    value = "Можно выделить и скопировать"
                    isEditable = false
                }
            }
            row {
                field("Теги", labelSize = 8, size = 16) {
                    tag("графы")
                    tag("DP")
                    badge("Идёт", Tone.Success)
                }
            }
        }
        block(size = 12, title = "Даты и числа") {
            row { dateInput("Дата", labelSize = 8, size = 16) }
            row { timeInput("Время", labelSize = 8, size = 16, step = Duration.ofMinutes(TIME_STEP_MINUTES)) }
            row { dateTimeInput("Начало тура", labelSize = 8, size = 16) }
            row { dateRangeInput("Период регистрации", labelSize = 8, size = 16) }
            row { integerInput("Длительность", labelSize = 8, size = 16, min = 10, max = 300, step = 5, unit = "минут") }
            row { decimalInput("Балл", labelSize = 8, size = 16, min = 0.0, max = 100.0, step = 0.5) }
            row {
                integerInput("Задач", labelSize = 4, size = 8)
                integerInput("Попыток", labelSize = 4, size = 8)
            }
        }
    }
}

internal fun PageScope.editingSection() {
    val profile = ShowcaseProfile()
    val binder = Binder<ShowcaseProfile>()
    lateinit var switched: BlockHandle
    row {
        block(size = 12, title = "Режим редактирования", subtitle = "Сохранение с пустым логином оставляет режим") {
            editing(onSave = { binder.writeBeanIfValid(profile) }, onCancel = { binder.readBean(profile) })
            row {
                textInput("Логин", labelSize = 8, size = 16) {
                    binder.forField(this)
                        .asRequired("Заполните логин")
                        .bind({ source -> source.login }, { target, value -> target.login = value })
                }
            }
            row {
                textInput("Почта", labelSize = 8, size = 16) {
                    binder.forField(this).bind({ source -> source.email }, { target, value -> target.email = value })
                }
            }
            row {
                dateInput("Создан", labelSize = 8, size = 16) {
                    value = LocalDate.parse("2026-09-12")
                    isEditable = false
                }
            }
        }
        switched = block(size = 12, title = "Переключение из кода", subtitle = "BlockHandle.isEditable") {
            actions { action("Переключить") { onClick { switched.isEditable = !switched.isEditable } } }
            row { textInput("Название", labelSize = 8, size = 16) { value = "Весенний тур" } }
        }
    }

    binder.readBean(profile)
    switched.isEditable = false

    row {
        highlightBlock(title = "Подсветка в режиме редактирования", subtitle = "Поля только для чтения в тёмном блоке") {
            editing(onSave = { true }, onCancel = {})
            row { textInput("Название", labelSize = 4, size = 8) { value = "Весенний тур" } }
            row { dateInput("Дата", labelSize = 4, size = 8) { value = LocalDate.parse("2026-09-12") } }
        }
    }
}
