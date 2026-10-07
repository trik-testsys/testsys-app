package tech.testsys.web.devapp.showcase

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.devapp.demo.ui.DemoView

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
            footer {
                link("Демонстрация сценариев", DemoView::class.java)
                link("Поля и файлы", ShowcaseFormsView::class.java)
                link(label = "Исходный код", href = "https://github.com/trik-testsys/testsys-app")
            }
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
