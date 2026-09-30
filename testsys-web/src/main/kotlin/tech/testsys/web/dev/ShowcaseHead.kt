package tech.testsys.web.dev

import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.display.CounterKind
import tech.testsys.web.ui.display.Tone
import tech.testsys.web.ui.feedback.FeedbackKind
import tech.testsys.web.ui.feedback.toast
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.navigation.CabinetHeader
import tech.testsys.web.ui.navigation.HeaderUser
import tech.testsys.web.ui.navigation.NavItem

/** Profile in which the showcase pages open. */
internal const val DEV_PROFILE = "dev"

/** Sections of the navigation and states page, shown as the counter of its tab. */
private const val STATES_SECTION_COUNT = 7

/** Cabinet header of the showcase pages. */
internal fun showcaseHeader(): CabinetHeader = CabinetHeader(
    items = listOf(NavItem(key = "showcase", label = "Витрина", target = ShowcaseView::class.java)),
    active = "showcase",
    user = HeaderUser("Анна Петрова"),
)

/** Page head shared by the showcase pages: a breadcrumb, a badge, a note, an action and the tabs of the pages. */
internal fun PageScope.showcaseHead(title: String) {
    head(title) {
        crumb("Витрина", ShowcaseView::class.java)
        badge("dev", Tone.Info)
        meta("Только в профиле dev")
        actions {
            action("Действие заголовка", IconName.Megaphone) {
                onClick { toast(FeedbackKind.Info, "Кнопка в заголовке страницы") }
            }
        }
        tabs {
            tab("Компоненты", ShowcaseView::class.java)
            tab("Навигация и состояния", ShowcaseStatesView::class.java, count = STATES_SECTION_COUNT, countKind = CounterKind.Neutral)
        }
    }
}
