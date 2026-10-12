package tech.testsys.web.app

import com.vaadin.flow.server.AppShellSettings
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import tech.testsys.web.components.TestSysBrand

class AppShellTests {
    private val environment = MockEnvironment()
        .withProperty("testsys.web.localization.default-region", "RU")
        .withProperty("testsys.web.localization.default-time-zone", "Europe/Moscow")

    @Test
    fun `should use the TestSys emblem as favicon`() {
        val settings = mockk<AppShellSettings>(relaxed = true)

        AppShell(environment).configurePage(settings)

        verify { settings.addFavIcon("icon", TestSysBrand.FAVICON_URL, "any") }
    }

    @Test
    fun `should set page title to the brand name of the default region`() {
        val settings = mockk<AppShellSettings>(relaxed = true)

        AppShell(environment).configurePage(settings)

        verify { settings.setPageTitle("TestSys") }
    }
}
