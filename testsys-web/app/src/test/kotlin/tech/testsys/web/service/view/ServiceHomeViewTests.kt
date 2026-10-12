package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.web.service.SERVICE_TEST_GRAFANA_URL
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.security.ServiceSignIn

class ServiceHomeViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var signIn: ServiceSignIn

    @BeforeEach
    fun openHome() {
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceHomeView::class.java)
        pendingJavaScript()
    }

    @Test
    fun `should open only the configured Grafana address in the same tab without passing service credentials`() {
        UI.getCurrent()._get<Button> { text = "Grafana" }._click()

        val transitions = pendingJavaScript().filter { call -> "window.open" in call.invocation.expression }
        assertEquals(listOf(listOf(SERVICE_TEST_GRAFANA_URL, "_self")), transitions.map { call -> call.invocation.parameters })
        assertEquals(ServiceHomeView::class.java, currentView)
    }

    private fun pendingJavaScript() = UI.getCurrent().internals.let { internals ->
        internals.stateTree.runExecutionsBeforeClientResponse()
        internals.dumpPendingJavaScriptInvocations()
    }
}
