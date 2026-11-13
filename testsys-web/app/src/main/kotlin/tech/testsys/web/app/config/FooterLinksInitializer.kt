package tech.testsys.web.app.config

import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.security.AuthenticationContext
import tech.testsys.web.app.view.ChangelogView
import tech.testsys.web.components.layout.setCommonFooterLinks
import tech.testsys.web.components.texts.currentLocalization

/**
 * Sets the common footer links of every new UI: a signed-in user gets the link to the changelog in the footer of each
 * page, guest page and error screen (testsys.web.page.changelog).
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class FooterLinksInitializer(private val authenticationContext: AuthenticationContext) : VaadinServiceInitListener {
    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addUIInitListener { uiEvent ->
            setCommonFooterLinks(uiEvent.ui) {
                if (authenticationContext.isAuthenticated) link(currentLocalization().pageChangelog.title(), ChangelogView::class.java)
            }
        }
    }
}
