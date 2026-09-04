package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import java.util.UUID

class ExerciseBuilderTests : DomainEntityBuilderTests<Exercise, ExerciseData, ExerciseDataBuilder>(
    ExerciseBuilder(),
    ExerciseDataBuilder()
)  {
    override fun buildDataWithAllFields() = listOf(
        exerciseData {
            name = "Exercise Python"
            description = "Python exercise"
            file("exercise.qrs", byteArrayOf(1, 2, 3))
            language.python()
            versionBucket = UUID.randomUUID()
        },
        exerciseData {
            name = "Exercise JS"
            description = "JS exercise"
            file("exercise.js", byteArrayOf(4, 5, 6))
            language.javaScript()
            versionBucket = UUID.randomUUID()
        },
        exerciseData {
            name = "Exercise Visual"
            description = "Visual exercise"
            file("exercise.xml", byteArrayOf(7, 8, 9))
            language.visualLanguage()
            versionBucket = UUID.randomUUID()
        },
    )
}
