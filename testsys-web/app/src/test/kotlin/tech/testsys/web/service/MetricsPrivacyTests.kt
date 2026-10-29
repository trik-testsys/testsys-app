package tech.testsys.web.service

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.router.RouteParameters
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.web.monitoring.FailingPage
import tech.testsys.web.service.security.ServiceSignIn
import tech.testsys.web.service.view.SERVICE_USER_ID_PARAMETER
import tech.testsys.web.service.view.ServiceUserView

class MetricsPrivacyTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var signIn: ServiceSignIn

    @Autowired
    private lateinit var registry: PrometheusMeterRegistry

    @Test
    fun `should keep the service code and an opened user out of the exported metrics`() {
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceUserView::class.java, RouteParameters(SERVICE_USER_ID_PARAMETER, "1003"))
        RouteConfiguration.forApplicationScope().setAnnotatedRoute(FailingPage::class.java)
        UI.getCurrent().navigate(FailingPage::class.java)

        val exported = registry.scrape()

        assertFalse(SERVICE_TEST_SECRET in exported)
        assertFalse("observer-token" in exported)
        assertFalse("1003" in exported)
    }
}
