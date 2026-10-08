package tech.testsys.web.devapp.showcase

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.iconAction
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.icon
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.counter
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.layout.PageScope

private const val RULER_COLUMNS = 24

internal fun PageScope.gridSection() {
    row {
        block(title = "Сетка", subtitle = "Своя сетка из 24 колонок у ряда страницы и блока") {
            row {
                for (column in 1..RULER_COLUMNS) text("$column", size = 1)
            }
        }
    }

    row { block(size = 24, title = "Блок 24") { row { text("Блок на всю ширину") } } }

    row {
        block(size = 12, title = "Блок 12") { row { text("Половина") } }
        block(size = 12, title = "Блок 12") { row { text("Половина") } }
    }

    row {
        block(size = 8, title = "Блок 8") { row { text("Треть") } }
        block(size = 8, title = "Блок 8") { row { text("Треть") } }
        block(size = 8, title = "Блок 8") { row { text("Треть") } }
    }

    row {
        space(size = 7)
        block(size = 10, title = "Блок 10") { row { text("По центру между пустыми колонками") } }
        space(size = 7)
    }

    row {
        highlightBlock(size = 6, title = "Блок 6", subtitle = "Подсветка") {
            row { text("Одна на ряд страницы") }
        }
        block(size = 12, title = "Блок 12") { row { text("Рядом") } }
        block(title = "Без size") { row { text("Остаток ряда: 6 колонок") } }
    }

    row {
        block(size = 16, title = "Блок 16") { row { text("Две трети") } }
        block(size = 8, title = "Блок 8") { row { text("Треть") } }
    }
}

internal fun PageScope.blockSection() {
    row {
        block(size = 8, title = "Заголовок", subtitle = "И подзаголовок") { row { text("Шапка в две строки") } }
        block(size = 8, title = "Только заголовок") { row { text("Шапка в одну строку") } }
        block(size = 8) { row { text("Блок без шапки: нет заголовка и действий") } }
    }
    row {
        block(size = 12, title = "Шапка с действиями и подвал") {
            actions {
                badge("Идёт", Tone.Success)
                action("Отфильтровать", icon = IconName.ListFilter) { onClick { toast(FeedbackKind.Info, "Фильтр применён") } }
                iconAction(IconName.Settings, "Открыть настройки") { onClick { toast(FeedbackKind.Info, "Настройки открыты") } }
            }
            row { text("Кнопки шапки — маленькие, в теле и подвале — обычные") }
            footer {
                linkAction("Показать подробности") { onClick { toast(FeedbackKind.Info, "Подробности показаны") } }
                mainAction("Сохранить") { onClick { toast(FeedbackKind.Info, "Сохранить: действие выполнено") } }
            }
        }
        block(size = 12, title = "Строки блока", subtitle = "Элемент без size занимает остаток строки") {
            row {
                text("size = 12", size = 12)
                text("Без size — остаток, 12 колонок")
            }
            row {
                tag("size = 8", size = 8)
                badge("size = 12", Tone.Info, size = 12)
                counter(value = 7, size = 4)
            }
        }
    }
}
