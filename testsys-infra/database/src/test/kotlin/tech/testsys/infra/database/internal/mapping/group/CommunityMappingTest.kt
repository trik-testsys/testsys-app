package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.infra.database.internal.mapping.EntityMappingTest
import tech.testsys.infra.database.internal.InternalDatabaseApi

@InternalDatabaseApi
class CommunityMappingTest : EntityMappingTest<CommunityMapping>() {

    override val mapping = CommunityMapping
}