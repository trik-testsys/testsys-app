package tech.testsys.web.devapp.dev

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.overlay.TooltipPlacement
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.overlay.drawer
import tech.testsys.web.components.overlay.popover
import tech.testsys.web.components.overlay.tooltip

private const val LABEL_COLUMNS = 4
private const val VALUE_COLUMNS = 8

/**
 * Showcase of reusable drawers, nonmodal popups and noninteractive tooltips, available only in dev.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/overlays")
@PageTitle("Оверлеи")
class ShowcaseOverlaysView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Оверлеи")
            block(title = "Drawer") {
                val editor = drawer(title = "Параметры", subtitle = "Значения сохраняются между открытиями") {
                    row { textInput("Название", labelSize = LABEL_COLUMNS, size = VALUE_COLUMNS) { value = "Сохранённое значение" } }
                    row { vertical { popover("Вложенный попап") { text("Попап внутри Drawer") } } }
                    footer { handle ->
                        action("Готово") {
                            onClick {
                                toast(FeedbackKind.Success, "Значения Drawer сохранены")
                                handle.close()
                            }
                        }
                    }
                }
                row { horizontal { action("Открыть Drawer") { onClick { editor.open() } } } }
            }
            block(title = "Popover") {
                var clicks = 0
                lateinit var result: TextHandle
                row {
                    horizontal {
                        popover("Открыть Popover") { text("Закрывается по Escape и клику вне. Фон остаётся доступен.") }
                        action("Действие за попапом") {
                            onClick {
                                clicks++
                                result.text = "Действие выполнено: $clicks"
                            }
                        }
                    }
                }
                row { vertical { result = text("Действие ещё не выполнено") } }
            }
            block(title = "Tooltip") {
                row {
                    horizontal {
                        TooltipPlacement.entries.forEach { side ->
                            action("Подсказка $side").tooltip("Обычный текст: $side", side)
                        }
                    }
                }
                val nested = dialog("Диалог с попапом") {
                    row { vertical { popover("Попап в диалоге") { text("Вложенность и возврат фокуса") } } }
                    footer { handle -> action("Закрыть") { onClick { handle.close() } } }
                }
                row { horizontal { action("Проверить вложенность") { onClick { nested.open() } } } }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }
}
