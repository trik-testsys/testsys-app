package tech.testsys.infra.database.api.persistence

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.model.task.FileContent
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.nio.file.Path
import java.util.Optional
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class FileDataStorageTests {

    private val fileDataJpaEntityRepository = mockk<FileDataJpaEntityRepository>()
    private val fileBlobStorage = mockk<FileBlobStorage>()
    private val storage = FileDataStorage(
        fileDataJpaEntityRepository,
        fileBlobStorage,
        FileStoragePaths(statement = PATH, exercise = PATH, test = PATH, solution = PATH, recording = PATH, logs = PATH),
    )

    @Nested
    inner class LoadAllTests {

        @Test
        fun `should skip repositories and blobs for empty input`() {
            val result = storage.loadAll(emptyList(), FileStorageKind.Logs)

            assertTrue(result.isEmpty())
            verify(exactly = 0) { fileDataJpaEntityRepository.findAllById(any<Iterable<Long>>()) }
            verify(exactly = 0) { fileBlobStorage.load(any(), any()) }
        }

        @Test
        fun `should chunk file metadata and load references without reading blobs`() {
            val ids = (1L..1025L).toList()
            every { fileDataJpaEntityRepository.findAllById(any<Iterable<Long>>()) } answers {
                firstArg<Iterable<Long>>().map { id ->
                    FileDataJpaEntity(uploadedFileName = "file-$id", storedFileName = "key-$id", contentHash = "hash", id = id)
                }
            }

            val result = storage.loadAll(ids + ids.first(), FileStorageKind.Logs)

            assertEquals(1025, result.size)
            assertEquals("file-1025", result.getValue(1025).uploadedFilename)
            assertEquals(FileContent.Stored(StoredBlobRef("key-1025"), FileStorageKind.Logs), result.getValue(1025).content)
            verify(exactly = 2) { fileDataJpaEntityRepository.findAllById(any<Iterable<Long>>()) }
            verify(exactly = 0) { fileBlobStorage.load(any(), any()) }
        }
    }

    @Nested
    inner class StoreTests {

        private val storedContents = mutableListOf<ByteArray>()
        private val savedRows = mutableListOf<FileDataJpaEntity>()

        @BeforeEach
        fun setUp() {
            every { fileBlobStorage.store(capture(storedContents), PATH) } returns StoredBlobRef(STORED_KEY)
            every { fileDataJpaEntityRepository.save(capture(savedRows)) } answers {
                val row = firstArg<FileDataJpaEntity>()
                FileDataJpaEntity(row.uploadedFileName, row.storedFileName, row.contentHash, id = NEW_ID)
            }
        }

        @Test
        fun `should store the content as a blob in the given path and a row with the name, blob key and content hash`() {
            val result = storage.store(fileData(CURRENT_NAME, CURRENT_CONTENT), FileStorageKind.Logs)

            assertEquals(NEW_ID, result)
            assertContentEquals(CURRENT_CONTENT, storedContents.single())
            val savedRow = savedRows.single()
            assertEquals(CURRENT_NAME, savedRow.uploadedFileName)
            assertEquals(STORED_KEY, savedRow.storedFileName)
            assertEquals(CURRENT_CONTENT_HASH, savedRow.contentHash)
        }

        @Test
        fun `should read stored content once and hash the bytes written to the copy`() {
            val ref = StoredBlobRef("source")
            val file = FileData(CURRENT_NAME, FileContent.Stored(ref, FileStorageKind.Logs))
            every { fileBlobStorage.load(ref, PATH) } returns CURRENT_CONTENT

            storage.store(file, FileStorageKind.Logs)

            assertContentEquals(CURRENT_CONTENT, storedContents.single())
            assertEquals(CURRENT_CONTENT_HASH, savedRows.single().contentHash)
            verify(exactly = 1) { fileBlobStorage.load(ref, PATH) }
        }
    }

    @Nested
    inner class MatchesTests {

        @BeforeEach
        fun setUp() {
            every { fileDataJpaEntityRepository.findById(CURRENT_ID) } returns Optional.of(currentRow())
        }

        @Test
        fun `should match the stored file if name and content are equal`() {
            val isMatch = storage.matches(CURRENT_ID, fileData(CURRENT_NAME, CURRENT_CONTENT), FileStorageKind.Logs)

            assertTrue(isMatch)
        }

        @Test
        fun `should not match the stored file if the content changed`() {
            val isMatch = storage.matches(CURRENT_ID, fileData(CURRENT_NAME, "changed".toByteArray()), FileStorageKind.Logs)

            assertFalse(isMatch)
        }

        @Test
        fun `should not match the stored file if the filename changed`() {
            val isMatch = storage.matches(CURRENT_ID, fileData(RENAMED_NAME, CURRENT_CONTENT), FileStorageKind.Logs)

            assertFalse(isMatch)
        }
    }

    @Nested
    inner class StoredTests {
        @BeforeEach
        fun setUp() {
            every { fileDataJpaEntityRepository.findById(CURRENT_ID) } returns Optional.of(currentRow())
        }

        @Test
        fun `should match a stored reference without reading a missing blob`() {
            val file = storage.load(CURRENT_ID, FileStorageKind.Logs)

            assertTrue(storage.matches(CURRENT_ID, file, FileStorageKind.Logs))
            verify(exactly = 0) { fileBlobStorage.load(any(), any()) }
        }

        @Test
        fun `should reject a stored reference from another directory`() {
            val file = storage.load(CURRENT_ID, FileStorageKind.Solution)

            assertFalse(storage.matches(CURRENT_ID, file, FileStorageKind.Logs))
        }

        @Test
        fun `should reject another blob key`() {
            val file = FileData(CURRENT_NAME, FileContent.Stored(StoredBlobRef("another"), FileStorageKind.Logs))

            assertFalse(storage.matches(CURRENT_ID, file, FileStorageKind.Logs))
        }

        @Test
        fun `should reject another filename for a stored reference`() {
            val file = FileData(RENAMED_NAME, FileContent.Stored(StoredBlobRef(STORED_KEY), FileStorageKind.Logs))

            assertFalse(storage.matches(CURRENT_ID, file, FileStorageKind.Logs))
        }

        @Test
        fun `should explicitly read a stored file on every call without metadata lookup`() {
            val file = storage.load(CURRENT_ID, FileStorageKind.Logs)
            every { fileBlobStorage.load(StoredBlobRef(STORED_KEY), PATH) } returns CURRENT_CONTENT

            assertContentEquals(CURRENT_CONTENT, storage.read(file))
            assertContentEquals(CURRENT_CONTENT, storage.read(file))
            verify(exactly = 1) { fileDataJpaEntityRepository.findById(CURRENT_ID) }
            verify(exactly = 2) { fileBlobStorage.load(StoredBlobRef(STORED_KEY), PATH) }
        }
    }

    private fun fileData(name: String, content: ByteArray) = logsData { file(name, content) }.file

    private fun currentRow() = FileDataJpaEntity(
        uploadedFileName = CURRENT_NAME,
        storedFileName = STORED_KEY,
        contentHash = CURRENT_CONTENT_HASH,
        id = CURRENT_ID,
    )

    companion object {

        private const val CURRENT_ID = 1L
        private const val NEW_ID = 42L
        private const val CURRENT_NAME = "solution.qrs"
        private const val RENAMED_NAME = "renamed.txt"
        private const val STORED_KEY = "stored-key"
        private val PATH: Path = Path.of("/testsys/logs")
        private const val CURRENT_CONTENT_HASH = "97b0560280ed60a5a1eaa1bc45492543c8a986ad5a25b468c427eb83c3e88191"
        private val CURRENT_CONTENT = "current".toByteArray()
    }
}
