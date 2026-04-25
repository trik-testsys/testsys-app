package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.statementData
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import java.util.UUID

class StatementBuilderTests : DomainEntityBuilderTests<Statement, StatementData, StatementDataBuilder>(
    StatementBuilder(),
    StatementDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(statementData {
        name = "Statement"
        description = "Statement description"
        file("statement.pdf", byteArrayOf(1, 2, 3))
        versionBucket = UUID.randomUUID()
    })
}
