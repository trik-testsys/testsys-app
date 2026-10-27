package tech.testsys.web.app.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.mock.env.MockEnvironment
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.contract.PasswordHasher
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.PasswordHash
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.web.app.AppTestConfiguration
import java.time.Clock
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.random.RandomGenerator
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Import(AppTestConfiguration::class)
class PublicCommunityBootstrapTests {
    @Autowired
    private lateinit var communities: CommunityRepository

    @Autowired
    private lateinit var users: MultipleRoleUserRepository

    @Autowired
    private lateinit var managerInvites: ManagerCommunityInviteRepository

    @Autowired
    private lateinit var developerInvites: DeveloperCommunityInviteRepository

    @Autowired
    private lateinit var transactions: TransactionOperations

    @Autowired
    private lateinit var inviteConfig: CommunityInviteConfig

    @Autowired
    private lateinit var clock: Clock

    @Autowired
    private lateinit var random: RandomGenerator

    @Autowired
    private lateinit var passwordHasher: PasswordHasher

    @Test
    fun `should create the configured public community on application startup`() {
        val community = assertNotNull(communities.findById(CommunityId(1)))
        val owner = assertNotNull(users.findByEmail(PublicCommunityOwner.EMAIL))

        assertEquals(CommunityId(1), community.id)
        assertEquals(owner.id, community.data.owner.id)
        assertEquals("Public test community", community.data.name)
        assertEquals(listOf(community.id), owner.data.roles.filterIsInstance<Administrator>().single().memberOf.ids)
        assertNotNull(managerInvites.findById(community.data.managerInvite.id))
        assertNotNull(developerInvites.findById(community.data.developerInvite.id))
    }

    @Test
    fun `should reuse a fully consistent community without creating records`() {
        val id = newId()
        val email = newEmail()
        val bootstrap = bootstrap(email)
        val environment = environment(id)
        bootstrap.ensure(environment)
        val first = assertNotNull(communities.findById(id))
        val owner = assertNotNull(users.findByEmail(email))

        assertEquals(id, bootstrap.ensure(environment))

        val again = assertNotNull(communities.findById(id))
        assertEquals(first.data.managerInvite.id, again.data.managerInvite.id)
        assertEquals(first.data.developerInvite.id, again.data.developerInvite.id)
        assertEquals(listOf(id), communities.findByOwner(owner.id).map { it.id })
        assertEquals(owner.id, users.findByEmail(email)?.id)
    }

    @Test
    fun `should reject an occupied id belonging to another owner`() {
        val email = newEmail()

        assertFailsWith<IllegalStateException> { bootstrap(email).ensure(environment(CommunityId(1))) }

        assertNull(users.findByEmail(email))
    }

    @Test
    fun `should reject a partial technical owner`() {
        val email = newEmail()
        users.save(
            multipleRoleUserData {
                passwordHash = PasswordHash("hash-${UUID.randomUUID()}")
                name = "Partial"
                this.email = email
            },
        )
        val id = newId()

        assertFailsWith<IllegalStateException> { bootstrap(email).ensure(environment(id)) }

        assertNull(communities.findById(id))
    }

    @Test
    fun `should reject a missing name on first creation`() {
        assertRejectName(null)
    }

    @Test
    fun `should reject an empty name on first creation`() {
        assertRejectName("")
    }

    @Test
    fun `should reject a blank name on first creation`() {
        assertRejectName("   ")
    }

    @Test
    fun `should reject a name longer than 255 Unicode characters on first creation`() {
        assertRejectName("😀" + "a".repeat(255))
    }

    @Test
    fun `should accept a 255 Unicode character name including a non-BMP character`() {
        val id = newId()
        val email = newEmail()
        val name = "😀" + "a".repeat(254)

        assertEquals(id, bootstrap(email).ensure(environment(id, name)))

        assertEquals(name, assertNotNull(communities.findById(id)).data.name)
        assertNotNull(users.findByEmail(email))
    }

    @Test
    fun `should reject missing administrator membership on restart`() {
        val id = newId()
        val email = newEmail()
        val bootstrap = bootstrap(email)
        val environment = environment(id)
        bootstrap.ensure(environment)
        val owner = assertNotNull(users.findByEmail(email))
        users.removeCommunityMembership(owner.id, id, CommunityRole.Administrator)

        assertFailsWith<IllegalStateException> { bootstrap.ensure(environment) }
    }

    @Test
    fun `should accept two concurrent first starts with one community`() {
        val id = newId()
        val email = newEmail()
        val environment = environment(id)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val attempts = List(2) {
                executor.submit(
                    Callable {
                        ready.countDown()
                        start.await(10, TimeUnit.SECONDS)
                        bootstrap(email).ensure(environment)
                    },
                )
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()

            assertEquals(listOf(id, id), attempts.map { it.get(30, TimeUnit.SECONDS) })
            val owner = assertNotNull(users.findByEmail(email))
            assertEquals(listOf(id), communities.findByOwner(owner.id).map { it.id })
        } finally {
            executor.shutdownNow()
        }
    }

    private fun bootstrap(email: String) = PublicCommunityBootstrap(
        communities, users, managerInvites, developerInvites, transactions, inviteConfig, clock, random, passwordHasher, email,
    )

    private fun assertRejectName(name: String?) {
        val id = newId()
        val email = newEmail()

        assertFailsWith<IllegalArgumentException> { bootstrap(email).ensure(environment(id, name)) }

        assertNull(communities.findById(id))
        assertNull(users.findByEmail(email))
    }

    private fun environment(id: CommunityId, name: String? = "Test public community") = MockEnvironment()
        .withProperty("testsys.operation.community.public-community-id", id.value.toString())
        .apply {
            name?.let { value -> withProperty("testsys.operation.community.public-community-name", value) }
        }

    private fun newId() = CommunityId(8_000_000_000_000_000_000L + (UUID.randomUUID().leastSignificantBits and 0xFFFFFFFFL))

    private fun newEmail() = "technical-${UUID.randomUUID()}@testsys.invalid"
}
