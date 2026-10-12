@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.dependency.NpmPackage
import com.vaadin.flow.component.html.NativeButton
import tech.testsys.infra.localization.UiGuide
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.DomProperty
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setProperty
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon

private const val GUIDE_MODULE = "./testsys-ui/guide.ts"
private const val GUIDE_CONTROLLER_MODULE = "./testsys-ui/guide-controller.ts"
private const val GUIDE_TAG = "ts-guide-controller"
private const val DRIVER_PACKAGE = "driver.js"

@Tag(GUIDE_TAG)
@JsModule(GUIDE_MODULE)
@JsModule(GUIDE_CONTROLLER_MODULE)
@NpmPackage(value = DRIVER_PACKAGE, version = DRIVER_VERSION)
internal class GuideClient(texts: UiGuide) : Component() {
    init {
        element.classList.add(CssClass.GuideController)
        // The custom element owns this protocol; the application only supplies semantic texts.
        element.setProperty(DomProperty.GuidePreviousLabel, texts.previous())
        element.setProperty(DomProperty.GuideNextLabel, texts.next())
        element.setProperty(DomProperty.GuideDoneLabel, texts.done())
        element.setProperty(DomProperty.GuideCloseLabel, texts.close())
    }
}

internal fun guideButton(label: String, isPage: Boolean): NativeButton = NativeButton().apply {
    addClassNames(CssClass.Btn, CssClass.BtnGhost, CssClass.BtnIcon, CssClass.GuideButton)
    element.setType(ElementType.Button)
    element.setAttribute(HtmlAttribute.AriaLabel, label)
    element.setAttribute(HtmlAttribute.Title, label)
    element.setAttribute(HtmlAttribute.DataGuideStart, if (isPage) "page" else "block")
    add(svgIcon(IconName.Info))
}
