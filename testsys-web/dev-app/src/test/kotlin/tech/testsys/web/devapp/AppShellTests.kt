package tech.testsys.web.devapp

import com.vaadin.flow.server.AppShellSettings
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.web.components.localization.buildUiTexts

class AppShellTests {
    private val texts = buildUiTexts(SupportedRegion.RU)

    @Test
    fun `should set page title to the brand text`() {
        val settings = mockk<AppShellSettings>(relaxed = true)

        AppShell(texts).configurePage(settings)

        verify { settings.setPageTitle(texts.brand) }
    }
}
