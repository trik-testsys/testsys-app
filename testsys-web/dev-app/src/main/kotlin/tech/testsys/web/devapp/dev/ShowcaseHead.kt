package tech.testsys.web.devapp.dev

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.CabinetHeader
import tech.testsys.web.components.navigation.HeaderUser
import tech.testsys.web.components.navigation.NavItem

/** Profile in which the showcase pages open. */
internal const val DEV_PROFILE = "dev"

/** Sections of the navigation and states page, shown as the counter of its tab. */
private const val STATES_SECTION_COUNT = 8

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
            tab("Поля и файлы", ShowcaseFormsView::class.java)
            tab("Оверлеи", ShowcaseOverlaysView::class.java)
            tab("Шапка", ShowcaseHeaderView::class.java)
            tab("Отображение", ShowcaseDisplayView::class.java)
            tab("Компоненты", ShowcaseView::class.java)
            tab(
                "Навигация и состояния",
                ShowcaseStatesView::class.java,
                count = STATES_SECTION_COUNT,
                countKind = CounterKind.Neutral,
                activeOn = setOf(ShowcaseNestedView::class.java),
            )
        }
    }
}
