package tech.testsys.web.devapp.showcase

import com.vaadin.flow.data.binder.Binder
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.destructiveAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.DialogHandle
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog

private const val DESCRIPTION_LINES = 4

private const val DANGER_TOUR_NAME = "Весенний кубок"

/** Tour created by the showcase form dialog. */
private class ShowcaseTour(var name: String = "", var period: DateRange = DateRange(), var description: String = "")

/** Confirmations of every kind and a form dialog that stays open until its fields are valid. */
internal fun PageScope.dialogSection() {
    val newTour = tourDialog()
    block(title = "Диалоги", subtitle = "«Создать» с пустым названием диалог не закрывает") {
        row {
            horizontal {
                action("Подтвердить") {
                    onClick {
                        confirm(title = "Опубликовать тур?", text = "Участники увидят тур в своих кабинетах.", action = "Опубликовать") {
                            toast(FeedbackKind.Success, "Тур опубликован")
                        }
                    }
                }
                destructiveAction("Удалить тур") {
                    onClick {
                        confirm(title = "Удалить тур?", text = "Это действие нельзя отменить.", action = "Удалить", isDanger = true) {
                            toast(FeedbackKind.Success, "Тур удалён")
                        }
                    }
                }
                destructiveAction("Удалить с вводом названия") {
                    onClick {
                        confirm(
                            title = "Удалить тур «$DANGER_TOUR_NAME»?",
                            text = "Вместе с туром удалятся все посылки.",
                            action = "Удалить",
                            isDanger = true,
                            typeToConfirm = DANGER_TOUR_NAME,
                        ) { toast(FeedbackKind.Success, "Тур удалён") }
                    }
                }
                mainAction("Создать тур") { onClick { newTour.open() } }
            }
        }
    }
}

private fun tourDialog(): DialogHandle {
    val binder = Binder<ShowcaseTour>()
    return dialog(title = "Новый тур", subtitle = "Название обязательно") {
        row {
            textInput("Название", labelSize = 8, size = 16) {
                binder.forField(this)
                    .asRequired("Заполните название")
                    .bind({ source -> source.name }, { target, value -> target.name = value })
            }
        }
        row {
            dateRangeInput("Период", labelSize = 8, size = 16) {
                binder.forField(this).bind({ source -> source.period }, { target, value -> target.period = value })
            }
        }
        row {
            textArea("Описание", labelSize = 8, size = 16, maxLines = DESCRIPTION_LINES) {
                binder.forField(this).bind({ source -> source.description }, { target, value -> target.description = value })
            }
        }
        footer { handle ->
            action("Отменить") { onClick { handle.close() } }
            mainAction("Создать") {
                onClick {
                    val tour = ShowcaseTour()
                    if (binder.writeBeanIfValid(tour)) {
                        toast(kind = FeedbackKind.Success, title = "Тур создан", description = tour.name)
                        handle.close()
                    }
                }
            }
        }
    }
}
