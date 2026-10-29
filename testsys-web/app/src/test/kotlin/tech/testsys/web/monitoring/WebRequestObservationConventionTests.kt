package tech.testsys.web.monitoring

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.server.VaadinServletContext
import com.vaadin.flow.server.startup.ApplicationRouteRegistry
import io.micrometer.common.KeyValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext

class WebRequestObservationConventionTests {
    private val servletContext = MockServletContext().apply {
        val registry = ApplicationRouteRegistry.getInstance(VaadinServletContext(this))
        registry.setRoute("", HomePage::class.java, emptyList())
        registry.setRoute("developer/tasks/:taskId([0-9]+)", TaskPage::class.java, emptyList())
    }
    private val convention = WebRequestObservationConvention("main")

    @Test
    fun `should label a request with its interface`() {
        val labels = convention.getLowCardinalityKeyValues(context("/developer/tasks/42"))

        assertEquals("main", labels.stream().filter { label -> label.key == "interface" }.findFirst().orElseThrow().value)
    }

    @ParameterizedTest
    @CsvSource(
        "/, , 200, /",
        "/developer/tasks/42, , 200, /developer/tasks/:taskId",
        "/developer/tasks/42;jsessionid=ABC, , 200, /developer/tasks/:taskId",
        "/, v-r=uidl&v-uiId=0, 200, /?v-r=uidl",
        "/, v-uiId=0&v-r=heartbeat, 200, /?v-r=heartbeat",
        "/, v-r=init&location=developer%2Ftasks%2F42, 200, /?v-r=init",
        "/, v-r=push&v-uiId=0, 200, /?v-r=push",
        "/, v-r=webcomponent-42, 200, /?v-r=other",
        "/VAADIN/push, v-r=push&v-uiId=0, 200, /VAADIN/push",
        "/VAADIN/dynamic/resource/0/abc/report.csv, , 200, /VAADIN/dynamic/**",
        "/VAADIN/build/bundle-42.js, , 200, /VAADIN/**",
        "/testsys-ui/tokens.css, , 200, /testsys-ui/**",
        "/developer/tasks/abc, , 404, NOT_FOUND",
        "/no-such/987, , 404, NOT_FOUND",
        "/no-such/987, , 302, REDIRECTION",
        "/favicon.ico, , 200, OTHER",
    )
    fun `should name the endpoint without raw paths or query values`(path: String, query: String?, status: Int, endpoint: String) {
        val uri = convention.getLowCardinalityKeyValues(context(path, query, status)).stream()
            .filter { label -> label.key == "uri" }.findFirst().map(KeyValue::getValue).orElseThrow()

        assertEquals(endpoint, uri)
    }

    @Test
    fun `should name a page endpoint without the context path`() {
        val request = MockHttpServletRequest(servletContext, "GET", "/testsys/developer/tasks/42").apply { contextPath = "/testsys" }
        val context = ServerRequestObservationContext(request, MockHttpServletResponse())

        val uri = convention.getLowCardinalityKeyValues(context).stream()
            .filter { label -> label.key == "uri" }.findFirst().map(KeyValue::getValue).orElseThrow()

        assertEquals("/developer/tasks/:taskId", uri)
    }

    private fun context(path: String, query: String? = null, status: Int = 200): ServerRequestObservationContext {
        val request = MockHttpServletRequest(servletContext, "GET", path).apply { queryString = query }
        val response = MockHttpServletResponse().apply { this.status = status }
        return ServerRequestObservationContext(request, response)
    }

    class HomePage : Div()

    class TaskPage : Div()
}
