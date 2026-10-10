package tech.testsys.web.service

import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import jakarta.annotation.security.RolesAllowed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.web.service.security.ServiceSignIn

class ServicePagesTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var signIn: ServiceSignIn

    @ParameterizedTest
    @MethodSource("pages")
    fun `should build each service route and allow repeated navigation`(page: Class<out Component>) {
        if (page.isAnnotationPresent(RolesAllowed::class.java)) signIn.signIn(SERVICE_TEST_SECRET)

        UI.getCurrent().navigate(page)
        assertEquals(page, currentView)
        UI.getCurrent().navigate(page)
        assertEquals(page, currentView)
    }

    companion object {
        @JvmStatic
        fun pages(): List<Class<out Component>> = Routes().autoDiscoverViews("tech.testsys.web.service").routes.toList()
    }
}
