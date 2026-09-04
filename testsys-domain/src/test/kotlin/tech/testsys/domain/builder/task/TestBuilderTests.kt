package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import java.util.UUID

class TestBuilderTests : DomainEntityBuilderTests<Test, TestData, TestDataBuilder>(
    TestBuilder(),
    TestDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(testData {
        name = "Polygon"
        description = "Polygon description"
        file("test.txt", byteArrayOf(1, 2, 3))
        versionBucket = UUID.randomUUID()
    })
}
