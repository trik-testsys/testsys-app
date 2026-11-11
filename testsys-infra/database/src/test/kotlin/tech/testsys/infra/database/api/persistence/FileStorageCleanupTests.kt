package tech.testsys.infra.database.api.persistence

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.dao.DataAccessResourceFailureException
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.persistence.FileStorageCleanupRepository
import tech.testsys.infra.database.internal.persistence.FileSystemBlobInventory
import tech.testsys.infra.settings.api.Settings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import tech.testsys.infra.settings.api.settingsOf
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

@OptIn(InternalDatabaseApi::class)
class FileStorageCleanupTests {

    @TempDir
    lateinit var directory: Path

    private val repository = mockk<FileStorageCleanupRepository>()
    private val now = Instant.parse("2026-10-01T12:00:00Z")
    private val cutoff = now.minus(Duration.ofDays(1))

    @BeforeEach
    fun prepare() {
        directory = directory.toRealPath()

        every { repository.deleteFileData(2) } returns 0
        every { repository.findStoredKeys(any()) } returns emptySet()
    }

    @Test
    fun `should capture cleanup settings once for all batches of one explicit pass`() {
        val snapshot = SettingsSnapshot(
            revision = 1,
            values = SettingsValues(mapOf(SettingKey.CleanupMinimumAge to "PT24H", SettingKey.CleanupBatchSize to "3")),
            activeMaxMessageBytes = 100,
        )
        val captures = AtomicInteger()
        val environment = StandardEnvironment()
        every { repository.deleteFileData(3) } returnsMany listOf(3, 0)
        val paths = FileStoragePaths(directory, directory, directory, directory, directory, directory)
        val dynamic = FileStorageCleanup(
            repository = repository,
            inventory = FileSystemBlobInventory(),
            paths = paths,
            settings = Settings {
                captures.incrementAndGet()
                snapshot.get<FileStorageCleanupProperties>(environment)
            },
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        dynamic.run()

        assertEquals(1, captures.get())
        verify(exactly = 2) { repository.deleteFileData(3) }
        verify(exactly = 0) { repository.deleteFileData(2) }
    }

    @Test
    fun `should bind Spring duration units without the shared holder`() {
        val environment = environment("10m", "7")

        val actual = settingsOf(FileStorageCleanupProperties::class.java, environment, null).current()

        assertEquals(FileStorageCleanupProperties(Duration.ofMinutes(10), 7), actual)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0s", "-1m"])
    fun `should reject a nonpositive minimum age when created`(minimumAge: String) {
        val environment = environment(minimumAge, "7")

        assertThrows(BindException::class.java) { settingsOf(FileStorageCleanupProperties::class.java, environment, null) }
    }

    @Test
    fun `should reject an unparsable minimum age when created`() {
        val environment = environment("invalid", "7")

        assertThrows(BindException::class.java) { settingsOf(FileStorageCleanupProperties::class.java, environment, null) }
    }

    @Test
    fun `should remove old unowned blobs across batches and allow another pass`() {
        val first = blob(1)
        val second = blob(2)
        val third = blob(3)
        every { repository.deleteFileData(2) } returnsMany listOf(2, 1, 0)

        cleanup().run()
        cleanup().run()

        assertFalse(Files.exists(first))
        assertFalse(Files.exists(second))
        assertFalse(Files.exists(third))
        verify(exactly = 3) { repository.deleteFileData(2) }
        verify(exactly = 2) { repository.findStoredKeys(any()) }
    }

    @Test
    fun `should retain stored keys fresh blobs and exact age boundary`() {
        val owned = blob(1)
        val fresh = blob(2, now)
        val boundary = blob(3, cutoff)
        every { repository.findStoredKeys(any()) } returns setOf(owned.fileName.toString())

        cleanup().run()

        assertTrue(Files.exists(owned))
        assertTrue(Files.exists(fresh))
        assertTrue(Files.exists(boundary))
        verify(exactly = 1) { repository.findStoredKeys(listOf(owned.fileName.toString())) }
    }

    @Test
    fun `should retain files when a database deletion batch fails`() {
        val orphan = blob(1)
        every { repository.deleteFileData(2) } throws DataAccessResourceFailureException("Database unavailable")

        assertThrows(DataAccessResourceFailureException::class.java) { cleanup().run() }

        assertTrue(Files.exists(orphan))
        verify(exactly = 0) { repository.findStoredKeys(any()) }
    }

    @Test
    fun `should stop deleting files when the stored key check fails`() {
        val orphan = blob(1)
        every { repository.findStoredKeys(any()) } throws DataAccessResourceFailureException("Database unavailable")

        assertThrows(DataAccessResourceFailureException::class.java) { cleanup().run() }

        assertTrue(Files.exists(orphan))
    }

    @Test
    fun `should ignore nested directories foreign names and absent configured directories`() {
        val nested = Files.createDirectory(directory.resolve("nested"))
        val nestedBlob = Files.writeString(nested.resolve("00000000-0000-0000-0000-000000000001"), "nested")
        val foreign = Files.writeString(directory.resolve("notes.txt"), "foreign")
        val missing = directory.resolve("missing")
        Files.setLastModifiedTime(nestedBlob, FileTime.from(Instant.EPOCH))
        Files.setLastModifiedTime(foreign, FileTime.from(Instant.EPOCH))
        val visited = mutableListOf<Path>()

        FileSystemBlobInventory().forEachBatch(listOf(directory, missing), cutoff, 2) { files -> visited.addAll(files) }

        assertEquals(emptyList<Path>(), visited)
        assertTrue(Files.exists(nestedBlob))
        assertTrue(Files.exists(foreign))
        assertFalse(Files.exists(missing))
    }

    @Test
    fun `should skip symbolic links to files and directories`() {
        val targetDirectory = Files.createDirectory(directory.resolve("target"))
        val target = Files.writeString(targetDirectory.resolve("target.txt"), "retained")
        Files.setLastModifiedTime(target, FileTime.from(Instant.EPOCH))
        val fileLink = directory.resolve("00000000-0000-0000-0000-000000000001")
        val directoryLink = directory.resolve("directory-link")
        try {
            Files.createSymbolicLink(fileLink, target)
            Files.createSymbolicLink(directoryLink, targetDirectory)
        } catch (failure: FileSystemException) {
            assumeTrue(false, "Symbolic links unavailable: ${failure.message}")
        }
        val visited = mutableListOf<Path>()

        FileSystemBlobInventory().forEachBatch(listOf(directory, directoryLink), cutoff, 2) { files -> visited.addAll(files) }

        assertEquals(emptyList<Path>(), visited)
        assertTrue(Files.isSymbolicLink(fileLink))
        assertEquals("retained", Files.readString(target))
    }

    @Test
    fun `should keep the same stored key in every configured directory`() {
        val first = blob(1)
        val otherDirectory = Files.createDirectory(directory.resolve("other"))
        val second = Files.writeString(otherDirectory.resolve(first.fileName), "second")
        Files.setLastModifiedTime(second, FileTime.from(Instant.EPOCH))
        every { repository.findStoredKeys(any()) } returns setOf(first.fileName.toString())
        val cleanup = FileStorageCleanup(
            repository = repository,
            inventory = FileSystemBlobInventory(),
            paths = FileStoragePaths(directory, otherDirectory, directory, directory, directory, directory),
            settings = Settings { FileStorageCleanupProperties(minimumAge = Duration.ofDays(1), batchSize = 2) },
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        cleanup.run()

        assertTrue(Files.exists(first))
        assertTrue(Files.exists(second))
        verify(exactly = 2) { repository.findStoredKeys(listOf(first.fileName.toString())) }
    }

    @Test
    fun `should tolerate a missing blob on deletion`() {
        val missing = directory.resolve("00000000-0000-0000-0000-000000000001")

        FileSystemBlobInventory().delete(missing)

        assertFalse(Files.exists(missing))
    }

    @Test
    fun `should leave a file for retry after an IO deletion failure`() {
        val nonemptyDirectory = Files.createDirectory(directory.resolve("blocked"))
        val child = Files.writeString(nonemptyDirectory.resolve("child"), "content")

        FileSystemBlobInventory().delete(nonemptyDirectory)

        assertTrue(Files.exists(child))
    }

    private fun blob(id: Int, modifiedAt: Instant = Instant.EPOCH): Path {
        val path = directory.resolve("00000000-0000-0000-0000-${id.toString().padStart(12, '0')}")
        Files.writeString(path, "blob")
        Files.setLastModifiedTime(path, FileTime.from(modifiedAt))
        return path
    }

    private fun environment(minimumAge: String, batchSize: String) = StandardEnvironment().apply {
        propertySources.addFirst(
            MapPropertySource(
                "test",
                mapOf("testsys.file-storage.cleanup.minimum-age" to minimumAge, "testsys.file-storage.cleanup.batch-size" to batchSize),
            ),
        )
    }

    private fun cleanup() = FileStorageCleanup(
        repository = repository,
        inventory = FileSystemBlobInventory(),
        paths = FileStoragePaths(directory, directory, directory, directory, directory, directory),
        settings = Settings { FileStorageCleanupProperties(minimumAge = Duration.ofDays(1), batchSize = 2) },
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )
}
