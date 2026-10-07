package tech.testsys.web.devapp.showcase

import com.vaadin.flow.component.UI
import com.vaadin.flow.function.SerializableRunnable
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.signals.local.ValueSignal
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.navigation.header.HeaderDestination
import tech.testsys.web.components.navigation.header.HeaderMegaColumn
import tech.testsys.web.components.navigation.header.HeaderMegaLink
import tech.testsys.web.components.navigation.header.HeaderMegaMenu
import tech.testsys.web.components.navigation.header.HeaderNotification
import tech.testsys.web.components.navigation.header.HeaderNotifications
import tech.testsys.web.components.navigation.header.HeaderUser
import tech.testsys.web.components.navigation.header.HeaderUserMenu
import tech.testsys.web.components.navigation.header.HeaderUserMenuItem
import tech.testsys.web.components.navigation.header.MegaMenuItem
import tech.testsys.web.components.navigation.header.NavItem
import tech.testsys.web.components.texts.UiTexts
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val NOTIFICATION_DELAY_SECONDS = 2L

/**
 * Functional showcase of the Cabinet header and its application-owned data, available only in dev.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/header")
@PageTitle("Шапка Кабинета")
class ShowcaseHeaderView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    private val status = ValueSignal("Действий пока нет")
    private val nextNotification = AtomicInteger()
    private val notifications = ValueSignal(initialNotifications())

    init {
        page(header()) {
            showcaseHead("Шапка Кабинета")
            row { block(title = "Результат действий") { row { text(status) } } }
            notificationControls()
            row {
                block(title = "Клавиатура и навигация") {
                    row {
                        text("Cmd/Ctrl+K — поиск по разделам; стрелки, Home, End и Enter — пункт меню; Escape — закрытие.")
                    }
                    row {
                        text("Разделы открываются наведением и кликом. Профиль, настройки, выход и действия меню меняют результат выше.")
                    }
                }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }

    private fun header(): CabinetHeader = CabinetHeader(
        items = listOf(
            NavItem(key = "showcase", label = "Витрина", target = ShowcaseView::class.java),
            MegaMenuItem(key = "sections", label = "Разделы", menu = sections()),
        ),
        active = "sections",
        user = HeaderUser(
            name = "Анна Петрова",
            menu = HeaderUserMenu(
                listOf(
                    HeaderUserMenuItem("Профиль", actionDestination("Открыт профиль")),
                    HeaderUserMenuItem("Настройки", actionDestination("Открыты настройки")),
                    HeaderUserMenuItem("Скоро появится", actionDestination("Недоступный пункт"), isEnabled = false),
                    HeaderUserMenuItem("Выйти", actionDestination("Выход: демонстрационный обработчик выполнен"), isDestructive = true),
                ),
            ),
        ),
        menuSearchKey = "sections",
        notifications = HeaderNotifications(
            items = notifications,
            onRead = { key ->
                notifications.set(notifications.peek().map { item -> if (item.key == key) item.copy(isUnread = false) else item })
                status.set("Прочитано уведомление: $key")
            },
            onReadAll = {
                notifications.set(notifications.peek().map { item -> item.copy(isUnread = false) })
                status.set("Все уведомления прочитаны")
            },
        ),
    )

    private fun sections(): HeaderMegaMenu = HeaderMegaMenu(
        columns = listOf(
            HeaderMegaColumn(
                title = "Компоненты",
                links = listOf(
                    HeaderMegaLink(
                        label = "Поля и файлы",
                        destination = HeaderDestination.Route(ShowcaseFormsView::class.java),
                        description = "Поля, составные даты и передача файлов",
                    ),
                    HeaderMegaLink(
                        label = "Оверлеи",
                        destination = HeaderDestination.Route(ShowcaseOverlaysView::class.java),
                        description = "Диалоги, Drawer и Popover",
                    ),
                ),
            ),
            HeaderMegaColumn(
                title = "Действия",
                links = listOf(
                    HeaderMegaLink(
                        label = "Создать соревнование",
                        destination = actionDestination("Создание соревнования: выполнено"),
                        description = "Демонстрационный обработчик",
                    ),
                    HeaderMegaLink(
                        label = "Очень длинное название раздела с пояснением",
                        destination = actionDestination("Открыт длинный раздел"),
                        description = "Текст переносится внутри границ меню",
                    ),
                ),
            ),
        ),
    )

    private fun PageScope.notificationControls() {
        row {
            block(title = "Живые уведомления") {
                row {
                    horizontal {
                        action("Добавить уведомление") { onClick { addNotification() } }
                        action("Добавить 3 уведомления") { onClick { addNotification(count = 3) } }
                        action("Добавить через 2 секунды") { onClick { addNotificationLater() } }
                        action("Удалить первое") {
                            onClick {
                                notifications.set(notifications.peek().drop(1))
                                status.set("Первое уведомление удалено")
                            }
                        }
                        action("Очистить уведомления") {
                            onClick {
                                notifications.set(emptyList())
                                status.set("Уведомления очищены")
                            }
                        }
                        action("Показать обычный тост результата") {
                            onClick {
                                toast(
                                    FeedbackKind.Success,
                                    title = "Действие выполнено",
                                    description = "Обычный тост остаётся справа снизу",
                                )
                            }
                        }
                        action("Восстановить уведомления") {
                            onClick {
                                notifications.set(initialNotifications())
                                status.set("Уведомления восстановлены")
                            }
                        }
                    }
                }
                row {
                    text("Отметки чтения и список меняются только сигналом приложения. Прочитать все остаётся доступным в открытом попапе.")
                }
            }
        }
    }

    private fun addNotificationLater() {
        val ui = UI.getCurrent()
        CompletableFuture.delayedExecutor(NOTIFICATION_DELAY_SECONDS, TimeUnit.SECONDS).execute {
            ui.accessLater(SerializableRunnable { if (isAttached) addNotification() }, SerializableRunnable {}).run()
        }
        status.set("Уведомление появится через 2 секунды: откройте колокольчик")
    }

    private fun addNotification(count: Int = 1) {
        val added = (1..count).map {
            val number = nextNotification.incrementAndGet()
            HeaderNotification(
                key = "new-$number",
                label = "Новое уведомление $number",
                destination = actionDestination("Открыто уведомление $number"),
                description = "Карточка держится 6 секунд; наведение и фокус ставят таймер на паузу",
            )
        }

        notifications.set(notifications.peek() + added)
        status.set(if (count == 1) "Добавлено уведомление ${nextNotification.get()}" else "Добавлено уведомлений: $count")
    }

    private fun initialNotifications(): List<HeaderNotification> = listOf(
        HeaderNotification(
            key = "round",
            label = "Тур опубликован",
            destination = actionDestination("Открыто уведомление: Тур опубликован"),
            description = "Весенний кубок готов к участию",
        ),
        HeaderNotification(
            key = "result",
            label = "Решение проверено",
            destination = actionDestination("Открыто уведомление: Решение проверено"),
            description = "Вердикт доступен в Кабинете",
        ),
        HeaderNotification(
            key = "read",
            label = "Прочитанное уведомление",
            destination = actionDestination("Открыто прочитанное уведомление"),
            isUnread = false,
        ),
    )

    private fun actionDestination(message: String): HeaderDestination = HeaderDestination.Action { status.set(message) }
}
