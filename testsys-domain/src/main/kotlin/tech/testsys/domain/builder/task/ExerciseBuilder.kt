package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.VersionData

/**
 * Builder for constructing [ExerciseData].
 *
 * @since %CURRENT_VERSION%
 */
class ExerciseDataBuilder : Builder<ExerciseData> {

    private var _file: FileData? = null

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
     * Chooser for selecting the programming language of the exercise.
     *
     * @since %CURRENT_VERSION%
     */
    val language = LanguageChooser()

    /**
     * Builds the [ExerciseData] instance.
     *
     * @return the constructed [ExerciseData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): ExerciseData {
        val file = requireField(_file) { ::_file }

        return ExerciseData(
            file = file,
            language = language.build(),
        )
    }

}

/**
 * Builder for constructing [Exercise] domain entities.
 * Exercises support versioning via [versionData].
 *
 * @since %CURRENT_VERSION%
 */
class ExerciseBuilder : DomainEntityWithDataBuilder<Exercise, ExerciseData, ExerciseDataBuilder>() {

    private var _versionData: VersionData<ExerciseId, Exercise>? = null

    /**
     * Sets the version data for the exercise.
     *
     * @param root the ID of the root exercise in the version chain.
     * @param index the version index.
     * @since %CURRENT_VERSION%
     */
    fun versionData(root: ExerciseId, index: Long) {
        _versionData = VersionData(root.lazify(), index)
    }

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
        val versionData = requireField(_versionData) { ::_versionData }
        val data = requireField(data) { ::data }

        return Exercise(
            id = ExerciseId(id),
            createdAt = createdAt,
            versionData = versionData,
            data = data,
        )
    }

}
