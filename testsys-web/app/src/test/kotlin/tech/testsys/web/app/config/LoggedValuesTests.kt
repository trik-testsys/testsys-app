package tech.testsys.web.app.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.group.CommunityInviteDataBuilder
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.task.FileContent
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.web.app.service.manager.IssuedParticipantVo
import tech.testsys.web.app.service.manager.ParticipantVo
import java.io.ByteArrayInputStream
import java.lang.reflect.Method
import java.time.Instant

class LoggedValuesTests {
    @Nested
    inner class ArgumentsTests {
        @ParameterizedTest
        @ValueSource(strings = ["accessToken", "confirmationCode", "inviteCode", "rawAccessToken", "password", "secret"])
        fun `should mask an argument whose parameter name is sensitive`(name: String) {
            val rendered = LoggedValues.arguments(method(name), arrayOf<Any?>(SECRET_VALUE, 5))

            assertEquals("$name=***, page=5", rendered)
        }

        @Test
        fun `should write other arguments with their parameter names`() {
            val rendered = LoggedValues.arguments(method("requestRegistration"), arrayOf("user@testsys.tech"))

            assertEquals("email=user@testsys.tech", rendered)
        }

        @Test
        fun `should mask the code of a method with a value class parameter`() {
            val joinClass = Calls::class.java.methods.single { method -> method.name.startsWith("joinClass") }

            val rendered = LoggedValues.arguments(joinClass, arrayOf<Any?>(7L, SECRET_VALUE))

            assertFalse(SECRET_VALUE in rendered, rendered)
            assertTrue("7" in rendered, rendered)
        }

        @Test
        fun `should mask every string argument when parameter names are unavailable`() {
            val concat = String::class.java.getMethod("concat", String::class.java)

            val rendered = LoggedValues.arguments(concat, arrayOf<Any?>(SECRET_VALUE))

            assertEquals("***", rendered)
        }

        private fun method(name: String): Method = Calls::class.java.methods.single { method -> method.name == name }
    }

    @Nested
    inner class ResultTests {
        @Test
        fun `should mask a string result`() {
            assertEquals("***", LoggedValues.result(SECRET_VALUE))
        }

        @Test
        fun `should write other results by the value rules`() {
            assertEquals("[1, 2]", LoggedValues.result(listOf(1, 2)))
        }

        @Test
        fun `should mask the issued access code of created participants`() {
            val participant = ParticipantVo(
                id = SingleRoleUserId(31),
                createdAt = Instant.EPOCH,
                accessTokenHash = AccessTokenHash(SECRET_VALUE, HashAlgorithm.Identity),
                name = "st31",
                competition = CompetitionId(21),
            )

            val rendered = LoggedValues.result(listOf(IssuedParticipantVo(participant = participant, accessToken = SECRET_VALUE)))

            assertFalse(SECRET_VALUE in rendered, rendered)
            assertTrue("accessToken=***" in rendered, rendered)
        }
    }

    @Nested
    inner class ValueTests {
        @Test
        fun `should mask the access code hash inside a data class`() {
            val data = participantData {
                competition(2001)
                accessToken(SECRET_VALUE, algorithm = HashAlgorithm.Identity)
                name = "Участник Пётр"
            }

            val rendered = LoggedValues.value(data)

            assertFalse(SECRET_VALUE in rendered, rendered)
            assertTrue("accessTokenHash=***" in rendered, rendered)
            assertTrue("name=Участник Пётр" in rendered, rendered)
        }

        @Test
        fun `should mask the invite code hash inside a data class`() {
            val data = CommunityInviteDataBuilder().apply {
                code(SECRET_VALUE, algorithm = HashAlgorithm.Identity)
                expiresAt = Instant.parse("2026-03-14T09:30:00Z")
            }.build()

            val rendered = LoggedValues.value(data)

            assertEquals("CommunityInviteData(codeHash=***, expiresAt=2026-03-14T09:30:00Z)", rendered)
        }

        @Test
        fun `should mask a plain code field inside a data class`() {
            val rendered = LoggedValues.value(Confirmation(email = "user@testsys.tech", confirmationCode = SECRET_VALUE))

            assertEquals("Confirmation(email=user@testsys.tech, confirmationCode=***)", rendered)
        }

        @Test
        fun `should mask the access code and invite code hashes`() {
            assertEquals("***", LoggedValues.value(AccessTokenHash(SECRET_VALUE, HashAlgorithm.Identity)))
            assertEquals("***", LoggedValues.value(InviteCodeHash(SECRET_VALUE, HashAlgorithm.Identity)))
        }

        @Test
        fun `should write only the name and size of an inline file`() {
            val rendered = LoggedValues.value(FileData("solution.qrs", ByteArray(3)))

            assertEquals("FileData(name=solution.qrs, size=3)", rendered)
        }

        @Test
        fun `should write only the name and storage kind of a stored file`() {
            val file = FileData("statement.pdf", FileContent.Stored(StoredBlobRef("b1"), FileStorageKind.Statement))

            assertEquals("FileData(name=statement.pdf, stored=Statement)", LoggedValues.value(file))
        }

        @Test
        fun `should write only the size of bytes and the type of a stream`() {
            assertEquals("byte[4]", LoggedValues.value(ByteArray(4)))
            assertEquals("InputStream", LoggedValues.value(ByteArrayInputStream(ByteArray(4))))
        }

        @Test
        fun `should write an entity as its class and id`() {
            val entity = participant {
                id = 1002
                createdAt = Instant.parse("2026-03-14T09:30:00Z")
                data = participantData {
                    competition(2001)
                    accessToken(SECRET_VALUE, algorithm = HashAlgorithm.Identity)
                    name = "Участник Пётр"
                }
            }

            assertEquals("Participant(id=1002)", LoggedValues.value(entity))
        }

        @Test
        fun `should shorten a long string`() {
            val rendered = LoggedValues.value("a".repeat(600))

            assertEquals("a".repeat(500) + "…", rendered)
        }

        @Test
        fun `should write the first ten items and the size of a long collection`() {
            val rendered = LoggedValues.value((1..12).toList())

            assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, … (12 total)]", rendered)
        }

        @Test
        fun `should apply the rules to the items of a map`() {
            val rendered = LoggedValues.value(mapOf(TaskId(1) to ByteArray(2)))

            assertEquals("[TaskId(value=1)=byte[2]]", rendered)
        }
    }

    interface Calls {
        fun accessToken(accessToken: String, page: Int)

        fun confirmationCode(confirmationCode: String, page: Int)

        fun inviteCode(inviteCode: String, page: Int)

        fun rawAccessToken(rawAccessToken: String, page: Int)

        fun password(password: String, page: Int)

        fun secret(secret: String, page: Int)

        fun requestRegistration(email: String)

        fun joinClass(taskId: TaskId, inviteCode: String)
    }

    data class Confirmation(val email: String, val confirmationCode: String)

    private companion object {
        const val SECRET_VALUE = "K7mQ2xP9"
    }
}
