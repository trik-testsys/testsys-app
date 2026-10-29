package tech.testsys.infra.database.api.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.persistence.repository.*
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.task.FileContent
import tech.testsys.infra.database.DatabaseIntegrationTests

class FileMetadataTests : DatabaseIntegrationTests() {
    @Autowired
    private lateinit var statements: StatementRepository

    @Autowired
    private lateinit var exercises: ExerciseRepository

    @Autowired
    private lateinit var tests: TestRepository

    @Autowired
    private lateinit var developerSolutions: DeveloperSolutionRepository

    @Autowired
    private lateinit var solutions: SolutionRepository

    @Autowired
    private lateinit var logss: LogsRepository

    @Autowired
    private lateinit var recordings: RecordingRepository

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var blobs: FileBlobStorage

    @Autowired
    private lateinit var paths: FileStoragePaths

    @Autowired
    private lateinit var reader: FileContentReader

    @Test
    fun `should list history and rename a statement without its blob`() {
        val saved = fixtures.statement()
        val task = tasks.update(fixtures.workingTask().withData { uploadedResources.add(saved.data.versionBucket) })
        val stored = assertInstanceOf(FileContent.Stored::class.java, saved.data.file.content)
        blobs.delete(stored.ref, paths.statement)

        val loaded = statements.findVersionsByVersionBucket(saved.data.versionBucket).single()
        val result = statements.update(loaded.withData { name = "Renamed" })

        assertEquals("Renamed", result.data.name)
        assertEquals(stored, result.data.file.content)
        assertEquals(EntityVersion(requireNotNull(saved.version).value + 1), result.version)
        assertEquals(EntityVersion(requireNotNull(task.version).value + 1), requireNotNull(tasks.findById(task.id)).version)
        assertThrows(ObjectOptimisticLockingFailureException::class.java) {
            statements.update(loaded.withData { name = "Stale" })
        }
        assertThrows(IllegalArgumentException::class.java) { reader.read(result.data.file) }
    }

    @Test
    fun `should copy stored content to an independent blob and metadata row`() {
        val original = fixtures.statement()
        val expected = reader.read(original.data.file)
        val originalRef = assertInstanceOf(FileContent.Stored::class.java, original.data.file.content)

        val copy = statements.save(original.data)
        val copyRef = assertInstanceOf(FileContent.Stored::class.java, copy.data.file.content)
        blobs.delete(originalRef.ref, paths.statement)

        assertNotEquals(originalRef.ref, copyRef.ref)
        assertEquals(2, jdbc.queryForObject("select count(distinct file_data_id) from ts_statement", Int::class.java))
        assertArrayEquals(expected, reader.read(copy.data.file))
    }

    @Test
    fun `should read a stored file after its repository transaction has completed`() {
        val saved = fixtures.statement()
        val expected = reader.read(saved.data.file)

        val detached = requireNotNull(statements.findById(saved.id)).data.file

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive())
        assertArrayEquals(expected, reader.read(detached))
    }

    @Test
    fun `should batch statement file metadata for one and twenty full entities`() {
        val rows = List(20) { fixtures.statement() }

        val (_, one) = withStatementCount { statements.findByIds(listOf(rows.first().id)) }
        val (loaded, twenty) = withStatementCount { statements.findByIds(rows.map { row -> row.id }) }

        assertEquals(20, loaded.size)
        assertEquals(one, twenty)
    }

    @Test
    fun `should batch exercise file metadata for one and twenty full entities`() {
        val rows = List(20) { fixtures.exercise() }

        val (_, one) = withStatementCount { exercises.findByIds(listOf(rows.first().id)) }
        val (loaded, twenty) = withStatementCount { exercises.findByIds(rows.map { row -> row.id }) }

        assertEquals(20, loaded.size)
        assertEquals(one, twenty)
    }

    @Test
    fun `should batch test file metadata for one and twenty full entities`() {
        val rows = List(20) { fixtures.polygon() }

        val (_, one) = withStatementCount { tests.findByIds(listOf(rows.first().id)) }
        val (loaded, twenty) = withStatementCount { tests.findByIds(rows.map { row -> row.id }) }

        assertEquals(20, loaded.size)
        assertEquals(one, twenty)
    }

    @Test
    fun `should batch developerSolution file metadata for one and twenty full entities`() {
        val rows = List(20) { fixtures.developerSolution() }

        val (_, one) = withStatementCount { developerSolutions.findByIds(listOf(rows.first().id)) }
        val (loaded, twenty) = withStatementCount { developerSolutions.findByIds(rows.map { row -> row.id }) }

        assertEquals(20, loaded.size)
        assertEquals(one, twenty)
    }

    @Test
    fun `should batch solution file metadata for one and twenty full entities`() {
        val rows = List(20) { fixtures.solution() }

        val (_, one) = withStatementCount { solutions.findByIds(listOf(rows.first().id)) }
        val (loaded, twenty) = withStatementCount { solutions.findByIds(rows.map { row -> row.id }) }

        assertEquals(20, loaded.size)
        assertEquals(one, twenty)
    }

    @Test
    fun `should batch logs file metadata for one and twenty full entities`() {
        val rows = List(20) { fixtures.logs() }

        val (_, one) = withStatementCount { logss.findByIds(listOf(rows.first().id)) }
        val (loaded, twenty) = withStatementCount { logss.findByIds(rows.map { row -> row.id }) }

        assertEquals(20, loaded.size)
        assertEquals(one, twenty)
    }

    @Test
    fun `should batch recording file metadata for one and twenty full entities`() {
        val rows = List(20) { fixtures.recording() }

        val (_, one) = withStatementCount { recordings.findByIds(listOf(rows.first().id)) }
        val (loaded, twenty) = withStatementCount { recordings.findByIds(rows.map { row -> row.id }) }

        assertEquals(20, loaded.size)
        assertEquals(one, twenty)
    }
}
