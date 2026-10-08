package tech.testsys.web.app

import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10._errorMessage
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.InternalServerError
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest

/** Opens every page of the application so that a page build error fails the build. */
@SpringBootTest
class PagesTests : MockSpringVaadinTests() {
    @ParameterizedTest(name = "{argumentSetName}", allowZeroInvocations = true)
    @MethodSource("pages")
    fun `should open the page without a build error`(page: Class<out Component>, parameters: RouteParameters) {
        UI.getCurrent().navigate(page, parameters)

        assertEquals(page, currentView) {
            UI.getCurrent().internals.activeRouterTargetsChain
                .filterIsInstance<InternalServerError>()
                .joinToString { error -> error._errorMessage }
        }
    }

    companion object {
        @JvmStatic
        fun pages(): List<Arguments> = Routes().autoDiscoverViews(MockSpringVaadinTests::class.java.packageName).routes
            .filter { page -> page.isAnnotationPresent(Route::class.java) }
            .sortedBy { page -> page.name }
            .map { page -> argumentSet(page.simpleName, page, RouteParameters.empty()) }
    }
}
