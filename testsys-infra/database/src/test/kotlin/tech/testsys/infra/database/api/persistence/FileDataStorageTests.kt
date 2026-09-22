package tech.testsys.infra.database.api.persistence

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Optional
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(InternalDatabaseApi::class)
class FileDataStorageTests {

    private val fileDataJpaEntityRepository = mockk<FileDataJpaEntityRepository>()
    private val fileBlobStorage = mockk<FileBlobStorage>()
    private val storage = FileDataStorage(fileDataJpaEntityRepository, fileBlobStorage)

    private val storedContents = mutableListOf<ByteArray>()
    private val savedRows = mutableListOf<FileDataJpaEntity>()

    @BeforeEach
    fun setUp() {
        every { fileDataJpaEntityRepository.findById(CURRENT_ID) } returns Optional.of(currentRow())
        every { fileBlobStorage.store(capture(storedContents)) } returns StoredBlobRef(STORED_KEY)
        every { fileDataJpaEntityRepository.save(capture(savedRows)) } answers {
            val row = firstArg<FileDataJpaEntity>()
            FileDataJpaEntity(row.uploadedFileName, row.storedFileName, row.versionBucket, row.contentHash, id = NEW_ID)
        }
    }

    @Test
    fun `should return the current id and store nothing if the file is unchanged`() {
        val result = storage.storeIfChanged(CURRENT_ID, fileData(CURRENT_NAME, CURRENT_CONTENT))

        assertEquals(CURRENT_ID, result)
        verify(exactly = 0) { fileBlobStorage.store(any()) }
        verify(exactly = 0) { fileDataJpaEntityRepository.save(any()) }
    }

    @Test
    fun `should store once and return a new id if the content changed`() {
        val changedContent = "changed".toByteArray()

        val result = storage.storeIfChanged(CURRENT_ID, fileData(CURRENT_NAME, changedContent))

        assertEquals(NEW_ID, result)
        assertContentEquals(changedContent, storedContents.single())
        val savedRow = savedRows.single()
        assertEquals(CURRENT_NAME, savedRow.uploadedFileName)
        assertEquals(STORED_KEY, savedRow.storedFileName)
        assertEquals(sha256Hex(changedContent), savedRow.contentHash)
        assertNotEquals(CURRENT_BUCKET, savedRow.versionBucket)
    }

    @Test
    fun `should store once and return a new id if the filename changed`() {
        val result = storage.storeIfChanged(CURRENT_ID, fileData(RENAMED_NAME, CURRENT_CONTENT))

        assertEquals(NEW_ID, result)
        assertContentEquals(CURRENT_CONTENT, storedContents.single())
        val savedRow = savedRows.single()
        assertEquals(RENAMED_NAME, savedRow.uploadedFileName)
        assertEquals(STORED_KEY, savedRow.storedFileName)
        assertEquals(sha256Hex(CURRENT_CONTENT), savedRow.contentHash)
        assertNotEquals(CURRENT_BUCKET, savedRow.versionBucket)
    }

    private fun fileData(name: String, content: ByteArray) = logsData { file(name, content) }.file

    private fun currentRow() = FileDataJpaEntity(
        uploadedFileName = CURRENT_NAME,
        storedFileName = STORED_KEY,
        versionBucket = CURRENT_BUCKET,
        contentHash = sha256Hex(CURRENT_CONTENT),
        id = CURRENT_ID,
    )

    private fun sha256Hex(content: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))

    companion object {

        private const val CURRENT_ID = 1L
        private const val NEW_ID = 42L
        private const val CURRENT_NAME = "solution.qrs"
        private const val RENAMED_NAME = "renamed.txt"
        private const val STORED_KEY = "stored-key"
        private val CURRENT_CONTENT = "current".toByteArray()
        private val CURRENT_BUCKET: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
