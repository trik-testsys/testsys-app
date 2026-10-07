package tech.testsys.web.devapp.showcase

import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.destructiveAction
import tech.testsys.web.components.actions.destructiveIconAction
import tech.testsys.web.components.actions.iconAction
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.actions.mainIconAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.icon
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockHandle
import tech.testsys.web.components.layout.PageScope

internal fun PageScope.actionSection() {
    block(title = "Кнопки", subtitle = "Роли, размеры и состояния") {
        actions {
            mainAction("Выполнить главное действие") { onClick { toast(FeedbackKind.Info, "Главное действие выполнено") } }
            action("Выполнить обычное действие", icon = IconName.ListFilter) {
                onClick { toast(FeedbackKind.Info, "Обычное действие выполнено") }
            }
            destructiveAction("Удалить") { onClick { toast(FeedbackKind.Info, "Удалить: действие выполнено") } }
            linkAction("Открыть ссылку") { onClick { toast(FeedbackKind.Info, "Ссылка открыта") } }
            iconAction(IconName.Settings, "Открыть настройки") { onClick { toast(FeedbackKind.Info, "Настройки открыты") } }
        }
        row {
            horizontal {
                mainAction("Выполнить главное действие", icon = IconName.Check) {
                    onClick { toast(FeedbackKind.Info, "Главное действие выполнено") }
                }
                action("Выполнить обычное действие") { onClick { toast(FeedbackKind.Info, "Обычное действие выполнено") } }
                destructiveAction("Удалить", icon = IconName.Trash) { onClick { toast(FeedbackKind.Info, "Удалить: действие выполнено") } }
                linkAction("Показать подробности") { onClick { toast(FeedbackKind.Info, "Подробности показаны") } }
                iconAction(IconName.Settings, "Открыть настройки") { onClick { toast(FeedbackKind.Info, "Настройки открыты") } }
                mainIconAction(IconName.Plus, "Добавить объект") { onClick { toast(FeedbackKind.Success, "Объект добавлен") } }
                destructiveIconAction(IconName.Trash, "Удалить объект") { onClick { toast(FeedbackKind.Info, "Объект удалён") } }
            }
        }
        row {
            horizontal {
                mainAction("Выполнить главное действие") { isEnabled = false }
                action("Выполнить обычное действие") { isEnabled = false }
                destructiveAction("Удалить", icon = IconName.Trash) { isEnabled = false }
                linkAction("Открыть ссылку") { isEnabled = false }
                iconAction(IconName.Settings, "Открыть настройки") { isEnabled = false }
            }
        }
        row {
            horizontal {
                val loading = mainAction("Отправить", icon = IconName.Upload) { isLoading = true }
                action("Переключить загрузку") { onClick { loading.isLoading = !loading.isLoading } }
            }
        }
        footer {
            action("Отменить") { onClick { toast(FeedbackKind.Info, "Отменить: действие выполнено") } }
            mainAction("Выполнить действие подвала") { onClick { toast(FeedbackKind.Info, "Действие подвала выполнено") } }
        }
    }
}

internal fun PageScope.propertySection() {
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
                            action("Переключить видимость поля") { onClick { targetField.isVisible = !targetField.isVisible } }
                            action("Переключить доступность поля") { onClick { targetField.isEnabled = !targetField.isEnabled } }
                            action("Переключить редактирование поля") { onClick { targetField.isEditable = !targetField.isEditable } }
                            action("Переключить видимость текста") { onClick { targetText.isVisible = !targetText.isVisible } }
                            action("Переключить доступность кнопки") { onClick { targetAction.isEnabled = !targetAction.isEnabled } }
                            action("Переключить загрузку кнопки") { onClick { targetAction.isLoading = !targetAction.isLoading } }
                            action("Переключить видимость блока") { onClick { targetBlock.isVisible = !targetBlock.isVisible } }
                            action("Переключить редактирование блока") { onClick { targetBlock.isEditable = !targetBlock.isEditable } }
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
                            targetAction = mainAction("Нажать цель") { onClick { toast(FeedbackKind.Info, "Нажата цель") } }
                        }
                    }
                }
            }
        }
    }
}
