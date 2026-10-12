package tech.testsys.web.app

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.PasswordField
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tech.testsys.web.app.view.AuthenticationView

@Import(PostgresTestConfiguration::class)
@SpringBootTest
class MetricsPrivacyTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var registry: PrometheusMeterRegistry

    @Test
    fun `should count a refused sign-in without the entered access code`() {
        val accessCode = "privacy-check-" + fixtures.unique("code")
        UI.getCurrent().navigate(AuthenticationView::class.java)
        UI.getCurrent()._find<PasswordField>().single().value = accessCode
        UI.getCurrent()._get<Button> { text = "Войти" }._click()

        val exported = registry.scrape()

        assertTrue("operation=\"UserService.authenticate\"" in exported)
        assertFalse(accessCode in exported)
    }
}
