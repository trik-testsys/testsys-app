package tech.testsys.infra.database.api.persistence

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.testsys.domain.contract.StoredBlobRef
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class FileSystemBlobStorageTests {

    @TempDir
    private lateinit var root: Path

    private val storage = FileSystemBlobStorage()

    private val path by lazy { root.resolve("statement") }

    @Nested
    inner class StoreTests {

        @Test
        fun `should load the stored content by the returned reference from the same path`() {
            val ref = storage.store(byteArrayOf(1, 2, 3), path)

            assertArrayEquals(byteArrayOf(1, 2, 3), storage.load(ref, path))
        }

        @Test
        fun `should keep both blobs under distinct references when two blobs are stored`() {
            val first = storage.store(byteArrayOf(1), path)
            val second = storage.store(byteArrayOf(2), path)

            assertNotEquals(first, second)
            assertArrayEquals(byteArrayOf(1), storage.load(first, path))
            assertArrayEquals(byteArrayOf(2), storage.load(second, path))
        }

        @Test
        fun `should create the directory if it does not exist`() {
            val nested = root.resolve("nested").resolve("logs")

            storage.store(byteArrayOf(1), nested)

            assertTrue(Files.isDirectory(nested))
        }

        @Test
        fun `should reject a relative path`() {
            assertThrows(IllegalArgumentException::class.java) { storage.store(byteArrayOf(1), Path.of("statement")) }
        }
    }

    @Nested
    inner class LoadTests {

        @Test
        fun `should not find a blob in another path`() {
            val ref = storage.store(byteArrayOf(1), path)

            assertThrows(NoSuchFileException::class.java) { storage.load(ref, root.resolve("test")) }
        }

        @Test
        fun `should reject a key that is not a canonical UUID`() {
            assertThrows(IllegalArgumentException::class.java) { storage.load(StoredBlobRef("../x"), path) }
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `should make the blob unavailable for loading`() {
            val ref = storage.store(byteArrayOf(1), path)

            storage.delete(ref, path)

            assertThrows(NoSuchFileException::class.java) { storage.load(ref, path) }
        }

        @Test
        fun `should do nothing when the blob is already deleted`() {
            val ref = storage.store(byteArrayOf(1), path)
            storage.delete(ref, path)

            storage.delete(ref, path)

            assertTrue(Files.notExists(path.resolve(ref.key)))
        }

        @Test
        fun `should reject a key that is not a canonical UUID`() {
            assertThrows(IllegalArgumentException::class.java) { storage.delete(StoredBlobRef("../x"), path) }
        }
    }
}
