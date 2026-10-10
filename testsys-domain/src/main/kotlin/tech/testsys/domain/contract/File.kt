package tech.testsys.domain.contract

import tech.testsys.domain.model.task.FileData
import java.nio.file.Path

/**
 * Opaque reference to a binary blob kept in a [FileBlobStorage]. Only the storage that issued the key can resolve it,
 * and only together with the path the blob was stored in.
 *
 * @property key the storage-specific key identifying the blob.
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class StoredBlobRef(val key: String)

/**
 * Port for keeping the raw binary content of files outside the domain. The caller chooses the path of every
 * call; every [store] call creates a new blob, and existing blobs are never overwritten.
 *
 * @since %CURRENT_VERSION%
 */
interface FileBlobStorage {

    /**
     * Stores the given bytes as a new blob in [path].
     *
     * @param content the raw bytes to store.
     * @param path the path chosen by the caller.
     * @return the reference to the new blob, valid only together with [path].
     * @since %CURRENT_VERSION%
     */
    fun store(content: ByteArray, path: Path): StoredBlobRef

    /**
     * Loads the bytes of a blob.
     *
     * @param ref the reference returned by [store].
     * @param path the path the blob was stored in.
     * @return the raw bytes of the blob.
     * @since %CURRENT_VERSION%
     */
    fun load(ref: StoredBlobRef, path: Path): ByteArray

    /**
     * Deletes a blob; the reference becomes invalid afterwards.
     *
     * @param ref the reference returned by [store].
     * @param path the path the blob was stored in.
     * @since %CURRENT_VERSION%
     */
    fun delete(ref: StoredBlobRef, path: Path)
}

/**
 * Synchronous reader of inline or stored file contents, usable outside a persistence transaction.
 *
 * @since %CURRENT_VERSION%
 */
interface FileContentReader {
    /**
     * Reads [file] on every call; missing blobs and other technical failures propagate to the caller.
     *
     * @param file the file whose bytes are needed.
     * @return the file contents.
     * @since %CURRENT_VERSION%
     */
    fun read(file: FileData): ByteArray
}
