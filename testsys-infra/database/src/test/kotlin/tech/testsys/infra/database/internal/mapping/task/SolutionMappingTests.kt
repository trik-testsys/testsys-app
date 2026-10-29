package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTests

@InternalDatabaseApi
class SolutionMappingTests : EntityMappingTests<SolutionMapping>() {

    override val mapping = SolutionMapping
}
