package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId

class TestBuilderTests : DomainEntityBuilderTests<Test, TestData, TestDataBuilder>(
    TestBuilder(),
    TestDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(testData {
        file("test.txt", byteArrayOf(1, 2, 3))
        versionData(TestId(1), 1)
    })
}
