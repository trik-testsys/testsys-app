package tech.testsys.infra.database.api.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.DatabaseIntegrationTests

class DatabaseServiceTrikStudioVersionsTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var service: ServiceTrikStudioVersions

    @Nested
    inner class FindAllTests {

        @Test
        fun `should return every registered version ordered by tag`() {
            val second = fixtures.trikStudioVersion(tag = "trik-studio:b", version = "1.0")
            val first = fixtures.trikStudioVersion(tag = "trik-studio:a", version = "2.0")

            val result = service.findAll()

            assertEquals(listOf(first, second), result)
        }

        @Test
        fun `should return an empty list if no version is registered`() {
            assertEquals(emptyList<TrikStudioVersion>(), service.findAll())
        }
    }

    @Nested
    inner class RegisterTests {

        @Test
        fun `should store the version with values trimmed of surrounding whitespace`() {
            val expected = TrikStudioVersion(tag = "trikset/trik-studio:2026.2", version = "2026.2")

            val result = service.register(tag = "  trikset/trik-studio:2026.2\t", version = " 2026.2 ")

            assertEquals(TrikStudioVersionRegistration.Registered(expected), result)
            assertEquals(listOf(expected), service.findAll())
        }

        @Test
        fun `should store values at the length limits counted in code points`() {
            val expected = TrikStudioVersion(tag = "😀".repeat(512), version = "😀".repeat(255))

            val result = service.register(tag = expected.tag, version = expected.version)

            assertEquals(TrikStudioVersionRegistration.Registered(expected), result)
            assertEquals(listOf(expected), service.findAll())
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("tech.testsys.infra.database.api.service.DatabaseServiceTrikStudioVersionsTests#refusals")
        fun `should refuse the first failed check without storing anything`(
            tag: String,
            version: String,
            reason: TrikStudioVersionRejection,
        ) {
            val registered = fixtures.trikStudioVersion(tag = "trik-studio:taken", version = "taken")

            val result = service.register(tag = tag, version = version)

            assertEquals(TrikStudioVersionRegistration.Rejected(reason), result)
            assertEquals(listOf(registered), service.findAll())
        }
    }

    companion object {

        @JvmStatic
        fun refusals(): List<Arguments> = listOf(
            Arguments.of("trik-studio:new", " \t ", TrikStudioVersionRejection.BlankVersion),
            Arguments.of("", "", TrikStudioVersionRejection.BlankVersion),
            Arguments.of("trik-studio:new", "😀".repeat(256), TrikStudioVersionRejection.VersionTooLong),
            Arguments.of("trik-studio:new", " taken ", TrikStudioVersionRejection.VersionAlreadyRegistered),
            Arguments.of("trik-studio:taken", "taken", TrikStudioVersionRejection.VersionAlreadyRegistered),
            Arguments.of("  ", "new", TrikStudioVersionRejection.BlankTag),
            Arguments.of("😀".repeat(513), "new", TrikStudioVersionRejection.TagTooLong),
            Arguments.of(" trik-studio:taken ", "new", TrikStudioVersionRejection.TagAlreadyRegistered),
        )
    }
}
