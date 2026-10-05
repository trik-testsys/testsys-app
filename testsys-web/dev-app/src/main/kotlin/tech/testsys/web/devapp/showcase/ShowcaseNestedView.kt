package tech.testsys.web.devapp.showcase

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.signals.local.ValueSignal
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.lookup
import tech.testsys.web.components.forms.skipWhenHidden
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts

/**
 * Nested showcase page for keyboard focus, compound fields and explicit Binder saving, available only in `dev`.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/states/accessibility")
@PageTitle("Доступность компонентов")
class ShowcaseNestedView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Доступность компонентов")
            focusExamples()
            hiddenFieldExample()
            dialogSignalExample()
            block(title = "Страница 404") {
                actions { action("Открыть отсутствующую страницу") { onClick { UI.getCurrent().navigate("dev/missing") } } }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }
}

/** Draft saved explicitly by the hidden-field demonstration. */
private class HiddenFieldDraft(var name: String = "Сохранённое название")

/** Draft used to validate the compound range demonstration. */
private class RangeDraft(var period: DateRange = DateRange())

private fun PageScope.focusExamples() {
    block(title = "Фокус: обычное поле") {
        editing(onSave = { true }, onCancel = {})
        row { textInput("Скрытое", labelSize = 4, size = 20) { isVisible = false } }
        row { textInput("Выключенное", labelSize = 4, size = 20) { isEnabled = false } }
        row { textInput("Только чтение", labelSize = 4, size = 20) { isEditable = false } }
        row { textInput("Первое доступное", labelSize = 4, size = 20) }
    }
    block(title = "Фокус: диапазон дат") {
        val binder = Binder<RangeDraft>()
        val required = ValueSignal(true)
        editing(onSave = { binder.validate().isOk }, onCancel = {})
        actions { action("Переключить обязательность") { onClick { required.set(!required.peek()) } } }
        row {
            dateRangeInput("Период", labelSize = 4, size = 20) {
                binder.forField(this).withValidator({ range -> !required.peek() || range != DateRange() }, "Укажите границу периода")
                    .bind({ draft -> draft.period }, { draft, value -> draft.period = value })
                bindRequiredIndicatorVisible(required)
            }
        }
    }
    block(title = "Фокус: дата и время") {
        editing(onSave = { true }, onCancel = {})
        row { dateTimeInput("Начало", labelSize = 4, size = 20) }
    }
    block(title = "Фокус: лукап") {
        editing(onSave = { true }, onCancel = {})
        row {
            lookup(
                "Тур",
                labelSize = 4,
                size = 20,
                fetch = { _, _ -> Page(listOf("Весенний кубок"), 1) },
                display = { value -> value },
                columns = { textColumn("Название") { value -> value } },
            )
        }
    }
    block(title = "Фокус: нет доступных полей") {
        editing(onSave = { true }, onCancel = {})
        row { textInput("Архивный код", labelSize = 4, size = 20) { isEditable = false } }
    }
    block(title = "Фокус: пустой блок") {
        editing(onSave = { true }, onCancel = {})
    }
    block(title = "Фокус: пустая загрузка") {
        var isFieldShown = false
        editing(onSave = { true }, onCancel = {})
        val loaded = load({ isFieldShown }) { show ->
            if (show) row { textInput("Появившееся поле", labelSize = 4, size = 20) }
        }
        actions {
            action("Заполнить пустое тело") {
                onClick {
                    isFieldShown = true
                    loaded.reload()
                }
            }
        }
    }
    block(title = "Фокус: загруженное тело") {
        var revision = 0
        editing(onSave = { true }, onCancel = {})
        val loaded = load({ "Загрузка ${++revision}" }) { label ->
            row { textInput(label, labelSize = 4, size = 20) }
        }
        actions { action("Заменить тело") { onClick { loaded.reload() } } }
    }
}

private fun PageScope.hiddenFieldExample() {
    val draft = HiddenFieldDraft()
    val binder = Binder<HiddenFieldDraft>()
    lateinit var input: ValueInput<String>
    lateinit var saved: TextHandle
    block(title = "Скрытое поле Binder", subtitle = "Скрытие сохраняет прежнее значение при явном сохранении") {
        row {
            input = textInput("Название", labelSize = 4, size = 20) {
                binder.forField(this).asRequired("Укажите название")
                    .bind({ bean -> bean.name }, { bean, value -> bean.name = value }).skipWhenHidden()
            }
        }
        row { saved = text("Сохранено: ${draft.name}") }
        actions {
            action("Показать / скрыть поле") { onClick { input.isVisible = !input.isVisible } }
            action("Сохранить явно") { onClick { if (binder.writeBeanIfValid(draft)) saved.text = "Сохранено: ${draft.name}" } }
        }
    }
    binder.readBean(draft)
}

private fun PageScope.dialogSignalExample() {
    val editable = ValueSignal(false)
    val form = dialog("Режим диалога по сигналу") {
        row { textInput("Название диалога", labelSize = 4, size = 8) }
        row { textInput("Неизменяемый код", labelSize = 4, size = 8) { isEditable = false } }
        footer { handle ->
            action("Переключить режим") { onClick { editable.set(!editable.peek()) } }
            action("Закрыть") { onClick { handle.close() } }
        }
    }
    form.bindEditable(editable)
    block(title = "Диалог по сигналу") {
        actions { action("Открыть форму") { onClick { form.open() } } }
    }
}
