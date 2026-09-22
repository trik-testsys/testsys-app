package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.infra.database.internal.mapping.EntityMappingTests
import tech.testsys.infra.database.internal.InternalDatabaseApi

@InternalDatabaseApi
class ClassMappingTests : EntityMappingTests<ClassMapping>() {

    override val mapping = ClassMapping
}