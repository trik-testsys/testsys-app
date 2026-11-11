package tech.testsys.infra.database.internal.mapping.user

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.mapping.EntityMappingTests

@InternalDatabaseApi
class AccessRestorationRequestMappingTests : EntityMappingTests<AccessRestorationRequestMapping>() {

    override val mapping = AccessRestorationRequestMapping
}
