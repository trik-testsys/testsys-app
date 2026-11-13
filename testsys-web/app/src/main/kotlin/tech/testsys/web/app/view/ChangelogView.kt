package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.display.markdown
import tech.testsys.web.components.texts.currentLocalization

/**
 * User changelog of TestSys (testsys.web.page.changelog): a block for each version of the System with changes the
 * signed-in user sees: the common part and the parts of their roles, from the newest version to the oldest. Every
 * signed-in user opens it.
 *
 * @since %CURRENT_VERSION%
 */
@Route("changelog")
@PermitAll
class ChangelogView(headers: CabinetHeaders, currentUser: CurrentUser) : TestSysView(), HasDynamicTitle {
    init {
        val texts = currentLocalization().pageChangelog
        page(headers.cabinet()) {
            head(texts.title())
            guide(texts.guide())
            readChangelog(UI.getCurrent().locale.toLanguageTag(), changelogPartsOf(currentUser.user())).forEach { (version, content) ->
                row {
                    block(title = version) {
                        guide(texts.versionGuide())
                        row { markdown(content) }
                    }
                }
            }
        }
    }

    override fun getPageTitle(): String = currentLocalization().pageChangelog.title()
}
