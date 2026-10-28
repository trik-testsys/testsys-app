package tech.testsys.infra.database.api.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.DatabaseIntegrationTests

class DatabaseServiceTrikStudioVersionsTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var service: ServiceTrikStudioVersions

    @Autowired
    private lateinit var contests: ContestRepository

    @Autowired
    private lateinit var tasks: TaskRepository

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

    @Nested
    inner class ChangeTagTests {

        @Test
        fun `should store the trimmed new tag and keep the version number`() {
            val registered = fixtures.trikStudioVersion(tag = "trik-studio:old", version = "1.0")
            val expected = registered.copy(tag = "trik-studio:new")

            val result = service.changeTag(tag = registered.tag, newTag = " trik-studio:new ")

            assertEquals(TrikStudioVersionTagChange.Changed(expected), result)
            assertEquals(listOf(expected), service.findAll())
        }

        @Test
        fun `should keep a version used by a contest reachable under the new tag`() {
            val contest = fixtures.contest()

            service.changeTag(tag = contest.data.trikStudioVersion.tag, newTag = "trik-studio:renamed")

            assertEquals("trik-studio:renamed", checkNotNull(contests.findById(contest.id)).data.trikStudioVersion.tag)
        }

        @Test
        fun `should accept the same tag`() {
            val registered = fixtures.trikStudioVersion(tag = "trik-studio:same")

            val result = service.changeTag(tag = registered.tag, newTag = registered.tag)

            assertEquals(TrikStudioVersionTagChange.Changed(registered), result)
        }

        @Test
        fun `should refuse a tag of another version without storing anything`() {
            val registered = fixtures.trikStudioVersion(tag = "trik-studio:a")
            fixtures.trikStudioVersion(tag = "trik-studio:b")

            val result = service.changeTag(tag = registered.tag, newTag = "trik-studio:b")

            assertEquals(TrikStudioVersionTagChange.Rejected(TrikStudioVersionRejection.TagAlreadyRegistered), result)
            assertEquals(registered.tag, service.findAll().first().tag)
        }

        @Test
        fun `should report a missing version`() {
            assertEquals(TrikStudioVersionTagChange.NotFound, service.changeTag(tag = "trik-studio:none", newTag = "x"))
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `should delete an unused version`() {
            val registered = fixtures.trikStudioVersion()

            val result = service.delete(registered.tag)

            assertEquals(TrikStudioVersionDeletion.Deleted, result)
            assertEquals(emptyList<TrikStudioVersion>(), service.findAll())
        }

        @ParameterizedTest
        @EnumSource(VersionReference::class)
        fun `should keep a version referenced only by one kind of entity`(reference: VersionReference) {
            val version = fixtures.trikStudioVersion()
            reference(version, reference)

            val result = service.delete(version.tag)

            assertEquals(TrikStudioVersionDeletion.InUse, result)
            assertEquals(setOf(version.tag), service.findUsedTags())
        }

        @Test
        fun `should report a missing version`() {
            assertEquals(TrikStudioVersionDeletion.NotFound, service.delete("trik-studio:none"))
        }
    }

    /** Kind of entity that references a TRIK Studio version. */
    enum class VersionReference { Contest, TaskRevision, DeveloperSolutionTest, TaskValidationRequest }

    private fun reference(version: TrikStudioVersion, by: VersionReference) {
        when (by) {
            VersionReference.Contest -> fixtures.contest(version = version)
            VersionReference.TaskRevision -> workingTaskSupporting(version)
            VersionReference.DeveloperSolutionTest -> fixtures.submission(version = version)
            VersionReference.TaskValidationRequest -> {
                val task = workingTaskSupporting(version)
                fixtures.taskValidationRequest(task)
                val current = checkNotNull(tasks.findById(task.id))
                tasks.update(current.withData { content.new { supportedTrikStudioVersions = mutableListOf() } })
            }
        }
    }

    private fun workingTaskSupporting(version: TrikStudioVersion): Task {
        val task = fixtures.workingTask()
        return tasks.update(task.withData { content.new { supportedTrikStudioVersions = mutableListOf(version) } })
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
