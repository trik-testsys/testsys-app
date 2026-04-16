package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId


/**
 * Builder for constructing [SolutionData].
 *
 * @since %CURRENT_VERSION%
 */
class SolutionDataBuilder : Builder<SolutionData> {

    private var _file: FileData? = null

    /**
     * Sets the file data for the solution.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Chooser for selecting the programming language of the solution.
     *
     * @since %CURRENT_VERSION%
     */
    val language = LanguageChooser()

    /**
     * Builds the [SolutionData] instance.
     *
     * @return the constructed [SolutionData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): SolutionData {
        val file = requireField(_file) { ::_file }

        return SolutionData(
            file = file,
            language = language.build(),
        )
    }

}

/**
 * Builder for constructing [Solution] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class SolutionBuilder : DomainEntityWithDataBuilder<Solution, SolutionData, SolutionDataBuilder>() {

    override fun dataBuilder() = SolutionDataBuilder()

    /**
     * Builds the [Solution] instance.
     *
     * @return the constructed [Solution].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Solution {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Solution(
            id = SolutionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

