package tech.testsys.web.service.error

import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteConfiguration
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.web.monitoring.FailingPage
import tech.testsys.web.service.ServiceMockVaadinTests

class ServiceInternalErrorViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var registry: MeterRegistry

    @BeforeEach
    fun registerFailingPage() {
        RouteConfiguration.forApplicationScope().setAnnotatedRoute(FailingPage::class.java)
    }

    @Test
    fun `should show the internal error screen for an unexpected exception while opening a service page`() {
        UI.getCurrent().navigate(FailingPage::class.java)

        assertEquals(ServiceInternalErrorView::class.java, currentView)
    }

    @Test
    fun `should count an unexpected exception while opening a service page`() {
        val before = navigationFailures()

        UI.getCurrent().navigate(FailingPage::class.java)

        assertEquals(before + 1, navigationFailures())
    }

    private fun navigationFailures(): Double = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
        .tags(TestSysMetrics.SOURCE, "service", TestSysMetrics.STAGE, "navigation", TestSysMetrics.EXCEPTION, "IllegalStateException")
        .counter()?.count() ?: 0.0
}
