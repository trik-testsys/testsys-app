package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTests

@InternalDatabaseApi
class TestMappingTests : EntityMappingTests<TestMapping>() {

    override val mapping = TestMapping
}
