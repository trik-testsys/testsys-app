package tech.testsys.infra.database.api.persistence

import org.springframework.stereotype.Component
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.model.TextLimits
import tech.testsys.domain.model.task.FileContent
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.utils.findAllByIdOrError
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireId
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Append-only storage of file metadata and blobs. Loading metadata never reads blob contents.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class FileDataStorage(
    private val fileDataJpaEntityRepository: FileDataJpaEntityRepository,
    private val fileBlobStorage: FileBlobStorage,
    private val paths: FileStoragePaths,
) : FileContentReader {

    override fun read(file: FileData): ByteArray = when (val content = file.content) {
        is FileContent.Inline -> content.bytes
        is FileContent.Stored -> fileBlobStorage.load(content.ref, path(content.kind))
    }

    /**
     * Copies [file] into a new metadata row and blob of [kind], reading its contents once.
     *
     * @return the id of the inserted metadata row.
     * @throws IllegalArgumentException if the uploaded file name exceeds its limit, before any blob I/O.
     * @since %CURRENT_VERSION%
     */
    fun store(file: FileData, kind: FileStorageKind): Long {
        require(TextLimits.isValidUploadedFilename(file.uploadedFilename)) {
            "Uploaded file name exceeds 512 Unicode code points"
        }

        val bytes = read(file)
        val blobRef = fileBlobStorage.store(bytes, path(kind))
        val saved = fileDataJpaEntityRepository.save(
            FileDataJpaEntity(
                uploadedFileName = file.uploadedFilename,
                storedFileName = blobRef.key,
                contentHash = bytes.contentHash(),
            ),
        )
        return saved.requireId()
    }

    /**
     * Compares [file] with [fileDataId] by name and either stored reference or inline content hash, without blob I/O.
     *
     * @since %CURRENT_VERSION%
     */
    fun matches(fileDataId: Long, file: FileData, kind: FileStorageKind): Boolean {
        val stored = fileDataJpaEntityRepository.findByIdOrError(fileDataId)
        return stored.uploadedFileName == file.uploadedFilename && when (val content = file.content) {
            is FileContent.Inline -> stored.contentHash == content.bytes.contentHash()
            is FileContent.Stored -> content.kind == kind && content.ref.key == stored.storedFileName
        }
    }

    /**
     * Loads file metadata with a stored reference, without reading the blob.
     *
     * @param fileDataId the metadata row identifier.
     * @param kind the directory kind containing the blob.
     * @return the file with a reference usable outside a transaction.
     * @since %CURRENT_VERSION%
     */
    fun load(fileDataId: Long, kind: FileStorageKind): FileData = fileDataJpaEntityRepository.findByIdOrError(fileDataId).toFile(kind)

    /**
     * Loads stored references in one batched metadata lookup, failing if a row is missing.
     *
     * @param fileDataIds metadata identifiers; duplicates are ignored and empty input performs no I/O.
     * @param kind the directory kind containing the blobs.
     * @return files indexed by metadata identifier.
     * @since %CURRENT_VERSION%
     */
    fun loadAll(fileDataIds: Collection<Long>, kind: FileStorageKind): Map<Long, FileData> =
        fileDataJpaEntityRepository.findAllByIdOrError(fileDataIds).mapValues { (_, row) -> row.toFile(kind) }

    private fun FileDataJpaEntity.toFile(kind: FileStorageKind) = FileData(
        uploadedFilename = uploadedFileName,
        content = FileContent.Stored(ref = StoredBlobRef(storedFileName), kind = kind),
    )

    private fun path(kind: FileStorageKind): Path = when (kind) {
        FileStorageKind.Statement -> paths.statement
        FileStorageKind.Exercise -> paths.exercise
        FileStorageKind.Test -> paths.test
        FileStorageKind.Solution -> paths.solution
        FileStorageKind.Recording -> paths.recording
        FileStorageKind.Logs -> paths.logs
    }

    private fun ByteArray.contentHash(): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(this))
}
