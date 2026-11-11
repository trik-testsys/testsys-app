package tech.testsys.web.app.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.mock.env.MockEnvironment
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.AccessTokenHasher
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserId
import tech.testsys.infra.database.api.session.CabinetSignInUserKind
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.util.nextAccessToken
import tech.testsys.web.app.AppFixtures
import tech.testsys.web.app.AppTestConfiguration
import tech.testsys.web.app.security.SecurityConfiguration
import tech.testsys.web.app.security.UserKind
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Import(AppTestConfiguration::class)
@OptIn(InternalOperationsApi::class)
class SupervisorBootstrapTests {
    @Autowired
    private lateinit var supervisors: SupervisorRepository

    @Autowired
    private lateinit var participants: ParticipantRepository

    @Autowired
    private lateinit var observers: ObserverRepository

    @Autowired
    private lateinit var transactions: TransactionOperations

    @Autowired
    private lateinit var accessTokenHasher: AccessTokenHasher

    @Autowired
    private lateinit var fixtures: AppFixtures

    @Autowired
    private lateinit var signIns: CabinetSignInStore

    @Autowired
    private lateinit var configuration: Environment

    private val random = SecureRandom()

    /** Restores the shared supervisor of the test database to the test configuration for other tests. */
    @AfterEach
    fun restoreConfiguredSupervisor() {
        bootstrap().ensure(configuration)
    }

    @Test
    fun `should keep the single supervisor of the test configuration after application startup`() {
        val supervisor = assertNotNull(supervisors.findSingle())

        assertEquals("Test supervisor", supervisor.data.name)
        assertEquals(accessTokenHasher.hash("Supe-rvis-or00-0001"), supervisor.data.accessTokenHash)
    }

    @Test
    fun `should return the supervisor without changes if the configuration matches`() {
        val before = single()

        val again = bootstrap().ensure(configuration)

        assertEquals(before.id, again.id)
        assertEquals(before.version, again.version)
        assertEquals(before.version, single().version)
    }

    @Test
    fun `should set the configured name and access code of the existing supervisor`() {
        val before = single()
        val oldToken = "Supe-rvis-or00-0001"
        val token = newToken()

        val updated = bootstrap().ensure(environment(token, "New name"))

        assertEquals(before.id, updated.id)
        val found = single()
        assertEquals(before.id, found.id)
        assertEquals("New name", found.data.name)
        assertEquals(accessTokenHasher.hash(token), found.data.accessTokenHash)
        assertNull(supervisors.findByAccessToken(oldToken))
        assertEquals(before.id, supervisors.findByAccessToken(token)?.id)
    }

    @Test
    fun `should end the remembered sign-ins of the supervisor and write the end of those as users when the access code changes`() {
        val supervisor = single().id.value
        val user = fixtures.userOf(UserKind.MULTIPLE_ROLE).id.value
        rememberSignInsOf(supervisor, user)

        val lines = logged { bootstrap().ensure(environment(newToken())) }

        assertNull(signIns.find("supervisor-$supervisor", idleSince = Instant.EPOCH))
        assertNull(signIns.find("as-user-$supervisor", idleSince = Instant.EPOCH))
        assertEquals(listOf(Level.INFO to "Session of supervisor $supervisor as user $user ended"), lines)
    }

    @Test
    fun `should end the remembered sign-ins of the supervisor even if the new access code is not stored`() {
        val before = single()
        val supervisor = before.id.value
        rememberSignInsOf(supervisor, fixtures.userOf(UserKind.MULTIPLE_ROLE).id.value)
        val failingSupervisors = object : SupervisorRepository by supervisors {
            override fun update(entity: Supervisor): Supervisor = error("Simulated failure of the update of supervisor $supervisor")
        }
        val bootstrap =
            SupervisorBootstrap(failingSupervisors, participants, observers, transactions, accessTokenHasher, signIns)

        assertFailsWith<IllegalStateException> { bootstrap.ensure(environment(newToken())) }

        assertNull(signIns.find("supervisor-$supervisor", idleSince = Instant.EPOCH))
        assertNull(signIns.find("as-user-$supervisor", idleSince = Instant.EPOCH))
        assertEquals(before.data.accessTokenHash, single().data.accessTokenHash)
    }

    @Test
    fun `should keep the remembered sign-ins of the supervisor when only the name changes`() {
        val supervisor = single().id.value
        signIns.create("name-$supervisor", supervisorSignIn(supervisor), Instant.now())

        bootstrap().ensure(environment("Supe-rvis-or00-0001", "Renamed supervisor"))

        assertNotNull(signIns.find("name-$supervisor", idleSince = Instant.EPOCH))
        signIns.delete("name-$supervisor")
    }

    @Test
    fun `should store the trimmed name`() {
        bootstrap().ensure(environment(newToken(), "  New supervisor  "))

        assertEquals("New supervisor", single().data.name)
    }

    @Test
    fun `should create the supervisor if none is stored`() {
        supervisors.remove(single())
        val token = newToken()

        val created = bootstrap().ensure(environment(token))

        val found = single()
        assertEquals(created.id, found.id)
        assertEquals("Configured supervisor", found.data.name)
        assertEquals(accessTokenHasher.hash(token), found.data.accessTokenHash)
    }

    @ParameterizedTest
    @EnumSource(value = UserKind::class, names = ["PARTICIPANT", "OBSERVER"])
    fun `should reject an access code held by a user of another kind`(kind: UserKind) {
        val before = single()
        val token = newToken()
        fixtures.userOf(kind, rawAccessToken = token)

        assertFailsWith<IllegalStateException> { bootstrap().ensure(environment(token)) }

        assertUnchanged(before)
    }

    @Test
    fun `should reject a missing name`() {
        val before = single()

        assertFailsWith<IllegalStateException> { bootstrap().ensure(environment(newToken(), name = null)) }

        assertUnchanged(before)
    }

    @Test
    fun `should reject a missing access code`() {
        val before = single()
        val environment = MockEnvironment().withProperty("testsys.operation.supervisor.name", "Configured supervisor")

        assertFailsWith<IllegalStateException> { bootstrap().ensure(environment) }

        assertUnchanged(before)
    }

    @Test
    fun `should reject an empty name`() {
        assertRejectName("")
    }

    @Test
    fun `should reject a blank name`() {
        assertRejectName("   ")
    }

    @Test
    fun `should reject a name longer than 512 Unicode characters`() {
        assertRejectName("😀" + "a".repeat(512))
    }

    @Test
    fun `should accept a 512 Unicode character name including a non-BMP character`() {
        val name = "😀" + "a".repeat(511)

        bootstrap().ensure(environment(newToken(), name))

        assertEquals(name, single().data.name)
    }

    @Test
    fun `should reject an access code outside the issued format without revealing it`() {
        val before = single()
        val token = newToken() + "a"

        val error = assertFailsWith<IllegalArgumentException> { bootstrap().ensure(environment(token)) }

        assertFalse(error.message.orEmpty().contains(token))
        assertUnchanged(before)
    }

    @Test
    fun `should create one supervisor on two concurrent first starts with the same configuration`() {
        supervisors.remove(single())
        val environment = environment(newToken())
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val attempts = List(2) {
                executor.submit(
                    Callable {
                        ready.countDown()
                        start.await(10, TimeUnit.SECONDS)
                        bootstrap().ensure(environment)
                    },
                )
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()

            val ids = attempts.map { attempt -> attempt.get(30, TimeUnit.SECONDS).id }
            val supervisor = single()
            assertEquals(listOf(supervisor.id, supervisor.id), ids)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun bootstrap() = SupervisorBootstrap(supervisors, participants, observers, transactions, accessTokenHasher, signIns)

    private fun single(): Supervisor = assertNotNull(supervisors.findSingle())

    private fun supervisorSignIn(supervisor: Long) =
        StoredCabinetSignIn.User(userId = CabinetSignInUserId(supervisor), kind = CabinetSignInUserKind.SUPERVISOR)

    private fun rememberSignInsOf(supervisor: Long, user: Long) {
        val now = Instant.now()
        signIns.create("supervisor-$supervisor", supervisorSignIn(supervisor), now)
        signIns.create(
            "as-user-$supervisor",
            StoredCabinetSignIn.User(
                userId = CabinetSignInUserId(user),
                kind = CabinetSignInUserKind.MULTIPLE_ROLE,
                impersonatedBy = CabinetSignInUserId(supervisor),
            ),
            now,
        )
    }

    private fun logged(action: () -> Unit): List<Pair<Level, String>> {
        val logger = assertIs<Logger>(LoggerFactory.getLogger(SecurityConfiguration::class.java))
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            action()
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list.map { line -> line.level to line.formattedMessage }
    }

    private fun assertRejectName(name: String) {
        val before = single()

        assertFailsWith<IllegalArgumentException> { bootstrap().ensure(environment(newToken(), name)) }

        assertUnchanged(before)
    }

    private fun assertUnchanged(before: Supervisor) {
        val after = single()
        assertEquals(before.id, after.id)
        assertEquals(before.version, after.version)
        assertEquals(before.data, after.data)
    }

    private fun environment(token: String, name: String? = "Configured supervisor") = MockEnvironment()
        .withProperty("testsys.operation.supervisor.access-token", token)
        .apply {
            name?.let { value -> withProperty("testsys.operation.supervisor.name", value) }
        }

    private fun newToken() = random.nextAccessToken()
}
