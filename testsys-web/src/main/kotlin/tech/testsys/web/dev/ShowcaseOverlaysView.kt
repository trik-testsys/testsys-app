package tech.testsys.web.dev

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.ui.TestSysView
import tech.testsys.web.ui.TextHandle
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.feedback.FeedbackKind
import tech.testsys.web.ui.feedback.toast
import tech.testsys.web.ui.forms.textInput
import tech.testsys.web.ui.overlay.TooltipPlacement
import tech.testsys.web.ui.overlay.dialog
import tech.testsys.web.ui.overlay.drawer
import tech.testsys.web.ui.overlay.popover
import tech.testsys.web.ui.overlay.tooltip

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
