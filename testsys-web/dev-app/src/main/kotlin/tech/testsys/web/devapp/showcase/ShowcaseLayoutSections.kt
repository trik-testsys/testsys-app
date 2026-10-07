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
    block(title = "Сетка", subtitle = "24 колонки: ряд страницы → слот → ряд слота → блок → строка блока") {
        row {
            for (column in 1..RULER_COLUMNS) text("$column", size = 1)
        }
    }
    row { slot(size = 24) { row { block(title = "Слот 24") { row { text("Слот на всю ширину") } } } } }
    row {
        slot(size = 12) { row { block(title = "Слот 12") { row { text("Половина") } } } }
        slot(size = 12) { row { block(title = "Слот 12") { row { text("Половина") } } } }
    }
    row {
        slot(size = 8) { row { block(title = "Слот 8") { row { text("Треть") } } } }
        slot(size = 8) { row { block(title = "Слот 8") { row { text("Треть") } } } }
        slot(size = 8) { row { block(title = "Слот 8") { row { text("Треть") } } } }
    }
    row {
        slot(size = 6) {
            row {
                highlightBlock(title = "Слот 6", subtitle = "Подсветка на весь слот") {
                    row { text("Одна на ряд страницы") }
                }
            }
        }
        slot(size = 18) {
            row {
                block(size = 12, title = "Блок 12") { row { text("Ряд слота 18: 12 + 6") } }
                block(size = 6, title = "Блок 6") { row { text("Остаток слота") } }
            }
            row {
                block(size = 6, title = "Блок 6") { row { text("Второй ряд слота") } }
                block(size = 6, title = "Блок 6") { row { text("Три блока") } }
                block(size = 6, title = "Блок 6") { row { text("По 6 колонок") } }
            }
        }
    }
    row {
        slot(size = 16) { row { block(title = "Слот 16") { row { text("Две трети") } } } }
        slot(size = 8) {
            row {
                highlightBlock(size = 4, title = "Блок 4") { row { text("Частичная подсветка") } }
                block(size = 4, title = "Блок 4") { row { text("Рядом") } }
            }
        }
    }
}

internal fun PageScope.blockSection() {
    row {
        slot(size = 8) {
            row { block(title = "Заголовок", subtitle = "И подзаголовок") { row { text("Шапка в две строки") } } }
        }
        slot(size = 8) { row { block(title = "Только заголовок") { row { text("Шапка в одну строку") } } } }
        slot(size = 8) { row { block { row { text("Блок без шапки: нет заголовка и действий") } } } }
    }
    row {
        slot(size = 12) {
            row {
                block(title = "Шапка с действиями и подвал") {
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
            }
        }
        slot(size = 12) {
            row {
                block(title = "Строки блока", subtitle = "Элемент без size занимает остаток строки") {
                    row {
                        text("size = 6", size = 6)
                        text("Без size — остаток, 6 колонок")
                    }
                    row {
                        tag("size = 4", size = 4)
                        badge("size = 6", Tone.Info, size = 6)
                        counter(value = 7, size = 2)
                    }
                }
            }
        }
    }
}
