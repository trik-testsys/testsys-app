package tech.testsys.web.service.error

import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.VaadinSession
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.web.service.ServiceMockVaadinTests

class ServiceErrorHandlerTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var handler: ServiceErrorHandler

    @Autowired
    private lateinit var registry: MeterRegistry

    @Test
    fun `should count an unexpected exception of an event handler`() {
        val before = eventFailures()

        handler.error(ErrorEvent(IllegalStateException("listener failed")))

        assertEquals(before + 1, eventFailures())
    }

    @Test
    fun `should become the error handler of a new service session`() {
        val sessionHandler = VaadinSession.getCurrent().errorHandler

        assertSame(handler, sessionHandler)
    }

    private fun eventFailures(): Double = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
        .tags(TestSysMetrics.SOURCE, "service", TestSysMetrics.STAGE, "event", TestSysMetrics.EXCEPTION, "IllegalStateException")
        .counter()?.count() ?: 0.0
}
