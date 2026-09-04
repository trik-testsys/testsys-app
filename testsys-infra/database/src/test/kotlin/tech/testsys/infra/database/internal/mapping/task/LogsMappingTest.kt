package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTest

@InternalDatabaseApi
class LogsMappingTest : EntityMappingTest<LogsMapping>()  {

    override val mapping = LogsMapping
}