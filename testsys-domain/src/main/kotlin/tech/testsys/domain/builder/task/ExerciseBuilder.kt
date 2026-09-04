package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import java.util.UUID

/**
 * Builder of [ExerciseData]. Required: [file], [name], [description], [versionBucket], a choice in [language].
 *
 * @property name the name of the exercise, or `null` if not set yet.
 * @property description the description of the exercise, or `null` if not set yet.
 * @property versionBucket the UUID shared by all versions of the exercise, or `null` if not set yet.
 * @property language the chooser of the programming language.
 * @since %CURRENT_VERSION%
 */
class ExerciseDataBuilder : Builder<ExerciseData> {

    private var _file: FileData? = null

    var name: String? = null

    var description: String? = null

    var versionBucket: UUID? = null

    val language = LanguageChooser()

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

    override fun build(): ExerciseData {
        val file = requireField(_file) { ::_file }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val versionBucket = requireField(versionBucket) { ::versionBucket }

        return ExerciseData(
            name = name,
            description = description,
            file = file,
            language = language.build(),
            versionBucket = versionBucket,
        )
    }
}

/**
 * Builder of [Exercise] entities. Required: [id], [createdAt], [version], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ExerciseBuilder : DomainEntityWithDataBuilder<Exercise, ExerciseData, ExerciseDataBuilder>() {

    override fun dataBuilder() = ExerciseDataBuilder()

    override fun build(): Exercise {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val version = requireField(version) { ::version }
        val data = requireField(data) { ::data }

        return Exercise(
            id = ExerciseId(id),
            createdAt = createdAt,
            version = version,
            data = data,
        )
    }
}
