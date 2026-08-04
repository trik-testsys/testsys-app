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
 * Storage for domain [FileData] shared by file-backed persistence adapters.
 *
 * A stored file is split between two backends: the [FileDataJpaEntity] row keeps
 * the metadata (uploaded and stored file names) while the binary content lives in
 * the external [FileBlobStorage]. Storage is append-only: files are never deleted
 * or overwritten, every store produces a new row and a new blob.
 *
 * Versions of the same logical file are grouped by [FileDataJpaEntity.versionBucket]:
 * callers owning a versioned resource pass its version bucket so all file versions
 * of the resource share it; unversioned files get a fresh bucket on every store.
 * An unchanged file is detected by its content hash and filename via [storeIfChanged]
 * and is not stored twice.
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
     * Persists the binary content of [file] and the matching [FileDataJpaEntity] row,
     * starting a fresh version bucket. Intended for unversioned files.
     *
     * @return the id of the freshly-inserted [FileDataJpaEntity].
     */
    fun store(file: FileData): Long = store(file, versionBucket = UUID.randomUUID())

    /**
     * Persists the binary content of [file] and the matching [FileDataJpaEntity] row
     * under the caller-provided [versionBucket], grouping the row with the other
     * file versions of the same logical resource. Previous rows and blobs are kept
     * untouched.
     *
     * @return the id of the freshly-inserted [FileDataJpaEntity].
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
     * Persists [file] only when it differs from the file referenced by [currentFileDataId];
     * a matching file (same uploaded filename and content hash) is not stored again.
     *
     * A changed file is stored with a fresh version bucket. Intended for unversioned files.
     *
     * @return [currentFileDataId] when the file is unchanged,
     * the id of the freshly-inserted [FileDataJpaEntity] otherwise.
     */
    fun storeIfChanged(currentFileDataId: Long, file: FileData): Long = storeIfChanged(currentFileDataId, file) { store(file) }

    /**
     * Persists [file] only when it differs from the file referenced by [currentFileDataId];
     * a matching file (same uploaded filename and content hash) is not stored again.
     *
     * A changed file is stored under the caller-provided [versionBucket].
     *
     * @return [currentFileDataId] when the file is unchanged,
     * the id of the freshly-inserted [FileDataJpaEntity] otherwise.
     */
    fun storeIfChanged(currentFileDataId: Long, file: FileData, versionBucket: UUID): Long =
        storeIfChanged(currentFileDataId, file) { store(file, versionBucket) }

    private fun storeIfChanged(currentFileDataId: Long, file: FileData, storeChanged: () -> Long): Long {
        val current = fileDataJpaEntityRepository.findByIdOrError(currentFileDataId)
        val wasChanged = current.uploadedFileName != file.uploadedFilename ||
            current.contentHash != file.contentHash()
        return if (wasChanged) currentFileDataId else storeChanged()
    }

    private fun FileData.contentHash(): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))

    /**
     * Loads the uploaded filename and binary content for the file referenced by [fileDataId].
     */
    fun load(fileDataId: Long): LoadedFile {
        val fileData = fileDataJpaEntityRepository.findByIdOrError(fileDataId)
        val content = fileBlobStorage.load(StoredBlobRef(fileData.storedFileName))
        return LoadedFile(uploadedFilename = fileData.uploadedFileName, content = content)
    }

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
