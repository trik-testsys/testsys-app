package tech.testsys.web.service.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.api.service.ServiceTrikStudioVersions
import tech.testsys.infra.database.api.service.TrikStudioVersionRegistration
import tech.testsys.infra.database.api.service.TrikStudioVersionRejection
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.overlay.dialog

/**
 * Service list of the registered TRIK Studio versions with their version numbers and Docker image tags; «Добавить»
 * registers a new version.
 *
 * @since %CURRENT_VERSION%
 */
@Route("trik-studio-versions")
@PageTitle("Версии TRIK Studio")
@RolesAllowed("SERVICE")
class ServiceTrikStudioVersionsView(private val versions: ServiceTrikStudioVersions) : TestSysView() {
    private lateinit var rows: TableHandle<TrikStudioVersion>

    init {
        page(ServiceHeaders.header()) {
            head("Версии TRIK Studio")
            row {
                block(title = "Версии TRIK Studio") {
                    actions { action("Добавить", icon = IconName.Plus) { onClick { openAddDialog() } } }
                    rows = table(
                        key = { version: TrikStudioVersion -> version.tag },
                        fetch = { request ->
                            val registered = versions.findAll()
                            Page(registered.drop(request.offset).take(request.limit), registered.size)
                        },
                    ) {
                        codeColumn("Версия", size = 8) { version -> version.version }
                        codeColumn("Тег образа", size = 16) { version -> version.tag }
                        empty("Версий TRIK Studio нет", "Добавьте версию: номер и тег Docker-образа.")
                    }
                }
            }
        }
    }

    private fun openAddDialog() {
        dialog(title = "Новая версия TRIK Studio") {
            lateinit var version: ValueInput<String>
            lateinit var tag: ValueInput<String>
            row { version = textInput("Версия", labelSize = 8, size = 16, hint = "2026.2") }
            row { tag = textInput("Тег образа", labelSize = 8, size = 16, hint = "<репозиторий>:<тег>") }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Добавить") {
                    onClick {
                        when (val result = versions.register(tag = tag.value, version = version.value)) {
                            is TrikStudioVersionRegistration.Registered -> {
                                dialog.close()
                                toast(FeedbackKind.Success, "Версия TRIK Studio добавлена")
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
        TrikStudioVersionRejection.BlankVersion -> "Укажите версию"
        TrikStudioVersionRejection.VersionTooLong -> "Версия длиннее 255 символов"
        TrikStudioVersionRejection.VersionAlreadyRegistered -> "Такая версия уже зарегистрирована"
        TrikStudioVersionRejection.BlankTag -> "Укажите тег"
        TrikStudioVersionRejection.TagTooLong -> "Тег длиннее 512 символов"
        TrikStudioVersionRejection.TagAlreadyRegistered -> "Такой тег уже зарегистрирован"
    }
}
