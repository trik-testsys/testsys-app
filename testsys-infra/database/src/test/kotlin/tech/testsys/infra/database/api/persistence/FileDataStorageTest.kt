package tech.testsys.infra.database.api.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.model.task.FileData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@OptIn(InternalDatabaseApi::class)
class FileDataStorageTest {

    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository
    private lateinit var fileBlobStorage: FileBlobStorage
    private lateinit var storage: FileDataStorage

    @BeforeEach
    fun setUp() {
        fileDataJpaEntityRepository = mock(FileDataJpaEntityRepository::class.java)
        fileBlobStorage = mock(FileBlobStorage::class.java)
        storage = FileDataStorage(fileDataJpaEntityRepository, fileBlobStorage)

        `when`(fileDataJpaEntityRepository.findById(CURRENT_ID)).thenReturn(Optional.of(currentRow()))
        `when`(fileBlobStorage.store(anyNonNull())).thenReturn(StoredBlobRef(STORED_KEY))
        `when`(fileDataJpaEntityRepository.save(anyNonNull())).thenAnswer { invocation ->
            val row = invocation.getArgument<FileDataJpaEntity>(0)
            FileDataJpaEntity(row.uploadedFileName, row.storedFileName, row.versionBucket, row.contentHash, id = NEW_ID)
        }
    }

    @Test
    fun `unchanged file returns current id and stores nothing`() {
        val result = storage.storeIfChanged(CURRENT_ID, FileData(CURRENT_NAME, CURRENT_CONTENT))

        assertEquals(CURRENT_ID, result)
        verify(fileBlobStorage, never()).store(anyNonNull())
        verify(fileDataJpaEntityRepository, never()).save(anyNonNull())
    }

    @Test
    fun `changed content stores once and returns new id`() {
        val result = storage.storeIfChanged(CURRENT_ID, FileData(CURRENT_NAME, "changed".toByteArray()))

        assertEquals(NEW_ID, result)
        verify(fileBlobStorage, times(1)).store(anyNonNull())
        verify(fileDataJpaEntityRepository, times(1)).save(anyNonNull())
    }

    @Test
    fun `changed filename with same content stores once and returns new id`() {
        val result = storage.storeIfChanged(CURRENT_ID, FileData("renamed.txt", CURRENT_CONTENT))

        assertEquals(NEW_ID, result)
        verify(fileBlobStorage, times(1)).store(anyNonNull())
        verify(fileDataJpaEntityRepository, times(1)).save(anyNonNull())
    }

    private fun currentRow() = FileDataJpaEntity(
        uploadedFileName = CURRENT_NAME,
        storedFileName = STORED_KEY,
        versionBucket = UUID.randomUUID(),
        contentHash = sha256Hex(CURRENT_CONTENT),
        id = CURRENT_ID,
    )

    // Mockito matchers return null; a generic helper keeps Kotlin from null-checking it at the call site.
    private fun <T> anyNonNull(): T = ArgumentMatchers.any()

    private fun sha256Hex(content: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))

    companion object {

        private const val CURRENT_ID = 1L
        private const val NEW_ID = 42L
        private const val CURRENT_NAME = "solution.qrs"
        private const val STORED_KEY = "stored-key"
        private val CURRENT_CONTENT = "current".toByteArray()
    }
}
