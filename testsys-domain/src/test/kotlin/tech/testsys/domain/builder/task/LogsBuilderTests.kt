package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData

class LogsBuilderTests : DomainEntityBuilderTests<Logs, LogsData, LogsDataBuilder>(
    LogsBuilder(),
    LogsDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(logsData {
        file("logs.txt", byteArrayOf(1, 2, 3))
    })
}
