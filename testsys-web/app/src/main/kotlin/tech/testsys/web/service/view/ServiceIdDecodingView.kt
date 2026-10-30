package tech.testsys.web.service.view

import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.infra.database.api.service.ServiceIdParts
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Service decoding of an entity id into the moment of issue in UTC, the node id and the per-second counter.
 *
 * @since 1.0.0-beta.1
 */
@Route("id-decoding")
@RolesAllowed("SERVICE")
class ServiceIdDecodingView(headers: ServiceHeaders) :
    TestSysView(),
    HasDynamicTitle {
    init {
        val texts = currentLocalization().pageService
        page(headers.header(active = ServiceHeaders.MENU_SECTION)) {
            head(texts.idDecodingTitle())
            row {
                block(title = texts.idDecodingBlock()) {
                    lateinit var id: ValueInput<String>
                    lateinit var issuedAt: ValueInput<String>
                    lateinit var nodeId: ValueInput<String>
                    lateinit var counter: ValueInput<String>
                    row { id = codeInput(texts.id(), labelSize = 3, size = 9, hint = texts.idDecodingIdHint()) }
                    row {
                        issuedAt = codeInput(texts.idDecodingIssuedAt(), labelSize = 3, size = 9) { isEditable = false }
                        nodeId = codeInput(texts.idDecodingNode(), labelSize = 3, size = 9) { isEditable = false }
                    }
                    row { counter = codeInput(texts.idDecodingCounter(), labelSize = 3, size = 9) { isEditable = false } }
                    actions {
                        action(texts.idDecodingDecode()) {
                            onClick {
                                val number = id.value.trim().toLongOrNull()
                                val parts = number?.let(ServiceIdParts::decode)
                                id.errorMessage = when {
                                    number == null -> texts.numberRequired()
                                    parts == null -> texts.idDecodingNotIssued()
                                    else -> null
                                }
                                id.isInvalid = parts == null
                                issuedAt.value = parts?.issuedAt?.toString().orEmpty()
                                nodeId.value = parts?.nodeId?.toString().orEmpty()
                                counter.value = parts?.counter?.toString().orEmpty()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getPageTitle(): String = currentLocalization().pageService.idDecodingTitle()
}
