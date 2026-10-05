package tech.testsys.web.devapp

import com.vaadin.flow.server.AppShellSettings
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.texts.buildUiTexts

class AppShellTests {
    private val texts = buildUiTexts()

    @Test
    fun `should use the TestSys emblem as favicon`() {
        val settings = mockk<AppShellSettings>(relaxed = true)

        AppShell(texts).configurePage(settings)

        verify { settings.addFavIcon("icon", TestSysBrand.FAVICON_URL, "any") }
    }

    @Test
    fun `should set page title to the brand text`() {
        val settings = mockk<AppShellSettings>(relaxed = true)

        AppShell(texts).configurePage(settings)

        verify { settings.setPageTitle(texts.brand) }
    }
}
