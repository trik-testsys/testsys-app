package tech.testsys.web.service

import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.web.service.security.ServiceSignIn
import tech.testsys.web.service.view.SERVICE_USER_ID_PARAMETER
import tech.testsys.web.service.view.ServiceUserView

class ServicePagesTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var signIn: ServiceSignIn

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("pages")
    fun `should build each service route and allow repeated navigation`(page: Class<out Component>, parameters: RouteParameters) {
        if (page.isAnnotationPresent(RolesAllowed::class.java)) signIn.signIn(SERVICE_TEST_SECRET)

        UI.getCurrent().navigate(page, parameters)
        assertEquals(page, currentView)
        UI.getCurrent().navigate(page, parameters)
        assertEquals(page, currentView)
    }

    companion object {
        /** Route parameters of the service pages with a required parameter, by the name of the set. */
        private val parameterSets: Map<Class<out Component>, Map<String, RouteParameters>> = mapOf(
            ServiceUserView::class.java to SERVICE_TEST_USERS.associate { user ->
                user::class.java.simpleName to RouteParameters(SERVICE_USER_ID_PARAMETER, user.id.value.toString())
            },
        )

        @JvmStatic
        fun pages(): List<Arguments> = Routes().autoDiscoverViews("tech.testsys.web.service").routes
            .sortedBy { page -> page.name }
            .flatMap { page ->
                parameterSets.getOrDefault(page, mapOf("" to RouteParameters.empty()))
                    .map { (name, parameters) -> argumentSet("${page.simpleName} $name".trim(), page, parameters) }
            }
    }
}
