package tech.testsys.infra.database.api.persistence

import org.springframework.stereotype.Component
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.model.task.FileData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireId
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Append-only storage of [FileData]: metadata goes to a [FileDataJpaEntity] row, content to [FileBlobStorage].
 * A stored file never changes; [matches] compares a file with a stored one by uploaded name and content hash.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class FileDataStorage(
    private val fileDataJpaEntityRepository: FileDataJpaEntityRepository,
    private val fileBlobStorage: FileBlobStorage,
) {

    /**
     * Stores [file] as a new row and blob.
     *
     * @return the id of the inserted [FileDataJpaEntity].
     * @since %CURRENT_VERSION%
     */
    fun store(file: FileData): Long {
        val blobRef = fileBlobStorage.store(file.content)
        val saved = fileDataJpaEntityRepository.save(
            FileDataJpaEntity(
                uploadedFileName = file.uploadedFilename,
                storedFileName = blobRef.key,
                contentHash = file.contentHash(),
            ),
        )
        return saved.requireId()
    }

    /**
     * Returns `true` if [file] has the same uploaded name and content hash as the stored file [fileDataId].
     *
     * @since %CURRENT_VERSION%
     */
    fun matches(fileDataId: Long, file: FileData): Boolean {
        val stored = fileDataJpaEntityRepository.findByIdOrError(fileDataId)
        return stored.uploadedFileName == file.uploadedFilename && stored.contentHash == file.contentHash()
    }

    /**
     * Loads the uploaded filename and content of the file referenced by [fileDataId].
     *
     * @since %CURRENT_VERSION%
     */
    fun load(fileDataId: Long): LoadedFile {
        val fileData = fileDataJpaEntityRepository.findByIdOrError(fileDataId)
        val content = fileBlobStorage.load(StoredBlobRef(fileData.storedFileName))
        return LoadedFile(uploadedFilename = fileData.uploadedFileName, content = content)
    }

    /**
     * File loaded from [FileDataStorage].
     *
     * @property uploadedFilename the name the file was uploaded with.
     * @property content the binary content of the file.
     * @since %CURRENT_VERSION%
     */
    data class LoadedFile(val uploadedFilename: String, val content: ByteArray) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LoadedFile

            if (uploadedFilename != other.uploadedFilename) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uploadedFilename.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }

    companion object {

        @JvmStatic
        private fun FileData.contentHash(): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))
    }
}
