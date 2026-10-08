package tech.testsys.infra.database

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Spring Boot application booting the database module in tests; see [DatabaseIntegrationTests] for the datasource.
 */
@SpringBootApplication(scanBasePackages = ["tech.testsys.infra.database"])
class DatabaseTestApp {

    /**
     * In-memory [FileBlobStorage]: enough for the file round trips of the adapter tests.
     * Replaces FileSystemBlobStorage in the tests.
     */
    @Bean
    @Primary
    fun fileBlobStorage(): FileBlobStorage = InMemoryFileBlobStorage()
}

/**
 * [FileBlobStorage] keeping blobs in a map by path and key for the lifetime of the JVM.
 */
class InMemoryFileBlobStorage : FileBlobStorage {

    private val blobs = ConcurrentHashMap<Pair<Path, String>, ByteArray>()

    override fun store(content: ByteArray, path: Path): StoredBlobRef {
        val ref = StoredBlobRef(UUID.randomUUID().toString())
        blobs[path to ref.key] = content.copyOf()
        return ref
    }

    override fun load(ref: StoredBlobRef, path: Path): ByteArray =
        requireNotNull(blobs[path to ref.key]) { "Blob not found by ref=${ref.key} in $path" }.copyOf()

    override fun delete(ref: StoredBlobRef, path: Path) {
        blobs.remove(path to ref.key)
    }
}
