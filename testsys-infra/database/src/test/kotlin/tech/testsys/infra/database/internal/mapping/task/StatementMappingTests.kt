package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.infra.database.internal.mapping.EntityMappingTests
import tech.testsys.infra.database.internal.InternalDatabaseApi

@InternalDatabaseApi
class StatementMappingTests : EntityMappingTests<StatementMapping>() {

    override val mapping = StatementMapping
}