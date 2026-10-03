package tech.testsys.infra.database.api.persistence

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.util.Optional
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class FileDataStorageTests {

    private val fileDataJpaEntityRepository = mockk<FileDataJpaEntityRepository>()
    private val fileBlobStorage = mockk<FileBlobStorage>()
    private val storage = FileDataStorage(fileDataJpaEntityRepository, fileBlobStorage)

    @Nested
    inner class StoreTests {

        private val storedContents = mutableListOf<ByteArray>()
        private val savedRows = mutableListOf<FileDataJpaEntity>()

        @BeforeEach
        fun setUp() {
            every { fileBlobStorage.store(capture(storedContents)) } returns StoredBlobRef(STORED_KEY)
            every { fileDataJpaEntityRepository.save(capture(savedRows)) } answers {
                val row = firstArg<FileDataJpaEntity>()
                FileDataJpaEntity(row.uploadedFileName, row.storedFileName, row.contentHash, id = NEW_ID)
            }
        }

        @Test
        fun `should store the content as a blob and a row with the name, blob key and content hash`() {
            val result = storage.store(fileData(CURRENT_NAME, CURRENT_CONTENT))

            assertEquals(NEW_ID, result)
            assertContentEquals(CURRENT_CONTENT, storedContents.single())
            val savedRow = savedRows.single()
            assertEquals(CURRENT_NAME, savedRow.uploadedFileName)
            assertEquals(STORED_KEY, savedRow.storedFileName)
            assertEquals(CURRENT_CONTENT_HASH, savedRow.contentHash)
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
            val isMatch = storage.matches(CURRENT_ID, fileData(CURRENT_NAME, CURRENT_CONTENT))

            assertTrue(isMatch)
        }

        @Test
        fun `should not match the stored file if the content changed`() {
            val isMatch = storage.matches(CURRENT_ID, fileData(CURRENT_NAME, "changed".toByteArray()))

            assertFalse(isMatch)
        }

        @Test
        fun `should not match the stored file if the filename changed`() {
            val isMatch = storage.matches(CURRENT_ID, fileData(RENAMED_NAME, CURRENT_CONTENT))

            assertFalse(isMatch)
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
        private const val CURRENT_CONTENT_HASH = "97b0560280ed60a5a1eaa1bc45492543c8a986ad5a25b468c427eb83c3e88191"
        private val CURRENT_CONTENT = "current".toByteArray()
    }
}
