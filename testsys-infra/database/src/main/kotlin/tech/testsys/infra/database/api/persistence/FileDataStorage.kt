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
import java.util.UUID

/**
 * Append-only storage of [FileData]: metadata goes to a [FileDataJpaEntity] row, content to [FileBlobStorage].
 * Versions of one logical file share a [FileDataJpaEntity.versionBucket]; unversioned files get a fresh bucket per store.
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
     * Stores [file] as a new row and blob in a fresh version bucket.
     *
     * @return the id of the inserted [FileDataJpaEntity].
     * @since %CURRENT_VERSION%
     */
    fun store(file: FileData): Long = store(file, versionBucket = UUID.randomUUID())

    /**
     * Stores [file] as a new row and blob in [versionBucket]; previous versions are kept.
     *
     * @return the id of the inserted [FileDataJpaEntity].
     * @since %CURRENT_VERSION%
     */
    fun store(file: FileData, versionBucket: UUID): Long {
        val blobRef = fileBlobStorage.store(file.content)
        val saved = fileDataJpaEntityRepository.save(
            FileDataJpaEntity(
                uploadedFileName = file.uploadedFilename,
                storedFileName = blobRef.key,
                versionBucket = versionBucket,
                contentHash = file.contentHash(),
            ),
        )
        return saved.requireId()
    }

    /**
     * Stores [file] in a fresh version bucket unless it matches the row [currentFileDataId] by uploaded name and content hash.
     *
     * @return [currentFileDataId] when the file is unchanged, the id of the inserted [FileDataJpaEntity] otherwise.
     * @since %CURRENT_VERSION%
     */
    fun storeIfChanged(currentFileDataId: Long, file: FileData): Long = storeIfChanged(currentFileDataId, file) { store(file) }

    /**
     * Stores [file] in [versionBucket] unless it matches the row [currentFileDataId] by uploaded name and content hash.
     *
     * @return [currentFileDataId] when the file is unchanged, the id of the inserted [FileDataJpaEntity] otherwise.
     * @since %CURRENT_VERSION%
     */
    fun storeIfChanged(currentFileDataId: Long, file: FileData, versionBucket: UUID): Long =
        storeIfChanged(currentFileDataId, file) { store(file, versionBucket) }

    private fun storeIfChanged(currentFileDataId: Long, file: FileData, storeChanged: () -> Long): Long {
        val current = fileDataJpaEntityRepository.findByIdOrError(currentFileDataId)
        val wasChanged = current.uploadedFileName != file.uploadedFilename ||
            current.contentHash != file.contentHash()
        return if (wasChanged) storeChanged() else currentFileDataId
    }

    private fun FileData.contentHash(): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))

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
}
