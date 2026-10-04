package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTests

@InternalDatabaseApi
class CompetitionMappingTests : EntityMappingTests<CompetitionMapping>() {

    override val mapping = CompetitionMapping
}
