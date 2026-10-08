package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTests

@InternalDatabaseApi
class CommunityInviteMappingTests : EntityMappingTests<CommunityInviteMapping>() {

    override val mapping = CommunityInviteMapping
}
