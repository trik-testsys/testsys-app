package tech.testsys.domain.builder.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import java.time.Instant

private fun communityInviteVariants() = listOf(
    communityInviteData {
        code("abcdefghjkmn", HashAlgorithm.Identity)
        expiresAt = Instant.ofEpochSecond(100)
    },
    communityInviteData {
        storedCode(InviteCodeHash(value = "stored", algorithm = HashAlgorithm.Identity))
        expiresAt = Instant.ofEpochSecond(100)
    },
)

class ManagerCommunityInviteBuilderTests :
    DomainEntityBuilderTests<CommunityInvite.Manager, CommunityInviteData, CommunityInviteDataBuilder>(
        ManagerCommunityInviteBuilder(),
        CommunityInviteDataBuilder(),
    ) {
    override fun buildDataWithAllFields() = communityInviteVariants()
}

class DeveloperCommunityInviteBuilderTests :
    DomainEntityBuilderTests<CommunityInvite.Developer, CommunityInviteData, CommunityInviteDataBuilder>(
        DeveloperCommunityInviteBuilder(),
        CommunityInviteDataBuilder(),
    ) {
    override fun buildDataWithAllFields() = communityInviteVariants()
}

class CommunityInviteBuilderTests {

    @Nested
    inner class VariantTests {

        @Test
        fun `should build the manager variant with its kind`() {
            val invite = managerCommunityInvite {
                id = 1
                createdAt = Instant.EPOCH
                data = communityInviteVariants().first()
            }

            assertEquals(CommunityInvite.Kind.Manager, invite.kind)
        }

        @Test
        fun `should build the developer variant with its kind`() {
            val invite = developerCommunityInvite {
                id = 1
                createdAt = Instant.EPOCH
                data = communityInviteVariants().first()
            }

            assertEquals(CommunityInvite.Kind.Developer, invite.kind)
        }
    }

    @Nested
    inner class DataTests {

        @Test
        fun `should hash the raw code with the given algorithm`() {
            val data = communityInviteData {
                code("Abc 123", HashAlgorithm.Identity)
                expiresAt = Instant.ofEpochSecond(100)
            }

            assertEquals(InviteCodeHash(value = "Abc 123", algorithm = HashAlgorithm.Identity), data.codeHash)
        }

        @Test
        fun `should throw IllegalArgumentException if no code is set`() {
            assertThrows(IllegalArgumentException::class.java) {
                communityInviteData { expiresAt = Instant.ofEpochSecond(100) }
            }
        }
    }
}
