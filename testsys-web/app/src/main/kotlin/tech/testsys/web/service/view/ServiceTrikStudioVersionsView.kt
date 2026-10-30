package tech.testsys.web.service.view

import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.api.service.ServiceTrikStudioVersions
import tech.testsys.infra.database.api.service.TrikStudioVersionDeletion
import tech.testsys.infra.database.api.service.TrikStudioVersionRegistration
import tech.testsys.infra.database.api.service.TrikStudioVersionRejection
import tech.testsys.infra.database.api.service.TrikStudioVersionTagChange
import tech.testsys.infra.localization.PageService
import tech.testsys.web.app.view.LoadedTable
import tech.testsys.web.app.view.loadTable
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization

/**
 * Service list of the registered TRIK Studio versions with their version numbers and Docker image tags; «Добавить»
 * registers a new version, the row menu changes the tag of any version and deletes a version nothing uses.
 *
 * @since 1.0.0-beta.1
 */
@Route("trik-studio-versions")
@RolesAllowed("SERVICE")
class ServiceTrikStudioVersionsView(
    private val headers: ServiceHeaders,
    private val versions: ServiceTrikStudioVersions,
) : TestSysView(),
    HasDynamicTitle {
    private val texts: PageService get() = currentLocalization().pageService

    private lateinit var rows: LoadedTable<TrikStudioVersion>
    private var usedTags: Set<String> = emptySet()

    init {
        page(headers.header(active = ServiceHeaders.MENU_SECTION)) {
            head(texts.versionsTitle())
            row {
                block(title = texts.versionsTitle()) {
                    actions { action(texts.add(), icon = IconName.Plus) { onClick { openAddDialog() } } }
                    rows = loadTable(
                        key = { version: TrikStudioVersion -> version.tag },
                        fetch = { request ->
                            val registered = versions.findAll()
                            usedTags = versions.findUsedTags()
                            Page(registered.drop(request.offset).take(request.limit), registered.size)
                        },
                    ) {
                        codeColumn(texts.versionsVersion(), size = 8) { version -> version.version }
                        codeColumn(texts.versionsTag()) { version -> version.tag }
                        menuColumn(ariaLabel = { version -> texts.versionsActions(version = version.version) }) { version ->
                            item(texts.versionsChangeTag()) { openTagDialog(version) }
                            if (version.tag !in usedTags) destructiveItem(texts.delete()) { delete(version) }
                        }
                        empty(texts.versionsEmpty(), texts.versionsEmptyHint())
                    }
                }
            }
        }
    }

    override fun getPageTitle(): String = texts.versionsTitle()

    private fun openAddDialog() {
        dialog(title = texts.versionsAddTitle()) {
            lateinit var version: ValueInput<String>
            lateinit var tag: ValueInput<String>
            row { version = textInput(texts.versionsVersion(), labelSize = 8, size = 16, hint = "2026.2") }
            row { tag = textInput(texts.versionsTag(), labelSize = 8, size = 16, hint = texts.versionsTagHint()) }
            footer { dialog ->
                action(texts.cancel()) { onClick { dialog.close() } }
                mainAction(texts.add()) {
                    onClick {
                        when (val result = versions.register(tag = tag.value, version = version.value)) {
                            is TrikStudioVersionRegistration.Registered -> {
                                dialog.close()
                                toast(FeedbackKind.Success, texts.versionsAdded())
                                rows.refresh()
                            }
                            is TrikStudioVersionRegistration.Rejected -> {
                                val error = rejectionText(result.reason)
                                val isVersionError = isVersionRejection(result.reason)
                                version.showError(error.takeIf { isVersionError })
                                tag.showError(error.takeUnless { isVersionError })
                            }
                        }
                    }
                }
            }
        }.open()
    }

    private fun openTagDialog(version: TrikStudioVersion) {
        dialog(title = texts.versionsTagTitle(version = version.version)) {
            lateinit var tag: ValueInput<String>
            row { tag = textInput(texts.versionsTag(), labelSize = 8, size = 16, hint = texts.versionsTagHint()) { value = version.tag } }
            footer { dialog ->
                action(texts.cancel()) { onClick { dialog.close() } }
                mainAction(texts.save()) {
                    onClick {
                        when (val result = versions.changeTag(tag = version.tag, newTag = tag.value)) {
                            is TrikStudioVersionTagChange.Changed -> {
                                dialog.close()
                                toast(FeedbackKind.Success, texts.versionsTagChanged())
                                rows.refresh()
                            }
                            is TrikStudioVersionTagChange.Rejected -> tag.showError(rejectionText(result.reason))
                            TrikStudioVersionTagChange.NotFound -> {
                                dialog.close()
                                toast(FeedbackKind.Error, texts.versionsAlreadyDeleted())
                                rows.refresh()
                            }
                        }
                    }
                }
            }
        }.open()
    }

    private fun delete(version: TrikStudioVersion) {
        confirm(
            title = texts.versionsDeleteTitle(version = version.version),
            text = texts.versionsDeleteText(),
            action = texts.delete(),
            isDanger = true,
        ) {
            when (versions.delete(version.tag)) {
                TrikStudioVersionDeletion.Deleted -> toast(FeedbackKind.Success, texts.versionsDeleted())
                TrikStudioVersionDeletion.InUse -> toast(FeedbackKind.Error, texts.versionsInUse())
                TrikStudioVersionDeletion.NotFound -> toast(FeedbackKind.Error, texts.versionsAlreadyDeleted())
            }
            rows.refresh()
        }
    }

    private fun ValueInput<String>.showError(message: String?) {
        isInvalid = message != null
        errorMessage = message
    }

    private fun isVersionRejection(reason: TrikStudioVersionRejection): Boolean = when (reason) {
        TrikStudioVersionRejection.BlankVersion,
        TrikStudioVersionRejection.VersionTooLong,
        TrikStudioVersionRejection.VersionAlreadyRegistered,
        -> true
        TrikStudioVersionRejection.BlankTag,
        TrikStudioVersionRejection.TagTooLong,
        TrikStudioVersionRejection.TagAlreadyRegistered,
        -> false
    }

    private fun rejectionText(reason: TrikStudioVersionRejection): String = when (reason) {
        TrikStudioVersionRejection.BlankVersion -> texts.versionsRejectionBlankVersion()
        TrikStudioVersionRejection.VersionTooLong -> texts.versionsRejectionVersionTooLong()
        TrikStudioVersionRejection.VersionAlreadyRegistered -> texts.versionsRejectionVersionRegistered()
        TrikStudioVersionRejection.BlankTag -> texts.versionsRejectionBlankTag()
        TrikStudioVersionRejection.TagTooLong -> texts.versionsRejectionTagTooLong()
        TrikStudioVersionRejection.TagAlreadyRegistered -> texts.versionsRejectionTagRegistered()
    }
}
