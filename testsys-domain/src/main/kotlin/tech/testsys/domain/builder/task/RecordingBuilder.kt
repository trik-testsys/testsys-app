package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId

/**
 * Builder of [RecordingData]. Required: [file].
 *
 * @since %CURRENT_VERSION%
 */
class RecordingDataBuilder : Builder<RecordingData> {

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

    override fun build(): RecordingData {
        val file = requireField(_file) { ::_file }

        return RecordingData(
            file = file,
        )
    }
}

/**
 * Builder of [Recording] entities. Required: [id], [createdAt], [version], [data].
 *
 * @since %CURRENT_VERSION%
 */
class RecordingBuilder : DomainEntityWithDataBuilder<Recording, RecordingData, RecordingDataBuilder>() {

    override fun dataBuilder() = RecordingDataBuilder()

    override fun build(): Recording {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val version = requireField(version) { ::version }
        val data = requireField(data) { ::data }

        return Recording(
            id = RecordingId(id),
            createdAt = createdAt,
            version = version,
            data = data,
        )
    }
}
