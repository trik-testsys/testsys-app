package tech.testsys.web.service.security

import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.VaadinServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import tech.testsys.web.app.security.RememberedSignIns

/**
 * Stores only the technical identity in the service session after rotating its id against session fixation and
 * remembers the sign-in in [signIns], so that it survives a restart.
 *
 * @since %CURRENT_VERSION%
 */
@Component
class ServiceSignIn(private val authenticator: ServiceAuthenticator, private val signIns: RememberedSignIns) {
    private val repository = HttpSessionSecurityContextRepository()

    /**
     * Checks [code] using the container's verified remote address and signs in only on success.
     *
     * @since %CURRENT_VERSION%
     */
    fun signIn(code: String): ServiceSignInResult {
        val request = checkNotNull(VaadinServletRequest.getCurrent()) {
            "Service sign-in needs a Vaadin servlet request"
        }.httpServletRequest
        val response = checkNotNull(VaadinServletResponse.getCurrent()) {
            "Service sign-in needs a Vaadin servlet response"
        }.httpServletResponse
        val result = authenticator.authenticate(address = request.remoteAddr, code = code)
        if (result != ServiceSignInResult.Success) return result

        request.getSession(true)
        request.changeSessionId()
        val strategy = SecurityContextHolder.getContextHolderStrategy()
        val context = strategy.createEmptyContext().apply { authentication = serviceAuthentication() }
        strategy.context = context
        repository.saveContext(context, request, response)
        signIns.remember(request, response, StoredCabinetSignIn.Service)
        return result
    }
}
