package tech.testsys.web.app.security

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.session.SessionRegistry
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.PasswordHash
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserId
import tech.testsys.infra.database.api.session.CabinetSignInUserKind
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import tech.testsys.web.app.PostgresTestConfiguration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat

@Import(PostgresTestConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["vaadin.productionMode=true"])
class SecurityConfigurationHttpTests {
    @Value("\${local.server.port}")
    private lateinit var port: String

    @Autowired
    private lateinit var sessionRegistry: SessionRegistry

    @Autowired
    private lateinit var store: CabinetSignInStore

    @Autowired
    private lateinit var supervisors: SupervisorRepository

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @ParameterizedTest
    @ValueSource(strings = ["", "login", "registration", "home", "participant"])
    fun `should let a guest load any page and leave the access check to navigation`(path: String) {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port/$path"))
            .header("Accept", "text/html").GET().build()

        val status = HttpClient.newHttpClient().use { client ->
            client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
        }

        assertEquals(200, status)
    }

    @Test
    fun `should sign an expired session out and send it to the root on its next request`() {
        HttpClient.newHttpClient().use { client ->
            val first = client.send(page("login"), HttpResponse.BodyHandlers.discarding())
            val cookie = first.headers().firstValue("Set-Cookie").orElseThrow().substringBefore(';')
            val sessionId = cookie.substringAfter('=')
            sessionRegistry.registerNewSession(sessionId, CabinetPrincipal(userId = EXPIRED_USER_ID, kind = UserKind.MULTIPLE_ROLE))
            checkNotNull(sessionRegistry.getSessionInformation(sessionId)).expireNow()

            val next = client.send(page("login", cookie), HttpResponse.BodyHandlers.discarding())

            assertEquals(302, next.statusCode())
            assertEquals("/", URI(next.headers().firstValue("Location").orElseThrow()).path)
        }
    }

    @Test
    fun `should sign a new session in from the remembered sign-in of its cookie`() {
        val supervisor = checkNotNull(supervisors.findSingle()).id.value
        val hash = sha256Hex(RESTORED_TOKEN)
        store.create(
            hash,
            StoredCabinetSignIn.User(userId = CabinetSignInUserId(supervisor), kind = CabinetSignInUserKind.SUPERVISOR),
            Instant.now(),
        )
        try {
            val principal = restoredPrincipal(RESTORED_TOKEN)

            assertEquals(CabinetPrincipal(userId = supervisor, kind = UserKind.SUPERVISOR), principal)
        } finally {
            store.delete(hash)
        }
    }

    @Test
    fun `should restore the sign-in of a supervisor as a user and write it at INFO`() {
        val supervisor = checkNotNull(supervisors.findSingle()).id.value
        val user = multipleRoleUsers.save(
            multipleRoleUserData {
                passwordHash = PasswordHash("hash")
                name = "Restored user"
                email = "restored-user@example.com"
            },
        )
        val signIn = StoredCabinetSignIn.User(
            userId = CabinetSignInUserId(user.id.value),
            kind = CabinetSignInUserKind.MULTIPLE_ROLE,
            impersonatedBy = CabinetSignInUserId(supervisor),
        )
        store.create(sha256Hex(IMPERSONATED_TOKEN), signIn, Instant.now())
        val logger = assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(SecurityConfiguration::class.java))
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            val principal = restoredPrincipal(IMPERSONATED_TOKEN)

            assertEquals(CabinetPrincipal(userId = user.id.value, kind = UserKind.MULTIPLE_ROLE, impersonatedBy = supervisor), principal)
            val lines = appender.list.map { line -> line.level to line.formattedMessage }
            assertEquals(listOf(Level.INFO to "Session of supervisor $supervisor as user ${user.id.value} restored"), lines)
        } finally {
            logger.detachAppender(appender)
            multipleRoleUsers.removeById(user.id)
        }
    }

    @Test
    fun `should clear the cookie of a sign-in that is not remembered`() {
        val response = HttpClient.newHttpClient().use { client ->
            client.send(page("login", "$SIGN_IN_COOKIE=unknown"), HttpResponse.BodyHandlers.discarding())
        }

        val cleared = response.headers().allValues("Set-Cookie").first { cookie -> cookie.startsWith("$SIGN_IN_COOKIE=") }
        // Tomcat writes a zero Max-Age as an expiry in the past.
        assertTrue(cleared.startsWith("$SIGN_IN_COOKIE=;") && cleared.contains("Expires=Thu, 01 Jan 1970"), cleared)
    }

    /** Opens a page with the remembered sign-in cookie of [token] and returns the principal of the restored session. */
    private fun restoredPrincipal(token: String): Any? {
        val response = HttpClient.newHttpClient().use { client ->
            client.send(page("home", "$SIGN_IN_COOKIE=$token"), HttpResponse.BodyHandlers.discarding())
        }
        val sessionId = response.headers().allValues("Set-Cookie")
            .first { cookie -> cookie.startsWith("TESTSYS_APP_SESSION=") }
            .substringBefore(';').substringAfter('=')
        return sessionRegistry.getSessionInformation(sessionId)?.principal
    }

    private fun sha256Hex(token: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)))

    private fun page(path: String, cookie: String? = null): HttpRequest = HttpRequest.newBuilder(URI("http://localhost:$port/$path"))
        .header("Accept", "text/html")
        .apply { cookie?.let { value -> header("Cookie", value) } }
        .GET()
        .build()

    private companion object {
        const val EXPIRED_USER_ID = 42L
        const val SIGN_IN_COOKIE = "TESTSYS_APP_SIGN_IN"
        const val RESTORED_TOKEN = "restored-sign-in-token"
        const val IMPERSONATED_TOKEN = "impersonated-sign-in-token"
    }
}
