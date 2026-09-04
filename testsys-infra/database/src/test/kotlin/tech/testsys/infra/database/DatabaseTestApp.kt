package tech.testsys.infra.database

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Spring Boot application booting the database module in tests; see [DatabaseIntegrationTest] for the datasource.
 */
@SpringBootApplication(scanBasePackages = ["tech.testsys.infra.database"])
class DatabaseTestApp {

    /**
     * In-memory [FileBlobStorage]: enough for the file round trips of the adapter tests.
     * Production deployments wire their own implementation.
     */
    @Bean
    fun fileBlobStorage(): FileBlobStorage = InMemoryFileBlobStorage()
}

/**
 * [FileBlobStorage] keeping blobs in a map for the lifetime of the JVM.
 */
class InMemoryFileBlobStorage : FileBlobStorage {

    private val blobs = ConcurrentHashMap<String, ByteArray>()

    override fun store(content: ByteArray): StoredBlobRef {
        val ref = StoredBlobRef(UUID.randomUUID().toString())
        blobs[ref.key] = content.copyOf()
        return ref
    }

    override fun load(ref: StoredBlobRef): ByteArray = requireNotNull(blobs[ref.key]) { "Blob not found by ref=${ref.key}" }.copyOf()

    override fun delete(ref: StoredBlobRef) {
        blobs.remove(ref.key)
    }
}
