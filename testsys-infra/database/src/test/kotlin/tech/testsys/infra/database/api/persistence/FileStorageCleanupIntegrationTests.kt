package tech.testsys.infra.database.api.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.persistence.FileStorageCleanupRepository
import tech.testsys.infra.database.internal.persistence.FileSystemBlobInventory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@OptIn(InternalDatabaseApi::class)
class FileStorageCleanupIntegrationTests : DatabaseIntegrationTests() {

    @TempDir
    lateinit var directory: Path

    @Autowired
    private lateinit var repository: FileStorageCleanupRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var fileData: FileDataJpaEntityRepository

    @Autowired
    private lateinit var storage: FileDataStorage

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private val now = Instant.parse("2040-01-01T12:00:00Z")

    @BeforeEach
    fun prepare() {
        directory = directory.toRealPath()
    }

    @Test
    fun `should remove orphan file metadata of any age through all batches and preserve fresh blobs`() {
        createOrphanFile()
        createOrphanFile()
        createOrphanFile()
        ageRows()
        val fresh = createOrphanFile()
        jdbc.update("update ts_file_data set created_at = ? where id = ?", Timestamp.from(now), fresh)
        val freshKey = fileData.findById(fresh).orElseThrow().storedFileName
        val originalKeys = materializeBlobs()
        Files.setLastModifiedTime(directory.resolve(freshKey), FileTime.from(now))

        cleanup().run()

        assertEquals(0, count("ts_file_data"))
        assertTrue(Files.exists(directory.resolve(freshKey)))
        assertEquals(listOf(freshKey), originalKeys.filter { key -> Files.exists(directory.resolve(key)) })
    }

    @Test
    fun `should retain all six file owners including standalone solutions logs and recordings`() {
        createReferencedFiles()
        ageRows()
        val originalKeys = materializeBlobs()

        cleanup().run()

        assertEquals(1, count("ts_solution"))
        assertEquals(1, count("ts_logs"))
        assertEquals(1, count("ts_recording"))
        assertEquals(originalKeys.toSet(), keys().toSet())
        assertTrue(originalKeys.all { key -> Files.exists(directory.resolve(key)) })
    }

    @Test
    fun `should preserve physical files exactly at the age boundary after removing their metadata`() {
        createOrphanFile()
        val key = materializeBlobs().single()
        Files.setLastModifiedTime(directory.resolve(key), FileTime.from(now.minus(Duration.ofDays(1))))

        cleanup().run()

        assertEquals(0, count("ts_file_data"))
        assertTrue(Files.exists(directory.resolve(key)))
    }

    @Test
    fun `should remove a physical blob left by a rolled back write on the next pass`() {
        val storage = FileDataStorage(
            fileData,
            FileSystemBlobStorage(),
            FileStoragePaths(
                statement = directory,
                exercise = directory,
                test = directory,
                solution = directory,
                recording = directory,
                logs = directory,
            ),
        )
        val data = logsData { file("logs.txt", byteArrayOf(1, 2)) }
        TransactionTemplate(transactionManager).executeWithoutResult { status ->
            storage.store(data.file, FileStorageKind.Logs)
            status.setRollbackOnly()
        }
        val orphan = Files.list(directory).use { stream -> stream.toList().single() }
        Files.setLastModifiedTime(orphan, FileTime.from(Instant.EPOCH))
        assertTrue(Files.exists(orphan))
        assertEquals(0, count("ts_file_data"))

        cleanup().run()

        assertFalse(Files.exists(orphan))
    }

    @Test
    fun `should limit orphan file id pages ordered by id regardless of creation time`() {
        val first = createOrphanFile()
        val second = createOrphanFile()
        val third = createOrphanFile()
        createOrphanFile()
        ageRows()
        jdbc.update("update ts_file_data set created_at = ? where id = ?", Timestamp.from(now.minusSeconds(1)), first)
        jdbc.update("update ts_file_data set created_at = ? where id = ?", Timestamp.from(Instant.EPOCH.minusSeconds(1)), third)

        val ids = fileData.findOrphanIds(PageRequest.of(0, 2))

        assertEquals(listOf(first, second), ids)
    }

    @Test
    fun `should recheck all six file owners when deleting supplied ids directly`() {
        createReferencedFiles()
        ageRows()
        val originalKeys = keys().toSet()
        val fileIds = ids("ts_file_data")

        val deleted = TransactionTemplate(transactionManager).execute {
            fileData.deleteOrphansByIds(fileIds)
        }

        assertEquals(0, deleted)
        assertEquals(originalKeys, keys().toSet())
    }

    @Test
    fun `should delete fresh unowned file metadata when deleting supplied ids directly`() {
        val id = createOrphanFile()
        jdbc.update("update ts_file_data set created_at = ? where id = ?", Timestamp.from(now), id)

        val deleted = TransactionTemplate(transactionManager).execute { fileData.deleteOrphansByIds(listOf(id)) }

        assertEquals(1, deleted)
        assertEquals(emptyList<Long>(), ids("ts_file_data"))
    }

    @Test
    fun `should return only existing keys from a mixed key batch`() {
        fixtures.logs()
        fixtures.solution()
        val storedKey = keys().first()

        val found = repository.findStoredKeys(listOf(storedKey, "00000000-0000-0000-0000-000000000000", storedKey))

        assertEquals(setOf(storedKey), found)
    }

    @Test
    fun `should skip database access for an empty key batch`() {
        val (found, statements) = withStatementCount { repository.findStoredKeys(emptyList()) }

        assertEquals(emptySet<String>(), found)
        assertEquals(0L, statements)
    }

    private fun createReferencedFiles() {
        fixtures.statement()
        fixtures.exercise()
        fixtures.polygon()
        fixtures.solution()
        fixtures.logs()
        fixtures.recording()
    }

    private fun createOrphanFile(): Long = storage.store(logsData { file("logs.txt", byteArrayOf(1, 2)) }.file, FileStorageKind.Logs)

    private fun ids(table: String): List<Long> = jdbc.queryForList("select id from $table order by id", Long::class.java)
        .map { id -> checkNotNull(id) }

    private fun ageRows() {
        listOf("ts_logs", "ts_recording", "ts_solution", "ts_file_data").forEach { table ->
            jdbc.update("update $table set created_at = ?", Timestamp.from(Instant.EPOCH))
        }
    }

    private fun keys(): List<String> = jdbc.queryForList("select stored_file_name from ts_file_data", String::class.java)
        .map { key -> checkNotNull(key) }

    private fun materializeBlobs(): List<String> = keys().onEach { key ->
        val file = Files.writeString(directory.resolve(key), "blob")
        Files.setLastModifiedTime(file, FileTime.from(Instant.EPOCH))
    }

    private fun count(table: String): Int = checkNotNull(jdbc.queryForObject("select count(*) from $table", Int::class.java))

    private fun cleanup() = FileStorageCleanup(
        repository = repository,
        inventory = FileSystemBlobInventory(),
        paths = FileStoragePaths(directory, directory, directory, directory, directory, directory),
        settings = FileStorageCleanupSettings(minimumAge = Duration.ofDays(1), batchSize = 2),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )
}
