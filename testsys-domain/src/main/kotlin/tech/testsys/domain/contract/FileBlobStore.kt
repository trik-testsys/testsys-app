package tech.testsys.domain.contract

@JvmInline
value class StoredBlobRef(val key: String)

interface FileBlobStore {

    fun store(content: ByteArray): StoredBlobRef
    fun load(ref: StoredBlobRef): ByteArray
    fun delete(ref: StoredBlobRef)
}
