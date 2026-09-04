package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId

/**
 * Builder of [LogsData]. Required: [file].
 *
 * @since %CURRENT_VERSION%
 */
class LogsDataBuilder : Builder<LogsData> {

    private var _file: FileData? = null

    /**
     * Sets the file.
     *
     * @param uploadedFilename the original name of the uploaded file.
     * @param content the raw binary content of the file.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    override fun build(): LogsData {
        val file = requireField(_file) { ::_file }

        return LogsData(
            file = file,
        )
    }
}

/**
 * Builder of [Logs] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class LogsBuilder : DomainEntityWithDataBuilder<Logs, LogsData, LogsDataBuilder>() {

    override fun dataBuilder() = LogsDataBuilder()

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
