package tech.testsys.infra.database.api.persistence

import org.springframework.stereotype.Component
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.DSYNC
import java.nio.file.StandardOpenOption.WRITE
import java.util.UUID

/**
 * [FileBlobStorage] keeping every blob as a file named by a random UUID directly in the given absolute path,
 * which [store] creates if missing. Loading a missing blob throws `NoSuchFileException`; deleting it again has no effect.
 *
 * @since %CURRENT_VERSION%
 */
@Component
class FileSystemBlobStorage : FileBlobStorage {

    override fun store(content: ByteArray, path: Path): StoredBlobRef {
        val dir = Files.createDirectories(absolute(path))

        // All blobs share one flat directory; shard it by key prefix if the number of files slows the filesystem down.
        val key = UUID.randomUUID().toString()

        // CREATE_NEW never overwrites a blob; DSYNC puts the content on disk before the metadata row is committed.
        // The directory entry itself is not fsynced; add a directory fsync if a power loss must keep the new file.
        Files.write(dir.resolve(key), content, CREATE_NEW, WRITE, DSYNC)

        return StoredBlobRef(key)
    }

    override fun load(ref: StoredBlobRef, path: Path): ByteArray = Files.readAllBytes(resolve(ref, path))

    override fun delete(ref: StoredBlobRef, path: Path) {
        Files.deleteIfExists(resolve(ref, path))
    }

    private fun absolute(path: Path): Path {
        require(path.isAbsolute) { "Blob path $path is not absolute" }

        return path
    }

    // Only keys issued by store are accepted, so a reference never points outside the path.
    private fun resolve(ref: StoredBlobRef, path: Path): Path {
        require(UUID.fromString(ref.key).toString() == ref.key) { "Blob key ${ref.key} is not a canonical UUID" }

        return absolute(path).resolve(ref.key)
    }
}
