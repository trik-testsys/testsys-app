package tech.testsys.web.devapp.demo.ui

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
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
import tech.testsys.web.devapp.demo.DemoSession
import tech.testsys.web.devapp.demo.model.DemoResult
import tech.testsys.web.devapp.demo.model.DemoState
import tech.testsys.web.devapp.demo.model.DemoTableState
import tech.testsys.web.devapp.demo.model.actor
import tech.testsys.web.devapp.showcase.ShowcaseView

/** Existing mock scenarios composed with the public Kotlin DSL, without production operations. */
@Route("dev/demo/:screen?")
internal class DemoView(
    texts: UiTexts,
    private val session: DemoSession,
    private val environment: Environment,
) : TestSysView(texts), BeforeEnterObserver {
    private var screen: String = "home"
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles("dev")) {
            event.rerouteToError(NotFoundException::class.java)
            return
        }
        screen = demoScreen(event.routeParameters.get("screen").orElse("home"))
        render()
    }
    private fun render() {
        val cabinet = DEMO_CABINETS.firstOrNull {
            screen.startsWith(it.key + ".")
        }
        val section = screen.substringAfter('.', "overview")
        val actor = cabinet?.let {
            session.state.actor(it.role)
        }
        val context = DemoContext(session, screen, ::render, ::navigate)
        val title = when {
            screen == "login" -> "Вход и регистрация"
            cabinet == null -> "Главная"
            section == "overview" -> cabinet.label
            else -> cabinet.label
        }
        page(header()) {
            head(title) {
                crumb("Демонстрация", DemoView::class.java, parameters("home"))
                meta("Демонстрационные данные · без серверных операций")
                if (cabinet != null) {
                    tabs(matchRouteParameters = true) {
                        cabinet.sections.forEach { (key, label) ->
                            tab(label, DemoView::class.java, parameters("${cabinet.key}.$key"))
                        }
                    }
                }
                actions {
                    action("Сбросить демонстрацию") {
                        onClick {
                            session.reset()
                            navigate("home")
                        }
                    }
                }
            }
            when {
                screen == "home" -> demoHome(context)
                screen == "login" -> demoLogin(context)
                cabinet != null && actor != null -> demoCabinet(context, cabinet, actor, section)
            }
            footer {
                link("Компоненты и примеры", ShowcaseView::class.java)
            }
        }
    }
    private fun header(): CabinetHeader {
        val user = session.state.users.firstOrNull { actor -> actor.id == session.state.sessionUserId }
        val columns = DEMO_CABINETS.map { cabinet ->
            HeaderMegaColumn(
                title = demoRoleLabel(cabinet.role),
                links = cabinet.sections.filter { (key, _) -> key != "overview" }.map { (key, label) ->
                    HeaderMegaLink(
                        label,
                        HeaderDestination.Route(
                            DemoView::class.java,
                            parameters("${cabinet.key}.$key"),
                        ),
                    )
                },
                destination = HeaderDestination.Route(DemoView::class.java, parameters("${cabinet.key}.overview")),
                searchText = cabinet.label,
            )
        }
        return CabinetHeader(
            items = listOf(
                NavItem(key = "home", label = "Главная", target = DemoView::class.java),
                MegaMenuItem(key = "cabinets", label = "Меню", menu = HeaderMegaMenu(columns)),
            ),
            active = when {
                screen == "home" -> "home"
                screen != "login" -> "cabinets"
                else -> null
            },
            user = user?.let { actor ->
                HeaderUser(
                    name = actor.alias,
                    menu = HeaderUserMenu(
                        listOf(
                            HeaderUserMenuItem("Главная", HeaderDestination.Route(DemoView::class.java, parameters("home"))),
                            HeaderUserMenuItem(
                                "Обзор своего кабинета",
                                HeaderDestination.Route(
                                    DemoView::class.java,
                                    parameters("${DEMO_CABINETS.first { cabinet -> cabinet.role == actor.role }.key}.overview"),
                                ),
                            ),
                            HeaderUserMenuItem("Вход", HeaderDestination.Route(DemoView::class.java, parameters("login"))),
                            HeaderUserMenuItem(
                                "Восстановить доступ",
                                HeaderDestination.Route(DemoView::class.java, parameters("login")),
                            ),
                        ),
                    ),
                )
            },
            signIn = DemoView::class.java,
            signInParameters = parameters("login"),
            notifications = HeaderNotifications(
                items = ValueSignal(
                    listOf(
                        HeaderNotification(
                            key = "demo-state",
                            label = "Демонстрационные данные сохраняются при переходах",
                            destination = HeaderDestination.Action {},
                            isUnread = false,
                        ),
                    ),
                ),
                onRead = {},
                onReadAll = {},
            ),
            menuSearchKey = "cabinets",
        )
    }
    private fun navigate(target: String) {
        UI.getCurrent().navigate(DemoView::class.java, parameters(target))
    }
}

/** Parameters shared by all declared demo route links. */
internal fun parameters(screen: String): RouteParameters = RouteParameters("screen", screen)

/** UI callbacks and state for composable demonstration sections. */
internal class DemoContext(
    val session: DemoSession,
    val screen: String,
    val render: () -> Unit,
    val navigate: (String) -> Unit,
) {
    val state: DemoState get() = session.state
    fun apply(result: DemoResult): Boolean {
        val isSuccess = session.apply(result)
        toast(if (isSuccess) FeedbackKind.Success else FeedbackKind.Error, result.message)
        return isSuccess
    }
    fun selection(key: String, fallback: String?): String? = session.selections[key] ?: fallback
    fun select(key: String, id: String) {
        session.selections[key] = id
        render()
    }
    fun table(key: String): DemoTableState = session.tables.getOrPut(key) {
        DemoTableState()
    }
    fun created(result: DemoResult, selectionKey: String, id: String): Boolean {
        if (!apply(result)) {
            return false
        }
        session.selections[selectionKey] = id
        session.tables.clear()
        render()
        return true
    }
}

/** Home links preserve all eight existing cabinets and the mock authentication screen. */
private fun PageScope.demoHome(context: DemoContext) {
    block("Демонстрация сценариев") {
        row {
            horizontal {
                action("Открыть вход и регистрацию") {
                    onClick {
                        context.navigate("login")
                    }
                }
                DEMO_CABINETS.forEach { cabinet ->
                    action("Войти как ${demoRoleLabel(cabinet.role)}") {
                        onClick {
                            context.navigate("${cabinet.key}.overview")
                        }
                    }
                }
            }
        }
        row {
            text("Выбор кабинета здесь — инструмент демонстрации. Права реального приложения не реализованы.")
        }
    }
}
