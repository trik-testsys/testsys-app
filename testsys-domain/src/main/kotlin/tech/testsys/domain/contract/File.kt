package tech.testsys.domain.contract

/**
 * Opaque reference to a binary blob kept in a [FileBlobStorage]. Only the storage that issued the key can resolve it.
 *
 * @property key the storage-specific key identifying the blob.
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class StoredBlobRef(val key: String)

/**
 * Port for keeping the raw binary content of files outside the domain. Every [store] call creates a new blob;
 * existing blobs are never overwritten.
 *
 * @since %CURRENT_VERSION%
 */
interface FileBlobStorage {

    /**
     * Stores the given bytes as a new blob.
     *
     * @param content the raw bytes to store.
     * @return the reference to the new blob.
     * @since %CURRENT_VERSION%
     */
    fun store(content: ByteArray): StoredBlobRef

    /**
     * Loads the bytes of a blob.
     *
     * @param ref the reference returned by [store].
     * @return the raw bytes of the blob.
     * @since %CURRENT_VERSION%
     */
    fun load(ref: StoredBlobRef): ByteArray

    /**
     * Deletes a blob; the reference becomes invalid afterwards.
     *
     * @param ref the reference returned by [store].
     * @since %CURRENT_VERSION%
     */
    fun delete(ref: StoredBlobRef)
}
