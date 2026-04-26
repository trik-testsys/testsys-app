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
 * Builder for constructing [ExerciseData].
 *
 * @since %CURRENT_VERSION%
 */
class ExerciseDataBuilder : Builder<ExerciseData> {

    private var _file: FileData? = null

    /**
     * The name of the exercise.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * The description of the exercise.
     *
     * @since %CURRENT_VERSION%
     */
    var description: String? = null

    /**
     * Logical identity shared by all versions of this exercise.
     *
     * @since %CURRENT_VERSION%
     */
    var versionBucket: UUID? = null

    /**
     * Chooser for selecting the programming language of the exercise.
     *
     * @since %CURRENT_VERSION%
     */
    val language = LanguageChooser()

    /**
     * Sets the file data for the exercise.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Builds the [ExerciseData] instance.
     *
     * @return the constructed [ExerciseData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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
 * Builder for constructing [Exercise] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class ExerciseBuilder : DomainEntityWithDataBuilder<Exercise, ExerciseData, ExerciseDataBuilder>() {

    override fun dataBuilder() = ExerciseDataBuilder()

    /**
     * Builds the [Exercise] instance.
     *
     * @return the constructed [Exercise].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Exercise {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Exercise(
            id = ExerciseId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
