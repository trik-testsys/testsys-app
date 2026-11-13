package tech.testsys.web.app.config

import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.security.AuthenticationContext
import org.springframework.core.env.Environment
import tech.testsys.web.app.view.ChangelogView
import tech.testsys.web.components.layout.setCommonFooterLinks
import tech.testsys.web.components.layout.setFooterVersion
import tech.testsys.web.components.texts.currentLocalization

/**
 * Sets the footer of every new UI: the version of the system from `spring.application.version`, and for a signed-in
 * user the link to the changelog in the footer of each page, guest page and error screen (testsys.web.page.changelog).
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class FooterInitializer(
    private val authenticationContext: AuthenticationContext,
    environment: Environment,
) : VaadinServiceInitListener {
    private val version = environment.getProperty("spring.application.version")

    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addUIInitListener { uiEvent ->
            setFooterVersion(uiEvent.ui, version)
            setCommonFooterLinks(uiEvent.ui) {
                if (authenticationContext.isAuthenticated) link(currentLocalization().pageChangelog.title(), ChangelogView::class.java)
            }
        }
    }
}
