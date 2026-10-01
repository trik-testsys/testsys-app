package tech.testsys.web.dev

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.ui.TestSysView
import tech.testsys.web.ui.TextHandle
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.actions.ActionHandle
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.actions.destructiveAction
import tech.testsys.web.ui.actions.iconAction
import tech.testsys.web.ui.actions.linkAction
import tech.testsys.web.ui.actions.mainAction
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.core.icon
import tech.testsys.web.ui.display.CounterKind
import tech.testsys.web.ui.display.TagKind
import tech.testsys.web.ui.display.Tone
import tech.testsys.web.ui.display.Trend
import tech.testsys.web.ui.display.badge
import tech.testsys.web.ui.display.counter
import tech.testsys.web.ui.display.field
import tech.testsys.web.ui.display.statCard
import tech.testsys.web.ui.display.tag
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.display.verdict
import tech.testsys.web.ui.feedback.FeedbackKind
import tech.testsys.web.ui.feedback.alert
import tech.testsys.web.ui.feedback.toast
import tech.testsys.web.ui.forms.ValueInput
import tech.testsys.web.ui.forms.checkbox
import tech.testsys.web.ui.forms.codeInput
import tech.testsys.web.ui.forms.dateInput
import tech.testsys.web.ui.forms.dateRangeInput
import tech.testsys.web.ui.forms.dateTimeInput
import tech.testsys.web.ui.forms.decimalInput
import tech.testsys.web.ui.forms.integerInput
import tech.testsys.web.ui.forms.select
import tech.testsys.web.ui.forms.textArea
import tech.testsys.web.ui.forms.textInput
import tech.testsys.web.ui.forms.timeInput
import tech.testsys.web.ui.layout.BlockHandle
import tech.testsys.web.ui.layout.PageScope
import java.time.Duration
import java.time.LocalDate

private const val TIME_STEP_MINUTES = 30L
private const val RULER_COLUMNS = 24
private const val DEMO_VERDICT_SCORE = 87.0
private const val ICONS_PER_ROW = 4
private const val LONG_TEXT = "Дан ориентированный граф из n вершин и m рёбер. " +
    "Найдите кратчайший путь от вершины 1 до вершины n. " +
    "Если пути нет, выведите −1. " +
    "Веса рёбер — целые числа от 1 до 10⁹. " +
    "Граф может содержать кратные рёбра и петли. " +
    "Ограничение времени — одна секунда, памяти — 256 МБ."

/** Profile edited by the editing section. */
private class ShowcaseProfile(var login: String = "anna", var email: String = "anna@example.org")

/**
 * Showcase of every layout and component option of the design system DSL for visual checks;
 * it opens only in the `dev` profile.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase")
@PageTitle("Витрина дизайн-системы")
class ShowcaseView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Компоненты")
            gridSection()
            blockSection()
            fieldSection()
            editingSection()
            actionSection()
            propertySection()
            displaySection()
            iconGallery()
            feedbackSection()
            tableSection()
            dialogSection()
            lookupSection()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }
}

private fun PageScope.gridSection() {
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

private fun PageScope.blockSection() {
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
                        action("Фильтр", icon = IconName.ListFilter) { onClick { toast(FeedbackKind.Info, "Фильтр: действие выполнено") } }
                        iconAction(IconName.Settings, "Настройки") { onClick { toast(FeedbackKind.Info, "Настройки: действие выполнено") } }
                    }
                    row { text("Кнопки шапки — маленькие, в теле и подвале — обычные") }
                    footer {
                        linkAction("Подробнее") { onClick { toast(FeedbackKind.Info, "Подробнее: действие выполнено") } }
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

private fun PageScope.fieldSection() {
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

private fun PageScope.editingSection() {
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

private fun PageScope.actionSection() {
    block(title = "Кнопки", subtitle = "Роли, размеры и состояния") {
        actions {
            mainAction("Главная") { onClick { toast(FeedbackKind.Info, "Главная: действие выполнено") } }
            action("Обычная", icon = IconName.ListFilter) { onClick { toast(FeedbackKind.Info, "Обычная: действие выполнено") } }
            destructiveAction("Удалить") { onClick { toast(FeedbackKind.Info, "Удалить: действие выполнено") } }
            linkAction("Ссылка") { onClick { toast(FeedbackKind.Info, "Ссылка: действие выполнено") } }
            iconAction(IconName.Settings, "Настройки") { onClick { toast(FeedbackKind.Info, "Настройки: действие выполнено") } }
        }
        row {
            horizontal {
                mainAction("Главная", icon = IconName.Check) { onClick { toast(FeedbackKind.Info, "Главная: действие выполнено") } }
                action("Обычная") { onClick { toast(FeedbackKind.Info, "Обычная: действие выполнено") } }
                destructiveAction("Удалить", icon = IconName.Trash) { onClick { toast(FeedbackKind.Info, "Удалить: действие выполнено") } }
                linkAction("Подробнее") { onClick { toast(FeedbackKind.Info, "Подробнее: действие выполнено") } }
                iconAction(IconName.Settings, "Настройки") { onClick { toast(FeedbackKind.Info, "Настройки: действие выполнено") } }
            }
        }
        row {
            horizontal {
                mainAction("Главная") { isEnabled = false }
                action("Обычная") { isEnabled = false }
                destructiveAction("Удалить", icon = IconName.Trash) { isEnabled = false }
                linkAction("Ссылка") { isEnabled = false }
                iconAction(IconName.Settings, "Настройки") { isEnabled = false }
            }
        }
        row {
            horizontal {
                val loading = mainAction("Отправить", icon = IconName.Upload) { isLoading = true }
                action("Переключить загрузку") { onClick { loading.isLoading = !loading.isLoading } }
            }
        }
        footer {
            action("Отмена") { onClick { toast(FeedbackKind.Info, "Отмена: действие выполнено") } }
            mainAction("В подвале") { onClick { toast(FeedbackKind.Info, "В подвале: действие выполнено") } }
        }
    }
}

private fun PageScope.propertySection() {
    lateinit var targetField: ValueInput<String>
    lateinit var targetText: TextHandle
    lateinit var targetAction: ActionHandle
    lateinit var targetBlock: BlockHandle
    row {
        slot(size = 12) {
            row {
                block(title = "Служебные свойства", subtitle = "Переключатели меняют цели справа") {
                    row {
                        vertical {
                            action("Поле: видимость") { onClick { targetField.isVisible = !targetField.isVisible } }
                            action("Поле: включено") { onClick { targetField.isEnabled = !targetField.isEnabled } }
                            action("Поле: редактируемо") { onClick { targetField.isEditable = !targetField.isEditable } }
                            action("Текст: видимость") { onClick { targetText.isVisible = !targetText.isVisible } }
                            action("Кнопка: включена") { onClick { targetAction.isEnabled = !targetAction.isEnabled } }
                            action("Кнопка: загрузка") { onClick { targetAction.isLoading = !targetAction.isLoading } }
                            action("Блок: видимость") { onClick { targetBlock.isVisible = !targetBlock.isVisible } }
                            action("Блок: редактируемость") { onClick { targetBlock.isEditable = !targetBlock.isEditable } }
                        }
                    }
                }
            }
        }
        slot(size = 12) {
            row {
                targetBlock = block(title = "Цели") {
                    row {
                        targetField = textInput("Поле", labelSize = 4, size = 4) { value = "Скрываемое поле" }
                        text("Сдвинется влево", size = 4)
                    }
                    row { targetText = text("Скрываемый текст") }
                    row {
                        horizontal {
                            targetAction = mainAction("Цель") { onClick { toast(FeedbackKind.Info, "Нажата цель") } }
                        }
                    }
                }
            }
        }
    }
}

private fun PageScope.displaySection() {
    row {
        slot(size = 12) {
            row {
                block(title = "Отображение") {
                    row { text("Обычный текст абзаца на всю ширину блока") }
                    row {
                        horizontal {
                            verdict(0.0)
                            verdict(DEMO_VERDICT_SCORE, label = "баллов")
                        }
                    }
                    row {
                        horizontal {
                            icon(IconName.Trophy)
                            text("Иконка рядом с текстом")
                        }
                    }
                    row { horizontal { TagKind.entries.forEach { kind -> tag(kind.name, kind) } } }
                    row { horizontal { Tone.entries.forEach { tone -> badge(tone.name, tone) } } }
                    row { horizontal { CounterKind.entries.forEach { kind -> counter(value = 12, kind = kind) } } }
                }
            }
        }
        slot(size = 12) {
            row {
                Trend.entries.forEach { trend ->
                    statCard(label = "Тренд ${trend.name}", value = "1 842", size = 6, delta = "+38", trend = trend)
                }
            }
            row { statCard(label = "Без изменения", value = "42") }
            row {
                block(title = "Метрики в строке блока") {
                    row {
                        statCard(label = "Решено", value = "42", size = 6, delta = "+3", trend = Trend.Up)
                        statCard(label = "Попытки", value = "118", size = 6, delta = "−2", trend = Trend.Down)
                    }
                }
            }
        }
    }
}

private fun PageScope.feedbackSection() {
    block(title = "Обратная связь") {
        FeedbackKind.entries.forEach { kind -> row { alert(kind, "Алерт ${kind.name}", text = "Описание под заголовком") } }
        row {
            horizontal {
                FeedbackKind.entries.forEach { kind ->
                    action("Тост ${kind.name}") {
                        onClick { toast(kind = kind, title = "Тост ${kind.name}", description = "Описание тоста") }
                    }
                }
            }
        }
    }
}

private fun PageScope.iconGallery() {
    block(title = "Icon: поддерживаемые имена") {
        IconName.entries.chunked(ICONS_PER_ROW).forEach { names ->
            row {
                names.forEach { name ->
                    horizontal(size = 6) {
                        icon(name)
                        text(name.name)
                    }
                }
            }
        }
    }
}
