package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId

/**
 * Builder for constructing [RecordingData].
 *
 * @since %CURRENT_VERSION%
 */
class RecordingDataBuilder : Builder<RecordingData> {

    private var _file: FileData? = null

    /**
     * Sets the file data for the recording.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Builds the [RecordingData] instance.
     *
     * @return the constructed [RecordingData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): RecordingData {
        val file = requireField(_file) { ::_file }

        return RecordingData(
            file = file,
        )
    }
}

/**
 * Builder for constructing [Recording] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class RecordingBuilder : DomainEntityWithDataBuilder<Recording, RecordingData, RecordingDataBuilder>() {

    override fun dataBuilder() = RecordingDataBuilder()

    /**
     * Builds the [Recording] instance.
     *
     * @return the constructed [Recording].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Recording {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Recording(
            id = RecordingId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
