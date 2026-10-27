package tech.testsys.domain.builder.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.AccessTokenHasher
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.SingleRoleUserData
import tech.testsys.domain.model.user.Supervisor
import java.time.Instant

class UserDataBuilderTests {

    enum class UserKind { PARTICIPANT, OBSERVER, SUPERVISOR }

    private val hasher = mockk<AccessTokenHasher>()

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should hash raw access tokens once for every user kind`(kind: UserKind) {
        every { hasher.hash("raw-token") } returns AccessTokenHash(
            value = "hashed-token",
            algorithm = HashAlgorithm.HmacSha256,
        )
        val builder = dataBuilder(kind)

        builder.accessToken("raw-token", hasher)
        val data = builder.build()

        assertEquals(AccessTokenHash(value = "hashed-token", algorithm = HashAlgorithm.HmacSha256), data.accessTokenHash)
        verify(exactly = 1) { hasher.hash("raw-token") }
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should restore the stored hash without hashing for every user kind`(kind: UserKind) {
        every { hasher.hash(any()) } throws IllegalStateException("Stored tokens must not be hashed")
        val builder = dataBuilder(kind)

        builder.storedAccessToken(AccessTokenHash(value = "stored-token", algorithm = HashAlgorithm.HmacSha256))
        val data = builder.build()

        assertEquals(AccessTokenHash(value = "stored-token", algorithm = HashAlgorithm.HmacSha256), data.accessTokenHash)
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should preserve the computed hash when building again`(kind: UserKind) {
        every { hasher.hash("raw-token") } returns AccessTokenHash(
            value = "hashed-token",
            algorithm = HashAlgorithm.HmacSha256,
        )
        val builder = dataBuilder(kind).apply { accessToken("raw-token", hasher) }
        builder.build()

        val data = builder.build()

        assertEquals(AccessTokenHash(value = "hashed-token", algorithm = HashAlgorithm.HmacSha256), data.accessTokenHash)
        verify(exactly = 1) { hasher.hash("raw-token") }
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should preserve the stored hash when withData changes the name`(kind: UserKind) {
        every { hasher.hash(any()) } throws IllegalStateException("Stored tokens must not be hashed")
        val original = storedUser(kind)

        val updated = changeName(original)

        assertEquals(AccessTokenHash(value = "stored-token", algorithm = HashAlgorithm.HmacSha256), userData(updated).accessTokenHash)
        assertEquals("Updated", userData(updated).name)
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.version, updated.version)
    }

    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should hash the replacement token and preserve entity fields with withData`(kind: UserKind) {
        every { hasher.hash("new-token") } returns AccessTokenHash(
            value = "new-hash",
            algorithm = HashAlgorithm.HmacSha256,
        )
        val original = storedUser(kind)

        val updated = replaceToken(original)

        assertEquals(AccessTokenHash(value = "new-hash", algorithm = HashAlgorithm.HmacSha256), userData(updated).accessTokenHash)
        assertEquals(userData(original).name, userData(updated).name)
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.version, updated.version)
        verify(exactly = 1) { hasher.hash("new-token") }
    }

    private fun dataBuilder(kind: UserKind): UserDataBuilder<out SingleRoleUserData> = when (kind) {
        UserKind.PARTICIPANT -> ParticipantDataBuilder().apply {
            competition(10)
            name = "User"
        }
        UserKind.OBSERVER -> ObserverDataBuilder().apply {
            community(20)
            name = "User"
        }
        UserKind.SUPERVISOR -> SupervisorDataBuilder().apply { name = "User" }
    }

    private fun storedUser(kind: UserKind): SingleRoleUser = when (kind) {
        UserKind.PARTICIPANT -> participant {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ParticipantDataBuilder().apply {
                competition(10)
                name = "User"
                storedAccessToken(AccessTokenHash(value = "stored-token", algorithm = HashAlgorithm.HmacSha256))
            }.build()
        }
        UserKind.OBSERVER -> observer {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ObserverDataBuilder().apply {
                community(20)
                name = "User"
                storedAccessToken(AccessTokenHash(value = "stored-token", algorithm = HashAlgorithm.HmacSha256))
            }.build()
        }
        UserKind.SUPERVISOR -> supervisor {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = SupervisorDataBuilder().apply {
                name = "User"
                storedAccessToken(AccessTokenHash(value = "stored-token", algorithm = HashAlgorithm.HmacSha256))
            }.build()
        }
    }

    private fun userData(user: SingleRoleUser): SingleRoleUserData = when (user) {
        is Participant -> user.data
        is Observer -> user.data
        is Supervisor -> user.data
    }

    private fun changeName(user: SingleRoleUser): SingleRoleUser = when (user) {
        is Participant -> user.withData { name = "Updated" }
        is Observer -> user.withData { name = "Updated" }
        is Supervisor -> user.withData { name = "Updated" }
    }

    private fun replaceToken(user: SingleRoleUser): SingleRoleUser = when (user) {
        is Participant -> user.withData { accessToken("new-token", hasher) }
        is Observer -> user.withData { accessToken("new-token", hasher) }
        is Supervisor -> user.withData { accessToken("new-token", hasher) }
    }
}
