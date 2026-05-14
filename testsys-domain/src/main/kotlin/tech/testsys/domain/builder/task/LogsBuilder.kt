package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.VerdictId

/**
 * Builder for constructing [LogsData].
 *
 * @since %CURRENT_VERSION%
 */
class LogsDataBuilder : Builder<LogsData> {

    private var _file: FileData? = null

    /**
     * Sets the file data for the logs.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Builds the [LogsData] instance.
     *
     * @return the constructed [LogsData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): LogsData {
        val file = requireField(_file) { ::_file }

        return LogsData(
            file = file,
        )
    }
}

/**
 * Builder for constructing [Logs] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class LogsBuilder : DomainEntityWithDataBuilder<Logs, LogsData, LogsDataBuilder>() {

    override fun dataBuilder() = LogsDataBuilder()

    /**
     * Builds the [Logs] instance.
     *
     * @return the constructed [Logs].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Logs {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Logs(
            id = LogsId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
