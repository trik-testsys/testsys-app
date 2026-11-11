package tech.testsys.infra.database.api.persistence

import java.nio.file.Path

/**
 * Absolute paths of the [FileSystemBlobStorage] blobs of every resource kind; `DatabaseConfiguration` binds them
 * from `testsys.file-storage.paths`. Every property is required.
 *
 * @property statement the path of statement files.
 * @property exercise the path of exercise files.
 * @property test the path of polygon files.
 * @property solution the path of solution files, including developer solutions.
 * @property recording the path of recording files.
 * @property logs the path of logs files.
 * @since 1.0.0-beta.1
 */
data class FileStoragePaths(
    val statement: Path,
    val exercise: Path,
    val test: Path,
    val solution: Path,
    val recording: Path,
    val logs: Path,
)
