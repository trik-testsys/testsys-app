package tech.testsys.web.devapp.showcase

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.BrandAsset
import tech.testsys.web.components.display.brandImage
import tech.testsys.web.components.feedback.skeleton
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.devapp.demo.ui.DemoView

/** Illustrations of shared tokens and brand assets; visual rules belong to the UI documentation. */
@Route("dev/showcase/foundations")
internal class ShowcaseFoundationsView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Визуальные основы: примеры")
            foundations()
            footer {
                link("Демонстрация сценариев", DemoView::class.java)
            }
        }
    }
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) {
            event.rerouteToError(NotFoundException::class.java)
        }
    }
}

private fun PageScope.foundations() {
    row {
        block(title = "Состояния движения") {
            row { horizontal { action("Навести или сфокусировать") } }
            row { skeleton() }
        }
    }
    row {
        block(title = "Бренд") {
            BrandAsset.entries.forEach { asset ->
                row { brandImage(asset, label = "Фирменный ресурс TestSys") }
            }
        }
    }
}
