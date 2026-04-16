package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.exercise
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId

class ExerciseBuilderTests : DomainEntityBuilderTests<Exercise, ExerciseData, ExerciseDataBuilder>(
    ExerciseBuilder(),
    ExerciseDataBuilder()
)  {
    override fun buildDataWithAllFields() = listOf(
        exerciseData {
            file("exercise.qrs", byteArrayOf(1, 2, 3))
            language.python()
        },
        exerciseData {
            file("exercise.js", byteArrayOf(4, 5, 6))
            language.javaScript()
        },
        exerciseData {
            file("exercise.xml", byteArrayOf(7, 8, 9))
            language.visualLanguage()
        },
    )

    override fun buildEntityWithAllFields(data: ExerciseData) = exercise {
        id = 42
        versionData(ExerciseId(10), 0)
        createdNow()
        this.data = data
    }
}
