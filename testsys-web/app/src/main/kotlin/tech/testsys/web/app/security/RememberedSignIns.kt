package tech.testsys.web.app.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.boot.convert.DurationStyle
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.HexFormat

/** Property of the session idle time of a servlet context. */
private const val SESSION_TIMEOUT_PROPERTY = "server.servlet.session.timeout"

/** Session idle time of Spring Boot when [SESSION_TIMEOUT_PROPERTY] is not set. */
private val DEFAULT_SESSION_TIMEOUT: Duration = Duration.ofMinutes(30)

/** Bytes of a sign-in token. */
private const val TOKEN_BYTES = 32

/** Shortest pause between two records of the last request of one session. */
private val TOUCH_INTERVAL: Duration = Duration.ofMinutes(1)

/** Session attribute with the digest of the token of the session's sign-in. */
private const val TOKEN_HASH_ATTRIBUTE = "tech.testsys.signIn.tokenHash"

/** Session attribute with the moment of the last record of the session's request, in epoch milliseconds. */
private const val TOUCHED_AT_ATTRIBUTE = "tech.testsys.signIn.touchedAt"

/**
 * Sign-ins of one interface that survive a restart: a random token in the cookie [cookieName] and its digest from
 * [tokenHash] with the signed-in user in [store]. A sign-in idle for [idleTimeout] is not restored; the next sign-in
 * deletes it and records the end of a supervisor signed in as a user.
 *
 * @since 1.0.0-beta.1
 */
class RememberedSignIns(
    private val store: CabinetSignInStore,
    private val cookieName: String,
    private val idleTimeout: Duration,
    private val clock: Clock,
    private val tokenHash: (String) -> String = ::sha256Hex,
) {
    private val random = SecureRandom()

    /**
     * Remembers [signIn] for the session of [request], already signed in with its new id, replacing a sign-in the
     * session remembered before, and sets the cookie on [response].
     *
     * @since 1.0.0-beta.1
     */
    fun remember(request: HttpServletRequest, response: HttpServletResponse, signIn: StoredCabinetSignIn) {
        val session = request.getSession(true)
        (session.getAttribute(TOKEN_HASH_ATTRIBUTE) as? String)?.let(store::delete)

        val now = clock.instant()
        logEndedSupervisorSessions(store.deleteIdle(before = now - idleTimeout))
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(TOKEN_BYTES).also(random::nextBytes))
        val hash = tokenHash(token)
        store.create(hash, signIn, now)
        session.setAttribute(TOKEN_HASH_ATTRIBUTE, hash)
        session.setAttribute(TOUCHED_AT_ATTRIBUTE, now.toEpochMilli())
        response.addCookie(cookie(request, token, isSet = true))
    }

    /**
     * Returns the active sign-in of the cookie of [request] with its token digest, or `null` without one; a cookie
     * without an active sign-in is cleared on [response], and an idle sign-in stays in the store.
     *
     * @since 1.0.0-beta.1
     */
    fun find(request: HttpServletRequest, response: HttpServletResponse): Pair<String, StoredCabinetSignIn>? {
        val token = request.cookies?.firstOrNull { cookie -> cookie.name == cookieName }?.value ?: return null
        val hash = tokenHash(token)
        val signIn = store.find(hash, idleSince = clock.instant() - idleTimeout)
        if (signIn == null) response.addCookie(cookie(request, "", isSet = false))
        return signIn?.let { found -> hash to found }
    }

    /**
     * Marks [session] as restored from the sign-in with token digest [hash].
     *
     * @since 1.0.0-beta.1
     */
    fun attach(session: HttpSession, hash: String) {
        session.setAttribute(TOKEN_HASH_ATTRIBUTE, hash)
        session.setAttribute(TOUCHED_AT_ATTRIBUTE, clock.instant().toEpochMilli())
    }

    /**
     * Records the request of the remembered sign-in of [session], at most once a minute.
     *
     * @since 1.0.0-beta.1
     */
    fun touch(session: HttpSession) {
        val hash = session.getAttribute(TOKEN_HASH_ATTRIBUTE) as? String ?: return
        val now = clock.instant()
        val touchedAt = session.getAttribute(TOUCHED_AT_ATTRIBUTE) as? Long
        if (touchedAt != null && now.toEpochMilli() - touchedAt < TOUCH_INTERVAL.toMillis()) return

        store.touch(hash, now)
        session.setAttribute(TOUCHED_AT_ATTRIBUTE, now.toEpochMilli())
    }

    /**
     * Returns the token digest of the sign-in remembered by [session], or `null` if it remembers none.
     *
     * @since 1.0.0-beta.1
     */
    fun tokenHashOf(session: HttpSession): String? = session.getAttribute(TOKEN_HASH_ATTRIBUTE) as? String

    /**
     * Forgets the sign-in of the session or the cookie of [request] and clears the cookie on [response].
     *
     * @since 1.0.0-beta.1
     */
    fun forget(request: HttpServletRequest, response: HttpServletResponse) {
        val session = request.getSession(false)
        val hash = session?.let(::tokenHashOf)
            ?: request.cookies?.firstOrNull { cookie -> cookie.name == cookieName }?.value?.let(tokenHash)
        hash?.let(store::delete)
        session?.removeAttribute(TOKEN_HASH_ATTRIBUTE)
        response.addCookie(cookie(request, "", isSet = false))
    }

    /**
     * Handler of a sign-out, including an expired session, that forgets its sign-in.
     *
     * @since 1.0.0-beta.1
     */
    fun logoutHandler(): LogoutHandler = LogoutHandler { request, response, _ -> forget(request, response) }

    /**
     * Filter that signs a request without authentication in from the remembered sign-in of its cookie through
     * [authenticate], which returns `null` for a sign-in the interface cannot restore, and then calls [onRestored]
     * with the new session. An authenticated request records the request of its sign-in.
     *
     * @since 1.0.0-beta.1
     */
    fun restoreFilter(
        authenticate: (StoredCabinetSignIn) -> Authentication?,
        onRestored: (HttpSession, Authentication) -> Unit = { _, _ -> },
    ): OncePerRequestFilter = RestoreFilter(this, authenticate, onRestored)

    private fun cookie(request: HttpServletRequest, value: String, isSet: Boolean): Cookie = Cookie(cookieName, value).also { cookie ->
        cookie.isHttpOnly = true
        cookie.secure = request.isSecure
        cookie.path = request.contextPath.ifEmpty { "/" }
        cookie.setAttribute("SameSite", "Lax")
        if (!isSet) cookie.maxAge = 0
    }
}

/** Filter of [RememberedSignIns.restoreFilter]. */
private class RestoreFilter(
    private val signIns: RememberedSignIns,
    private val authenticate: (StoredCabinetSignIn) -> Authentication?,
    private val onRestored: (HttpSession, Authentication) -> Unit,
) : OncePerRequestFilter() {
    private val repository = HttpSessionSecurityContextRepository()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val current = SecurityContextHolder.getContext().authentication
        if (current != null && current !is AnonymousAuthenticationToken) {
            request.getSession(false)?.let(signIns::touch)
        } else {
            restore(request, response)
        }

        filterChain.doFilter(request, response)
    }

    private fun restore(request: HttpServletRequest, response: HttpServletResponse) {
        val (hash, signIn) = signIns.find(request, response) ?: return
        val authentication = authenticate(signIn)
        if (authentication == null) {
            signIns.forget(request, response)
            return
        }

        // A guest session that already exists gets a new id, so a planted id cannot take the restored sign-in.
        if (request.getSession(false) != null) request.changeSessionId()
        val session = request.getSession(true)
        signIns.attach(session, hash)
        val strategy = SecurityContextHolder.getContextHolderStrategy()
        val context = strategy.createEmptyContext().apply { this.authentication = authentication }
        strategy.context = context
        repository.saveContext(context, request, response)
        onRestored(session, authentication)
    }
}

/** Returns the SHA-256 hex digest of [token]. */
private fun sha256Hex(token: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)))

/** Idle time after which the servlet container ends a session of [environment], as Spring Boot reads it. */
internal fun sessionTimeoutOf(environment: Environment): Duration =
    environment.getProperty(SESSION_TIMEOUT_PROPERTY)?.let { value -> DurationStyle.detectAndParse(value, ChronoUnit.SECONDS) }
        ?: DEFAULT_SESSION_TIMEOUT
