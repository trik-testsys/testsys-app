package tech.testsys.infra.database.internal.persistence

import org.slf4j.LoggerFactory
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.UUID

/**
 * Visits old UUID blobs directly inside configured directories without following symbolic links.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
class FileSystemBlobInventory {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Passes old regular files to [consume] in bounded batches; physical directory aliases are visited once.
     * Missing directories are skipped and never created; database failures from [consume] propagate immediately.
     *
     * @since %CURRENT_VERSION%
     */
    fun forEachBatch(paths: List<Path>, cutoff: Instant, batchSize: Int, consume: (List<Path>) -> Unit) {
        require(batchSize > 0) { "Blob inventory batchSize must be positive: $batchSize" }
        val visited = mutableListOf<Path>()
        paths.forEach { directory ->
            require(directory.isAbsolute) { "Blob directory must be absolute: $directory" }
            if (hasSymbolicLink(directory) || !Files.isDirectory(directory, NOFOLLOW_LINKS)) return@forEach
            if (visited.any { previous -> Files.isSameFile(previous, directory) }) return@forEach
            visited.add(directory)
            val batch = mutableListOf<Path>()
            Files.newDirectoryStream(directory).use { files ->
                files.forEach { file ->
                    if (isOldBlob(file, cutoff)) batch.add(file)
                    if (batch.size == batchSize) {
                        consume(batch.toList())
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) consume(batch.toList())
        }
    }

    /**
     * Deletes [file] if it still exists; logs an I/O failure so a later pass can retry it.
     *
     * @since %CURRENT_VERSION%
     */
    fun delete(file: Path) {
        try {
            Files.deleteIfExists(file)
        } catch (failure: IOException) {
            logger.warn("Could not delete orphan blob {}", file, failure)
        }
    }

    private fun hasSymbolicLink(path: Path): Boolean = generateSequence(path) { current -> current.parent }
        .any { part -> Files.isSymbolicLink(part) }

    private fun isOldBlob(file: Path, cutoff: Instant): Boolean {
        val key = file.fileName.toString()
        val isCanonical = try {
            UUID.fromString(key).toString() == key
        } catch (_: IllegalArgumentException) {
            false
        }
        if (!isCanonical) return false
        return try {
            val attributes = Files.readAttributes(file, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
            attributes.isRegularFile && attributes.lastModifiedTime().toInstant().isBefore(cutoff)
        } catch (failure: IOException) {
            logger.warn("Could not inspect blob {}", file, failure)
            false
        }
    }
}
