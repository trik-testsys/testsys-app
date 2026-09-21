package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import java.util.UUID

/**
 * Builder of [SolutionData]. Required: [file], a choice in [language], [versionBucket].
 *
 * @property language the chooser of the programming language.
 * @property versionBucket the UUID shared by all versions of the solution, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class SolutionDataBuilder : Builder<SolutionData> {

    private var _file: FileData? = null

    val language = LanguageChooser()

    var versionBucket: UUID? = null

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

    override fun build(): SolutionData {
        val file = requireField(_file) { ::_file }
        val versionBucket = requireField(versionBucket) { ::versionBucket }

        return SolutionData(
            file = file,
            language = language.build(),
            versionBucket = versionBucket,
        )
    }
}

/**
 * Builder of [Solution] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class SolutionBuilder : DomainEntityWithDataBuilder<Solution, SolutionData, SolutionDataBuilder>() {

    override fun dataBuilder() = SolutionDataBuilder()

    override fun build(): Solution {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Solution(
            id = SolutionId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
