package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.PasswordHash
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.Instant

class ChangelogTests {
    @Nested
    inner class VersionOrderTests {
        @Test
        fun `should order versions by SemVer precedence`() {
            val versions = listOf(
                "1.0.0-beta.2", "0.9.0", "1.0.0-1", "1.0.0", "1.0.0-alpha",
                "1.0.0-beta.10", "1.0.0-alpha.1", "1.0.0-beta", "1.10.0", "1.2.0",
            )

            val ordered = versions.sortedWith(versionOrder.reversed())

            val expected = listOf(
                "1.10.0", "1.2.0", "1.0.0", "1.0.0-beta.10", "1.0.0-beta.2",
                "1.0.0-beta", "1.0.0-alpha.1", "1.0.0-alpha", "1.0.0-1", "0.9.0",
            )
            assertEquals(expected, ordered)
        }

        @Test
        fun `should reject a version name outside SemVer`() {
            assertThrows<IllegalStateException> { versionOrder.compare("1.0", "1.0.0") }
        }
    }

    @Nested
    inner class ReadChangelogTests {
        @Test
        fun `should read the changelog from the newest version to the oldest`() {
            val changelog = readChangelog("test")

            assertEquals(listOf("1.10.0", "1.2.0", "1.0.0-beta.2"), changelog.map { (version, _) -> version })
        }

        @Test
        fun `should join the common part before the role parts of a version`() {
            val changelog = readChangelog("test")

            val expected = "- Версия 1.10.0.\n\n### Администратор\n\n- Изменение Администратора в версии 1.10.0.\n"
            assertEquals("1.10.0" to expected, changelog.first())
        }

        @Test
        fun `should skip a version without parts the user sees`() {
            val changelog = readChangelog("test", setOf("common"))

            assertEquals(listOf("1.10.0" to "- Версия 1.10.0.\n", "1.0.0-beta.2" to "- Версия 1.0.0-beta.2.\n"), changelog)
        }

        @Test
        fun `should reject a changelog file of an unknown part`() {
            assertThrows<IllegalStateException> { readChangelog("test-unknown-part") }
        }

        @Test
        fun `should reject a changelog directory not named by a SemVer version`() {
            assertThrows<IllegalStateException> { readChangelog("test-bad-version") }
        }

        @ParameterizedTest
        @EnumSource(SupportedRegion::class)
        fun `should have the changelog of every ru-RU version for region`(region: SupportedRegion) {
            val changelog = readChangelog(region.toULocale().toLanguageTag())

            val versions = changelog.map { (version, _) -> version }
            assertEquals(readChangelog("ru-RU").map { (version, _) -> version }, versions)
            assertTrue(versions.isNotEmpty())
            assertTrue(changelog.all { (_, content) -> content.isNotBlank() })
        }

        @ParameterizedTest
        @MethodSource("tech.testsys.web.app.view.ChangelogTests#regionParts")
        fun `should have every ru-RU version of a part for region`(region: SupportedRegion, part: String) {
            val versions = readChangelog(region.toULocale().toLanguageTag(), setOf(part)).map { (version, _) -> version }

            assertEquals(readChangelog("ru-RU", setOf(part)).map { (version, _) -> version }, versions)
        }
    }

    @Nested
    inner class ChangelogPartsOfTests {
        @Test
        fun `should show a user without roles only the common part`() {
            val parts = changelogPartsOf(multipleRoleUserWith {})

            assertEquals(setOf("common"), parts)
        }

        @ParameterizedTest
        @MethodSource("tech.testsys.web.app.view.ChangelogTests#singleRoleUsers")
        fun `should show a user with one role the common part and the part of the role`(user: MultipleRoleUser, part: String) {
            val parts = changelogPartsOf(user)

            assertEquals(setOf("common", part), parts)
        }

        @Test
        fun `should show a user the common part and the parts of their roles`() {
            val user = multipleRoleUserWith {
                roles {
                    developer { data = developerData {} }
                    student { data = studentData {} }
                }
            }

            val parts = changelogPartsOf(user)

            assertEquals(setOf("common", "developer", "student"), parts)
        }

        @Test
        fun `should show a supervisor the parts of all roles`() {
            val parts = changelogPartsOf(multipleRoleUserWith { roles { supervisor {} } })

            val expected = setOf(
                "common", "administrator", "manager", "student", "developer", "judge", "supervisor", "participant", "observer",
            )
            assertEquals(expected, parts)
        }

        @Test
        fun `should show a participant the common part and the participant part`() {
            val user = participant {
                id = USER_ID
                createdAt = Instant.EPOCH
                data = participantData {
                    competition(1)
                    storedAccessToken(AccessTokenHash("participant-hash", HashAlgorithm.HmacSha256))
                    name = "Участник"
                }
            }

            val parts = changelogPartsOf(user)

            assertEquals(setOf("common", "participant"), parts)
        }

        @Test
        fun `should show an observer the common part and the observer part`() {
            val user = observer {
                id = USER_ID
                createdAt = Instant.EPOCH
                data = observerData {
                    community(1)
                    storedAccessToken(AccessTokenHash("observer-hash", HashAlgorithm.HmacSha256))
                    name = "Наблюдатель"
                }
            }

            val parts = changelogPartsOf(user)

            assertEquals(setOf("common", "observer"), parts)
        }
    }

    companion object {
        private const val USER_ID = 1L

        @JvmStatic
        fun regionParts(): List<Arguments> =
            SupportedRegion.entries.flatMap { region -> CHANGELOG_PARTS.map { part -> Arguments.of(region, part) } }

        @JvmStatic
        fun singleRoleUsers(): List<Arguments> = listOf(
            Arguments.of(multipleRoleUserWith { roles { administrator {} } }, "administrator"),
            Arguments.of(multipleRoleUserWith { roles { manager { data = managerData {} } } }, "manager"),
            Arguments.of(multipleRoleUserWith { roles { judge { data = judgeData {} } } }, "judge"),
        )

        private fun multipleRoleUserWith(configure: MultipleRoleUserDataBuilder.() -> Unit) = multipleRoleUser {
            id = USER_ID
            createdAt = Instant.EPOCH
            data = multipleRoleUserData {
                passwordHash = PasswordHash("password-hash")
                name = "Пользователь"
                email = "user@example.com"
                configure()
            }
        }
    }
}
