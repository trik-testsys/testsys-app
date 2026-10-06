package tech.testsys.domain.builder.user

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserData
import tech.testsys.domain.model.user.hashAccessToken
import java.time.Instant

class UserDataBuilderTests {

    enum class UserKind { PARTICIPANT, OBSERVER, SUPERVISOR, MULTIPLE_ROLE }

    @BeforeEach
    fun mockHashing() {
        mockkStatic(::hashAccessToken)
    }

    @AfterEach
    fun unmockHashing() {
        unmockkStatic(::hashAccessToken)
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should hash raw access tokens once for every user kind`(kind: UserKind) {
        every { hashAccessToken("raw-token", algorithm = HashAlgorithm.Identity) } returns AccessTokenHash(
            value = "hashed-token",
            algorithm = HashAlgorithm.Identity,
        )
        val builder = dataBuilder(kind)

        builder.accessToken("raw-token", algorithm = HashAlgorithm.Identity)
        val data = builder.build()

        assertEquals("hashed-token", data.accessToken)
        assertEquals(HashAlgorithm.Identity, data.accessTokenHashAlgorithm)
        verify(exactly = 1) { hashAccessToken("raw-token", algorithm = HashAlgorithm.Identity) }
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should restore the stored pair without hashing for every user kind`(kind: UserKind) {
        every { hashAccessToken(any(), any()) } throws IllegalStateException("Stored tokens must not be hashed")
        val builder = dataBuilder(kind)

        builder.storedAccessToken(value = "stored-token", algorithm = HashAlgorithm.Identity)
        val data = builder.build()

        assertEquals("stored-token", data.accessToken)
        assertEquals(HashAlgorithm.Identity, data.accessTokenHashAlgorithm)
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should preserve the computed pair when building again`(kind: UserKind) {
        every { hashAccessToken("raw-token", algorithm = HashAlgorithm.Identity) } returns AccessTokenHash(
            value = "hashed-token",
            algorithm = HashAlgorithm.Identity,
        )
        val builder = dataBuilder(kind).apply { accessToken("raw-token", algorithm = HashAlgorithm.Identity) }
        builder.build()

        val data = builder.build()

        assertEquals("hashed-token", data.accessToken)
        assertEquals(HashAlgorithm.Identity, data.accessTokenHashAlgorithm)
        verify(exactly = 1) { hashAccessToken("raw-token", algorithm = HashAlgorithm.Identity) }
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should preserve the stored pair when withData changes the name`(kind: UserKind) {
        every { hashAccessToken(any(), any()) } throws IllegalStateException("Stored tokens must not be hashed")
        val original = storedUser(kind)

        val updated = changeName(original)

        assertEquals("stored-token", userData(updated).accessToken)
        assertEquals(HashAlgorithm.Identity, userData(updated).accessTokenHashAlgorithm)
        assertEquals("Updated", userData(updated).name)
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.version, updated.version)
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should hash the replacement token and preserve entity fields with withData`(kind: UserKind) {
        every { hashAccessToken("new-token", algorithm = HashAlgorithm.Identity) } returns AccessTokenHash(
            value = "new-hash",
            algorithm = HashAlgorithm.Identity,
        )
        val original = storedUser(kind)

        val updated = replaceToken(original)

        assertEquals("new-hash", userData(updated).accessToken)
        assertEquals(HashAlgorithm.Identity, userData(updated).accessTokenHashAlgorithm)
        assertEquals(userData(original).name, userData(updated).name)
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.version, updated.version)
        verify(exactly = 1) { hashAccessToken("new-token", algorithm = HashAlgorithm.Identity) }
    }

    private fun dataBuilder(kind: UserKind): UserDataBuilder<out UserData> = when (kind) {
        UserKind.PARTICIPANT -> ParticipantDataBuilder().apply {
            competition(10)
            name = "User"
        }
        UserKind.OBSERVER -> ObserverDataBuilder().apply {
            community(20)
            name = "User"
        }
        UserKind.SUPERVISOR -> SupervisorDataBuilder().apply { name = "User" }
        UserKind.MULTIPLE_ROLE -> MultipleRoleUserDataBuilder().apply {
            name = "User"
            email = "user@example.com"
        }
    }

    private fun storedUser(kind: UserKind): User<*> = when (kind) {
        UserKind.PARTICIPANT -> participant {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ParticipantDataBuilder().apply {
                competition(10)
                name = "User"
                storedAccessToken(value = "stored-token", algorithm = HashAlgorithm.Identity)
            }.build()
        }
        UserKind.OBSERVER -> observer {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ObserverDataBuilder().apply {
                community(20)
                name = "User"
                storedAccessToken(value = "stored-token", algorithm = HashAlgorithm.Identity)
            }.build()
        }
        UserKind.SUPERVISOR -> supervisor {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = SupervisorDataBuilder().apply {
                name = "User"
                storedAccessToken(value = "stored-token", algorithm = HashAlgorithm.Identity)
            }.build()
        }
        UserKind.MULTIPLE_ROLE -> multipleRoleUser {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = MultipleRoleUserDataBuilder().apply {
                name = "User"
                email = "user@example.com"
                storedAccessToken(value = "stored-token", algorithm = HashAlgorithm.Identity)
            }.build()
        }
    }

    private fun userData(user: User<*>): UserData = when (user) {
        is Participant -> user.data
        is Observer -> user.data
        is Supervisor -> user.data
        is MultipleRoleUser -> user.data
    }

    private fun changeName(user: User<*>): User<*> = when (user) {
        is Participant -> user.withData { name = "Updated" }
        is Observer -> user.withData { name = "Updated" }
        is Supervisor -> user.withData { name = "Updated" }
        is MultipleRoleUser -> user.withData { name = "Updated" }
    }

    private fun replaceToken(user: User<*>): User<*> = when (user) {
        is Participant -> user.withData { accessToken("new-token", algorithm = HashAlgorithm.Identity) }
        is Observer -> user.withData { accessToken("new-token", algorithm = HashAlgorithm.Identity) }
        is Supervisor -> user.withData { accessToken("new-token", algorithm = HashAlgorithm.Identity) }
        is MultipleRoleUser -> user.withData { accessToken("new-token", algorithm = HashAlgorithm.Identity) }
    }
}
