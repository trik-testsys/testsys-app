package tech.testsys.web.app

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tech.testsys.web.app.view.AuthenticationView
import tech.testsys.web.app.view.SignInMethod

@Import(PostgresTestConfiguration::class)
@SpringBootTest
class MetricsPrivacyTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var registry: PrometheusMeterRegistry

    @Test
    fun `should count a refused sign-in without the entered access code`() {
        val accessCode = "privacy-check-" + fixtures.unique("code")
        UI.getCurrent().navigate(AuthenticationView::class.java)
        UI.getCurrent()._get<RadioButtonGroup<SignInMethod?>>().value = SignInMethod.AccessCode
        UI.getCurrent()._find<PasswordField>().single { field -> field.ariaLabel.orElse(null) == "Код-доступа" }.value = accessCode
        UI.getCurrent()._get<Button> { text = "Войти" }._click()

        val exported = registry.scrape()

        assertTrue("operation=\"UserService.authenticate\"" in exported)
        assertFalse(accessCode in exported)
    }

    @Test
    fun `should count a refused sign-in by password without the entered e-mail address and password`() {
        val email = "privacy-check-${fixtures.unique("email")}@example.com"
        val password = "privacy-check-" + fixtures.unique("password")
        UI.getCurrent().navigate(AuthenticationView::class.java)
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == "Почта" }.value = email
        UI.getCurrent()._find<PasswordField>().single { field -> field.ariaLabel.orElse(null) == "Пароль" }.value = password
        UI.getCurrent()._get<Button> { text = "Войти" }._click()

        val exported = registry.scrape()

        assertTrue("operation=\"UserService.authenticateByPassword\"" in exported)
        assertFalse(email in exported)
        assertFalse(password in exported)
    }
}
