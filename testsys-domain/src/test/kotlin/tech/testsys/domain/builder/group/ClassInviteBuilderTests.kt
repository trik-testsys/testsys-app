package tech.testsys.domain.builder.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import java.time.Instant

class ClassInviteBuilderTests : DomainEntityBuilderTests<ClassInvite, ClassInviteData, ClassInviteDataBuilder>(
    ClassInviteBuilder(),
    ClassInviteDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        classInviteData {
            code("abcdefghjkmn", HashAlgorithm.Identity)
            expiresAt = Instant.ofEpochSecond(100)
        },
        classInviteData {
            storedCode(InviteCodeHash(value = "stored", algorithm = HashAlgorithm.Identity))
            expiresAt = Instant.ofEpochSecond(100)
        },
    )

    @Test
    fun `should hash the raw code with the given algorithm`() {
        val data = classInviteData {
            code("Abc 123", HashAlgorithm.Identity)
            expiresAt = Instant.ofEpochSecond(100)
        }

        assertEquals(InviteCodeHash(value = "Abc 123", algorithm = HashAlgorithm.Identity), data.codeHash)
    }

    @Test
    fun `should keep the stored code unchanged`() {
        val stored = InviteCodeHash(value = "stored", algorithm = HashAlgorithm.Identity)

        val data = classInviteData {
            storedCode(stored)
            expiresAt = Instant.ofEpochSecond(100)
        }

        assertEquals(stored, data.codeHash)
    }

    @Test
    fun `should throw IllegalArgumentException if no code is set`() {
        assertThrows(IllegalArgumentException::class.java) {
            classInviteData {
                expiresAt = Instant.ofEpochSecond(100)
            }
        }
    }
}
