package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.infra.localization.bundle.SupportedRegion

class ChangelogTests {
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

    @Test
    fun `should read the changelog from the newest version to the oldest`() {
        val changelog = readChangelog("test")

        assertEquals(listOf("1.10.0", "1.2.0", "1.0.0-beta.2"), changelog.map { (version, _) -> version })
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
}
